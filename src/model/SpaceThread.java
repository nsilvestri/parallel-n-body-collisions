package model;

import java.awt.geom.Point2D;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;

public class SpaceThread extends Thread {

	private final static double G = 6.67e-2; // gravitational constant, currently 10^8 times bigger than real life
	private final static double timestep = .1 ; // tickrate of simulation, can be interpreted as units in "seconds"
	private Body[] bodies;
	private int nBodies;
	private static int borderSize;
	private long numTimesteps;
	private static double overlapTolerance;
	private Point2D.Double[] forces;
	private static boolean borderOn;
	private static boolean graphicsOn;
	private static boolean dissemination;
	private static boolean calculateTime;

	private static AtomicInteger numCollisions = new AtomicInteger(0);
	private static GraphicsContext gc;
	private double canvasWidth;
	private double canvasHeight;
	private static Canvas canvas;

	// parallelization variables
	private static int numThreads;
	private int id;
	private static Semaphore[][] dissBarrier;
	private static CyclicBarrier cycBarrier;
	
	private long barrierTime, barrierStartTime, barrierEndTime;
		
	/** 
	 * This constructor randomizes positions and velocities for the bodies.
	 * Each body will have the same set mass and radius.
	 * @param nBodies How many bodies there will be
	 * @param mass Mass for all bodies
	 * @param radius Radius for all bodies
	 * @param zeroVel If bodies should start with zero velocity
	 */
	public SpaceThread(int nBodies, double mass, double radius, boolean zeroVel, int borderSize)
	{
		this.nBodies = nBodies;
		bodies = new Body[nBodies];
		forces = new Point2D.Double[nBodies];
		SpaceThread.borderSize = borderSize;
		SpaceThread.overlapTolerance = radius / 2;

		for (int i = 0; i < nBodies; i++)
		{
			// generates random positions and velocities for bodies
			double randX = ThreadLocalRandom.current().nextDouble(radius, borderSize - radius);
			double randY = ThreadLocalRandom.current().nextDouble(radius, borderSize - radius);
			if (zeroVel) 
				bodies[i] = new Body(mass, radius, randX, randY, 0, 0);
			else {
				double randVX = ThreadLocalRandom.current().nextDouble(-8, 8);
				double randVY = ThreadLocalRandom.current().nextDouble(-8, 8);
				bodies[i] = new Body(mass, radius, randX, randY, randVX, randVY);
			}
			forces[i] = new Point2D.Double();
			
			//This line here to easier save scenarios (prints out the randomly generated parameters)
			//System.out.println("new Body(" + mass + ", " + radius + ", " + randX + ", " + randY  + ", " + 
			//bodies[i].getVelocity().getX() + ", " + bodies[i].getVelocity().getX() + ")");
		}
	}

	/* This constructor allows the initialization of preset bodies. */
	public SpaceThread(Body[] bodies)
	{
		this.nBodies = bodies.length;
		this.bodies = bodies;
		initializeForces();
	}

	/** 
	 * This constructor creates nBodies bodies of random mass and size. The mass is
	 * the cube of the radius.
	 * @param nBodies How many total bodies there will be
	 * @param zeroVel If the bodies should start with zero velocity
	 */
	public SpaceThread(int nBodies, boolean zeroVel)
	{
		bodies = new Body[nBodies];
		forces = new Point2D.Double[nBodies];

		for (int i = 0; i < nBodies; i++)
		{
			double randRadius = ThreadLocalRandom.current().nextDouble(2, 50);
			double mass = Math.pow(randRadius, 3);
			double randX = ThreadLocalRandom.current().nextDouble(randRadius, borderSize - randRadius);
			double randY = ThreadLocalRandom.current().nextDouble(randRadius, borderSize - randRadius);
			double randVX = 0, randVY = 0;
			if (zeroVel) 
				bodies[i] = new Body(mass, randRadius, randX, randY, 0, 0);
			else {
				randVX = ThreadLocalRandom.current().nextDouble(-15, 15);
				randVY = ThreadLocalRandom.current().nextDouble(-15, 15);
	
				bodies[i] = new Body(mass, randRadius, randX, randY, randVX, randVY);
			}
			
			forces[i]= new Point2D.Double();
			
			//This line here to easier save scenarios (prints out the randomly generated parameters)
			//System.out.println("new Body(" + mass + ", " + randRadius + ", " + randX + ", " + randY  + ", " + randVX + ", " + randVY + ")");
		}
	}
	
	public void setOptions(boolean graphics, boolean border, boolean dissBarrier, boolean time) {
		SpaceThread.graphicsOn = graphics;
		SpaceThread.borderOn = border;
		SpaceThread.dissemination = dissBarrier;
		SpaceThread.calculateTime = time;
		
		if (borderOn && borderSize == 0)
			borderSize = 1000;
	}
	
	/**
	 * This method sets parameters needed to run in parallel. 
	 * @param id Process's id
	 * @param threads How many total threads there are
	 * @param barrier A dissemination barrier
	 * @param cycBar A cyclic barrier
	 */
	public void setParallelmeters(int id, int threads, Semaphore[][] barrier, CyclicBarrier cycBar) {
		this.id = id;
		numThreads = threads;
		dissBarrier = barrier;
		cycBarrier = cycBar;
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
	
	/** 
	 * This method moves two bodies by a fraction of a timestep. Called when
	 * two bodies have overlapped more than an allowed tolerance.
	 * @param b1 Body 1
	 * @param b2 Body 2
	 * @param rewind Fraction of a timestep. Should be < 1
	 */
	public synchronized void rewind(Body b1, Body b2, double rewind) {

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

	}
	
	public void calculateForcesAndUpdateVelocities(int id) {
		double distance, magnitude;
		Point2D.Double direction = new Point2D.Double();
		Point2D.Double deltaV = new Point2D.Double();
		
		for (int i = id; i < nBodies - 1; i+= numThreads) {
			for (int j = i+1; j < nBodies; j++) {
				
				// get the distance of the two bodies
				double temp = (Math.pow((bodies[i].getXPos() - bodies[j].getXPos()), 2)
						+ Math.pow((bodies[i].getYPos() - bodies[j].getYPos()), 2));
				distance = Math.sqrt(temp);

				// Force = (G * m1 * m2) / (distance^2)
				magnitude = (G * bodies[i].getMass() * bodies[j].getMass()) / Math.pow(distance, 2);

				// Direction is the vector of the difference between the two x and y positions
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
				deltaV.setLocation(forces[i].getX() / bodies[i].getMass(), forces[i].getY() / bodies[i].getMass());
				bodies[i].changeVelocityBy(deltaV, timestep);
				
			}
		}

	}
	
	public void moveBodies(int id) {
		for (int i = id; i < nBodies; i+=numThreads) {
			bodies[i].move(timestep);
		}
	}
	
	public void checkCollisions(int id) {
		// check bodies for collisions and adjust only collided bodies accordingly

		for (int i = id; i < bodies.length - 1; i+= numThreads) {
			for (int j = i + 1; j < bodies.length; j++) {
				Body b1 = bodies[i];
				Body b2 = bodies[j];
				// if the distance between the bodies is less than the sum of their radii,
				// they've collided. Don't count collisions that have happened on last timestep
				if ((b1.getPosition().distance(b2.getPosition()) < (b1.getRadius() + b2.getRadius()))
						&& !b1.getPrevCollisions().contains(b2)) {
					numCollisions.incrementAndGet();
					// check within tolerance. If it's over the allowed tolerance, rewind until
					// they're not
					double overlap = (b1.getRadius() + b2.getRadius()) - b1.getPosition().distance(b2.getPosition());
					if (overlap > overlapTolerance) {
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

					// these equations calculate a new velocity for body 2

					double blackNumeratorC = v1ix * Math.pow(x2i - x1i, 2) + v1iy * (x2i - x1i) * (y2i - y1i);
					double redNumeratorC = v2ix * Math.pow(y2i - y1i, 2) - v2iy * (x2i - x1i) * (y2i - y1i);
					double denominatorC = Math.pow(x2i - x1i, 2) + Math.pow(y2i - y1i, 2);
					double v2fx = (blackNumeratorC + redNumeratorC) / denominatorC;

					double blackNumeratorD = v1ix * (x2i - x1i) * (y2i - y1i) + v1iy * Math.pow(y2i - y1i, 2);
					double redNumeratorD = v2ix * (y2i - y1i) * (x2i - x1i) + v2iy * Math.pow(x2i - x1i, 2);
					double denominatorD = Math.pow(x2i - x1i, 2) + Math.pow(y2i - y1i, 2);
					double v2fy = (blackNumeratorD - redNumeratorD) / denominatorD;
					
					//Update velocities of collided bodies
					collided(b1, v1fx, v1fy);
					collided(b2, v2fx, v2fy);					
				}
			}
			Body b1 = bodies[i];

			// Check collisions on border
			if (borderOn) {
				// Check two vertical walls
				if (b1.getXPos() <= b1.getRadius()
						|| b1.getXPos() >= (borderSize - b1.getRadius()) && !b1.getPrevXWallCollision()) {
					// switch x velocity
					Point2D.Double newVel = new Point2D.Double(-b1.getVelocity().getX(), b1.getVelocity().getY());
					b1.setVelocity(newVel);
					b1.setCurrXWallCollision(true);
				}
				// Check two horizontal walls
				if (b1.getYPos() <= b1.getRadius()
						|| b1.getYPos() >= (borderSize - b1.getRadius()) && !b1.getPrevYWallCollision()) {
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
		if (borderOn && id == 0) {
			if (b1.getXPos() <= b1.getRadius()
					|| b1.getXPos() >= (borderSize - b1.getRadius()) && !b1.getPrevXWallCollision()) {

				Point2D.Double newVel = new Point2D.Double(-b1.getVelocity().getX(), b1.getVelocity().getY());
				b1.setVelocity(newVel);
				b1.setCurrXWallCollision(true);
			}
			if (b1.getYPos() <= b1.getRadius()
					|| b1.getYPos() >= (borderSize - b1.getRadius()) && !b1.getPrevYWallCollision()) {
				Point2D.Double newVel = new Point2D.Double(b1.getVelocity().getX(), -b1.getVelocity().getY());
				b1.setVelocity(newVel);
				b1.setCurrYWallCollision(true);
			}
		}
		b1.resetCollisions();
	}
	
	/**
	 * This method is synchronized so that two threads won't try to change a body's
	 * velocity at the same time.
	 * @param b Body that needs its velocity changed
	 * @param velX
	 * @param velY
	 */
	public synchronized void collided(Body b, double velX, double velY) {
		b.setVelocity(new Point2D.Double(velX, velY));
	}
	
	public long getBarrierTime() {
		return barrierTime;
	}

	/* getBodies() returns the array containing the bodies. */
	public Body[] getBodies() {
		return bodies;
	}

	@Override
	public void run() {
		//start timer
		long startTime = 0;
		if (id == 0)
			startTime = System.nanoTime();
		
		

		//Run for set amount of timesteps
		for (int i = 0; i < numTimesteps; i++) {
			
			calculateForcesAndUpdateVelocities(id);
			barrier();
			moveBodies(id);
			barrier();
			checkCollisions(id);
			
			
			if (id == 0) {				
				//graphics option
				if (graphicsOn) {
					gc.clearRect(0, 0, canvasWidth, canvasHeight);
					for (int n = 0; n < nBodies; n++) {
						double xCorner = bodies[n].getXPos() - bodies[n].getRadius();
						double yCorner = bodies[n].getYPos() - bodies[n].getRadius();
						double width = bodies[n].getRadius() * 2;
						double height = bodies[n].getRadius() * 2;			
						gc.strokeOval(xCorner, yCorner, width, height);
					} 
					//It's easier to see graphics when they're on the screen for longer
					try {
						Thread.sleep(5);
					} catch (InterruptedException e) {
						System.out.println("Problem sleeping");
						e.printStackTrace();
					} 
				} //end if graphics on
			}
			barrier();
			// For testing purposes, in practice comment this
			if (id == 0 && i % 100 == 0)	System.out.println(i);
		} //end for loop
		
		//Print number of collisions at the end
		if (id == 0) {
			// end timer
			long endTime = System.nanoTime();
			long duration = (endTime - startTime);
			System.out.println("Time is " + duration / 1000000000 + " seconds, " + duration / 1000 + " microseconds");
			System.out.println("Detected collisions: " + numCollisions);
		}
		
		//Write final bodies to file
		if (id == 0) {
			PrintWriter writer = null;
			try {
				writer = new PrintWriter(new FileWriter("FinalBodies.txt"));
			} catch (IOException e) {
				e.printStackTrace();
			}
			for (Body b : bodies) {
				writer.println(b.toString());
			}
			writer.close();
			System.out.println("Printed final positions and velocities to FinalBodies.txt");
		}
		return;
	} 

	public int getNumCollisions() {
		return numCollisions.get();
	}
	
	public void barrier() {
		if (calculateTime) 
			barrierStartTime = System.nanoTime();
			
		if (dissemination)
			dissBar();
		else
			try {
				cycBarrier.await();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (BrokenBarrierException e) {
				e.printStackTrace();
			}
		
		if (calculateTime) {
			barrierEndTime = System.nanoTime();
			barrierTime += (barrierEndTime - barrierStartTime);
		}
			
		
	}

	/**
	 * Standard dissemination barrier
	 */
	public void dissBar() {
		int semSize = (int) Math.ceil(Math.log(numThreads) / Math.log(2));

		for (int n = 0; n < semSize; n++) {
			// Calculate neighbor
			int neighbor = Math.floorMod((int) Math.pow(2, n) + (id), numThreads);

			dissBarrier[n][neighbor].release(); // let your neighbor know you've arrived
			try {
				dissBarrier[n][id].acquire(); // wait for your neighbor to arrive
			} catch (InterruptedException e) {
				System.out.println("Error acquiring neighbor");
				e.printStackTrace();
			}
		}
	}
}