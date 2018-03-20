package model;

import java.awt.geom.Point2D;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;

public class SpaceThread extends Thread {

	private final static double G = 6.67e-2; // gravitational constant, currently 10^8 times bigger than real life
	private final static double timestep = .1; // tickrate of simulation, can be interpreted as units in "seconds"
	private Body[] bodies;
	private int nBodies;
	private final int BORDER_WIDTH = 2000; // width constraint that bodies should stay in
	private final int BORDER_HEIGHT = 2000; // height constraint that bodies should stay in
	private long numTimesteps;
	private final double overlapTolerance = .3;
	private static int numCollisions = 0;
	private Point2D.Double[] forces;
	private final static boolean borderOn = false;

	private static GraphicsContext gc;
	private double canvasWidth;
	private double canvasHeight;
	private static Canvas canvas;

	// parallelization variables
	private static int numThreads;
	private int id;
	private static Semaphore[][] dissBarrier;
	
	public void setParallelmeters(int id, int threads, Semaphore[][] barrier) {
		this.id = id;
		numThreads = threads;
		dissBarrier = barrier;
	}

	public void setCanvas(Canvas c) {
		canvas = c;
		gc = canvas.getGraphicsContext2D();
		canvasWidth = c.getWidth();
		canvasHeight = c.getHeight();
	}

	public Canvas getCanvas() {
		return canvas;
	}

	public void setNumTimesteps(long n) {
		numTimesteps = n;
	}

	public void initializeForces() {
		forces = new Point2D.Double[nBodies];
		for (int i = 0; i < nBodies; i++) {
			forces[i] = new Point2D.Double();
		}
	}

	/* This constructor allows the initialization of preset bodies. */
	public SpaceThread(Body[] bodies) {
		this.nBodies = bodies.length;
		this.bodies = bodies;
		initializeForces();
	}

	/*
	 * moveBodies() first recalculates new vx and vy for every body, and then calls
	 * each body's move() method to recalculate their new positions. It does not
	 * take collisions into account.
	 */
	public void moveBodies() {
		// get the gravitational forces acting on every body
		Point2D.Double[] forces = calculateForces();

		// adjust the velocity for every body
		updateVelocitiesByForce(forces);

		// testing
		gc.clearRect(0, 0, canvasWidth, canvasHeight);

		// move each body according to its new velocity
		for (Body b : bodies) {
			b.move(timestep);

			// testing
			double xCorner = b.getXPos() - b.getRadius();
			double yCorner = b.getYPos() - b.getRadius();
			double width = b.getRadius() * 2;
			double height = b.getRadius() * 2;
			gc.strokeOval(xCorner, yCorner, width, height);

			// testing
			if (b.getOldPosition().equals(b.getPosition()))
				System.out.println("Samesies");
		}

		// check for collisions between any bodies
		checkCollisions();

		// last step is to notify observers of the new state
		// setChangedAndNotifyObservers();
	}

	/*
	 * This method moves two bodies <b1, b2> by a fraction of a timestep <rewind>.
	 * This is called when two bodies have overlapped more than an allowed
	 * tolerance. <rewind> should be < 1.
	 */
	public void rewind(Body b1, Body b2, double rewind) {

		// Update velocities by force
		Point2D.Double deltaV; // dv = f/m, dv = a

		// Velocity = (Force / Mass) * timestep. This is F = ma derived for velocity
		// Recalculate first body's velocity
		deltaV = new Point2D.Double(b1.getOldForce().getX() / b1.getMass(), b1.getOldForce().getY() / b1.getMass());
		b1.changeOldVelocityBy(deltaV, timestep * rewind);

		// Recalculate second body's velocity
		deltaV = new Point2D.Double(b2.getOldForce().getX() / b2.getMass(), b2.getOldForce().getY() / b2.getMass());
		b2.changeOldVelocityBy(deltaV, timestep * rewind);

		// move bodies
		b1.moveRewind(timestep * rewind);
		b2.moveRewind(timestep * rewind);

		// setChangedAndNotifyObservers();
	}
	
	public void calculateForcesAndUpdateVelocities(int id) {
		double distance, magnitude;
		Point2D.Double direction = new Point2D.Double();
		Point2D.Double deltaV = new Point2D.Double();
		
		for (int i = id-1; i < nBodies - 1; i+= id) {
			for (int j = i+1; j < nBodies; j++) {
				// get the distance of the two bodies
				System.out.println("Id: " + id + " checking bodies " + i + " and " + j);
				double temp = (Math.pow((bodies[i].getXPos() - bodies[j].getXPos()), 2)
						+ Math.pow((bodies[i].getYPos() - bodies[j].getYPos()), 2));
				distance = Math.sqrt(temp);

				// Force = (G * m1 * m2) / (distance^2)
				magnitude = (G * bodies[i].getMass() * bodies[j].getMass()) / Math.pow(distance, 2);

				// Direction is the vector of the difference between the two x and y positions
				// direction = new Point2D.Double((bodies[j].getXPos() - bodies[i].getXPos()),
				// (bodies[j].getYPos() - bodies[i].getYPos()));
				direction.setLocation((bodies[j].getXPos() - bodies[i].getXPos()),
						(bodies[j].getYPos() - bodies[i].getYPos()));

				// calculate values of the forces for x and y components, and add them to the
				// net forces
				double ix = (forces[i].getX() + (magnitude * direction.getX()) / distance);
				double jx = (forces[j].getX() + (magnitude * -direction.getX()) / distance); // j is opposite direction
				double iy = (forces[i].getY() + (magnitude * direction.getY()) / distance);
				double jy = (forces[j].getY() + (magnitude * -direction.getY()) / distance); // j is opposite direction

				// set the net force to be the net calculated just previously
				forces[i].setLocation(ix, iy);
				forces[j].setLocation(jx, jy);
				
				//update velocities
				// Velocity = (Force / Mass) * timestep. This is F = ma derived for velocity
				bodies[i].setOldForce(forces[i]);
				// deltaV = new Point2D.Double(forces[i].getX() / bodies[i].getMass(),
				// forces[i].getY() / bodies[i].getMass());
				deltaV.setLocation(forces[i].getX() / bodies[i].getMass(), forces[i].getY() / bodies[i].getMass());
				bodies[i].changeVelocityBy(deltaV, timestep);
				
				//move body
				bodies[i].move(timestep);
				// testing
				double xCorner = bodies[i].getXPos() - bodies[i].getRadius();
				double yCorner = bodies[i].getYPos() - bodies[i].getRadius();
				double width = bodies[i].getRadius() * 2;
				double height = bodies[i].getRadius() * 2;
				
				gc.strokeOval(xCorner, yCorner, width, height);
				
			}
		}
	}

	/*
	 * calculateForces() calculates the net force on every pair of bodies and
	 * returns the array of forces of each associated body. The index of the forces
	 * matches the index of its associated body.
	 */
	public Point2D.Double[] calculateForces() {
		double distance, magnitude;
		Point2D.Double direction = new Point2D.Double();

		for (int i = 0; i < nBodies - 1; i++) {
			for (int j = i + 1; j < nBodies; j++) {
				// get the distance of the two bodies
				double temp = (Math.pow((bodies[i].getXPos() - bodies[j].getXPos()), 2)
						+ Math.pow((bodies[i].getYPos() - bodies[j].getYPos()), 2));
				distance = Math.sqrt(temp);

				// Force = (G * m1 * m2) / (distance^2)
				magnitude = (G * bodies[i].getMass() * bodies[j].getMass()) / Math.pow(distance, 2);

				// Direction is the vector of the difference between the two x and y positions
				// direction = new Point2D.Double((bodies[j].getXPos() - bodies[i].getXPos()),
				// (bodies[j].getYPos() - bodies[i].getYPos()));
				direction.setLocation((bodies[j].getXPos() - bodies[i].getXPos()),
						(bodies[j].getYPos() - bodies[i].getYPos()));

				// calculate values of the forces for x and y components, and add them to the
				// net forces
				double ix = (forces[i].getX() + (magnitude * direction.getX()) / distance);
				double jx = (forces[j].getX() + (magnitude * -direction.getX()) / distance); // j is opposite direction
				double iy = (forces[i].getY() + (magnitude * direction.getY()) / distance);
				double jy = (forces[j].getY() + (magnitude * -direction.getY()) / distance); // j is opposite direction

				// set the net force to be the net calculated just previously
				forces[i].setLocation(ix, iy);
				forces[j].setLocation(jx, jy);
			}
		}

		return forces;
	}

	/*
	 * updateVelocitiesByForce() uses the given Point2D.Double[] of forces to update
	 * the velocity of every Body in bodies. The velocities are updated with a call
	 * to changeVelocityBy() and are scaled by the timestep.
	 */
	public void updateVelocitiesByForce(Point2D.Double[] forces) {
		Point2D.Double deltaV = new Point2D.Double(); // dv = f/m, dv = a

		for (int i = 0; i < nBodies; i++) {
			// Velocity = (Force / Mass) * timestep. This is F = ma derived for velocity
			bodies[i].setOldForce(forces[i]);
			// deltaV = new Point2D.Double(forces[i].getX() / bodies[i].getMass(),
			// forces[i].getY() / bodies[i].getMass());
			deltaV.setLocation(forces[i].getX() / bodies[i].getMass(), forces[i].getY() / bodies[i].getMass());
			bodies[i].changeVelocityBy(deltaV, timestep);
		}
	}

	/**
	 * checkCollisions() checks if any two bodies in bodies are close enough to have
	 * collided; i.e. the distance from their centers is less than the sum of their
	 * radii. If two bodies are found to be collided, their velocities will be
	 * updated accordingly using each body's setVelocity() method.
	 */
	public void checkCollisions() {
		// check bodies for collisions and adjust only collided bodies accordingly

		for (int i = 0; i < bodies.length - 1; i++) {
			for (int j = i + 1; j < bodies.length; j++) {
				Body b1 = bodies[i];
				Body b2 = bodies[j];
				// if the distance between the bodies is less than the sum of their radii,
				// they've collided. Don't count collisions that have happened on last timestep
				if ((b1.getPosition().distance(b2.getPosition()) < (b1.getRadius() + b2.getRadius()))
						&& !b1.getPrevCollisions().contains(b2)) {
					numCollisions++;
					// System.out.println("Num collisions: " + numCollisions);
					// check within tolerance. If it's over the allowed tolerance, rewind until
					// they're not
					double overlap = (b1.getRadius() + b2.getRadius()) - b1.getPosition().distance(b2.getPosition());

					if (overlap > overlapTolerance) {
						// TODO: fix rewind value?
						double rewind = (overlapTolerance / overlap);
						rewind(b1, b2, rewind);
					}

					// add collision to list so we don't include it on the next timestep
					b1.addCollision(b2);

					double v1ix = b1.getVelocity().getX(); // initial x-velocity of body 1
					double v1iy = b1.getVelocity().getY(); // initial y-velocity of body 1
					double x1i = b1.getXPos(); // initial x-pos of body 1
					double y1i = b1.getYPos(); // initial y-pos of body 1

					double v2ix = b2.getVelocity().getX(); // initial x-velocity of body 2
					double v2iy = b2.getVelocity().getY(); // initial y-velocity of body 2
					double x2i = b2.getXPos(); // initial x-pos of body 2
					double y2i = b2.getYPos(); // initial y-pos of body 2

					/*
					 * blackNumerator, redNumerator, and denominator are variables that correspond
					 * the the portion of the associated letter equation they represent in the
					 * assignment files.
					 */

					// these equations calculate a new velocity for body 1
					double blackNumeratorA = v2ix * Math.pow(x2i - x1i, 2) + v2iy * (x2i - x1i) * (y2i - y1i);
					double redNumeratorA = v1ix * Math.pow(y2i - y1i, 2) - v1iy * (x2i - x1i) * (y2i - y1i);
					double denominatorA = Math.pow(x2i - x1i, 2) + Math.pow(y2i - y1i, 2);
					double v1fx = (blackNumeratorA + redNumeratorA) / denominatorA;

					double blackNumeratorB = v2ix * (x2i - x1i) * (y2i - y1i) + v2iy * Math.pow(y2i - y1i, 2);
					double redNumeratorB = v1ix * (y2i - y1i) * (x2i - x1i) + v1iy * Math.pow(x2i - x1i, 2);
					double denominatorB = Math.pow(x2i - x1i, 2) + Math.pow(y2i - y1i, 2);
					double v1fy = (blackNumeratorB - redNumeratorB) / denominatorB;

					b1.setVelocity(new Point2D.Double(v1fx, v1fy)); // update b1 velocity

					// these equations calculate a new velocity for body 2

					double blackNumeratorC = v1ix * Math.pow(x2i - x1i, 2) + v1iy * (x2i - x1i) * (y2i - y1i);
					double redNumeratorC = v2ix * Math.pow(y2i - y1i, 2) - v2iy * (x2i - x1i) * (y2i - y1i);
					double denominatorC = Math.pow(x2i - x1i, 2) + Math.pow(y2i - y1i, 2);
					double v2fx = (blackNumeratorC + redNumeratorC) / denominatorC;

					double blackNumeratorD = v1ix * (x2i - x1i) * (y2i - y1i) + v1iy * Math.pow(y2i - y1i, 2);
					double redNumeratorD = v2ix * (y2i - y1i) * (x2i - x1i) + v2iy * Math.pow(x2i - x1i, 2);
					double denominatorD = Math.pow(x2i - x1i, 2) + Math.pow(y2i - y1i, 2);
					double v2fy = (blackNumeratorD - redNumeratorD) / denominatorD;

					b2.setVelocity(new Point2D.Double(v2fx, v2fy));
				}

			}
			Body b1 = bodies[i];

			// Check collisions on border
			if (borderOn) {
				// Check two vertical walls
				if (b1.getXPos() <= b1.getRadius()
						|| b1.getXPos() >= (BORDER_WIDTH - b1.getRadius()) && !b1.getPrevXWallCollision()) {
					// switch x velocity
					Point2D.Double newVel = new Point2D.Double(-b1.getVelocity().getX(), b1.getVelocity().getY());
					b1.setVelocity(newVel);
					b1.setCurrXWallCollision(true);
				}
				// Check two horizontal walls
				if (b1.getYPos() <= b1.getRadius()
						|| b1.getYPos() >= (BORDER_HEIGHT - b1.getRadius()) && !b1.getPrevYWallCollision()) {
					// switch y velocity
					Point2D.Double newVel = new Point2D.Double(b1.getVelocity().getX(), -b1.getVelocity().getY());
					b1.setVelocity(newVel);
					b1.setCurrYWallCollision(true);
				}
			}
			b1.resetCollisions();
		}

		// check border collisions on last one that gets missed in for loop
		// Check collisions on border
		Body b1 = bodies[bodies.length - 1];
		if (borderOn) {
			if (b1.getXPos() <= b1.getRadius()
					|| b1.getXPos() >= (BORDER_WIDTH - b1.getRadius()) && !b1.getPrevXWallCollision()) {

				Point2D.Double newVel = new Point2D.Double(-b1.getVelocity().getX(), b1.getVelocity().getY());
				b1.setVelocity(newVel);
				b1.setCurrXWallCollision(true);
			}
			if (b1.getYPos() <= b1.getRadius()
					|| b1.getYPos() >= (BORDER_WIDTH - b1.getRadius()) && !b1.getPrevYWallCollision()) {
				Point2D.Double newVel = new Point2D.Double(b1.getVelocity().getX(), -b1.getVelocity().getY());
				b1.setVelocity(newVel);
				b1.setCurrYWallCollision(true);
			}
		}
		b1.resetCollisions();

	}

	/* getBodies() returns the array containing the bodies. */
	public Body[] getBodies() {
		return bodies;
	}

	@Override
	//TODO: change this
	public void run() {
		System.out.println("Process " + id + " running"); //testing
		// start timer
		long startTime = 0;
		if (id == 1) {
			startTime = System.nanoTime();
		}

		for (int i = 0; i < numTimesteps; i++) {
			calculateForcesAndUpdateVelocities(id);
			dissBar();
			if (id == 1) {
				
				
				checkCollisions();
				
				try {
					Thread.sleep(5);
				} catch (InterruptedException e) {
					System.out.println("Problem sleeping");
					e.printStackTrace();
				} 
				
				gc.clearRect(0, 0, canvasWidth, canvasHeight);
			}
			dissBar();

			// For testing purposes, in practice comment this
			if (id == 1 && i % 100 == 0)	System.out.println(i);
		}

		if (id == 1) {
			System.out.println("I have stopped");
			// end timer
			long endTime = System.nanoTime();
			long duration = (endTime - startTime);
			System.out.println("Time is " + duration / 1000000000 + " seconds, " + duration / 1000 + " microseconds");
			System.out.println("Detected collisions: " + numCollisions);
		}

		return;
	}

	public int getNumCollisions() {
		return numCollisions;
	}

	public void dissBar() {
		int semSize = (int) Math.ceil(Math.log(numThreads) / Math.log(2));

		for (int n = 0; n < semSize; n++) {
			// Calculate neighbor
			int neighbor = Math.floorMod((int) Math.pow(2, n) + (id-1), numThreads);

			dissBarrier[n][neighbor].release(); // let your neighbor know you've arrived
			try {
				dissBarrier[n][id-1].acquire(); // wait for your neighbor to arrive
			} catch (InterruptedException e) {
				System.out.println("Error acquiring neighbor");
				e.printStackTrace();
			}
		}
	}
}
