package model;

import java.awt.geom.Point2D;
import java.util.Observable;
import java.util.concurrent.ThreadLocalRandom;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;

/* Space represents the 2D space that all objects exist within. This class is
 * the core class within the model, and contains the list of objects that
 * inhabit the space. */
//public class Space extends Observable implements Runnable
public class Space extends Thread
{
	private final static double G = 6.67e-2; // gravitational constant, currently 10^8 times bigger than real life
	private final static double timestep = .1 ; // tickrate of simulation, can be interpreted as units in "seconds"
	private Body[] bodies;
	private int nBodies;
	private final int BORDER_WIDTH = 600; //width constraint that bodies should stay in
	private final int BORDER_HEIGHT =600; //height constraint that bodies should stay in
	private long numTimesteps;
	private final double overlapTolerance = .3;
	private static int numCollisions = 0;
	private Point2D.Double[] forces;
	private final static boolean borderOn = false;
	
	private GraphicsContext gc;
	private double canvasWidth;
	private double canvasHeight;
	private Canvas canvas;
	
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

	/* This constructor will be a random constructor of bodies with the given
	 * properties. */
	public Space(int nBodies, double mass, double radius, boolean zeroVel)
	{
		this.nBodies = nBodies;
		bodies = new Body[nBodies];
		forces = new Point2D.Double[nBodies];

		for (int i = 0; i < nBodies; i++)
		{
			// generates random positions and velocities for bodies
			double randX = ThreadLocalRandom.current().nextDouble(radius, BORDER_WIDTH - radius);
			double randY = ThreadLocalRandom.current().nextDouble(radius, BORDER_HEIGHT - radius);
			if (zeroVel) 
				bodies[i] = new Body(mass, radius, randX, randY, 0, 0);
			else {
				double randVX = ThreadLocalRandom.current().nextDouble(-8, 8);
				double randVY = ThreadLocalRandom.current().nextDouble(-8, 8);
				bodies[i] = new Body(mass, radius, randX, randY, randVX, randVY);
			}
			forces[i] = new Point2D.Double();
			
			//This line here to easier save scenarios (prints out the randomly generated parameters)
			System.out.println("new Body(" + mass + ", " + radius + ", " + randX + ", " + randY  + ", " + 
			bodies[i].getVelocity().getX() + ", " + bodies[i].getVelocity().getX() + ")");
		}
	}

	/* This constructor allows the initialization of preset bodies. */
	public Space(Body[] bodies)
	{
		this.nBodies = bodies.length;
		this.bodies = bodies;
		initializeForces();
	}

	/**
	 * This constructor creates nBodies bodies of random mass and size. The mass is
	 * the cube of the radius.
	 */
	public Space(int nBodies, boolean zeroVel)
	{
		bodies = new Body[nBodies];
		forces = new Point2D.Double[nBodies];

		for (int i = 0; i < nBodies; i++)
		{
			double randRadius = ThreadLocalRandom.current().nextDouble(2, 50);
			double mass = Math.pow(randRadius, 3);
			double randX = ThreadLocalRandom.current().nextDouble(randRadius, BORDER_WIDTH - randRadius);
			double randY = ThreadLocalRandom.current().nextDouble(randRadius, BORDER_HEIGHT - randRadius);
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

	/* setChangedAndNotifyObservers() calls setChanged() and notifyObservers(). */
	/*
	public void setChangedAndNotifyObservers()
	{
		setChanged();
		notifyObservers();
	} */

	/* moveBodies() first recalculates new vx and vy for every body, and then calls
	 * each body's move() method to recalculate their new positions. It does not
	 * take collisions into account. */
	public void moveBodies()
	{
		// get the gravitational forces acting on every body
		Point2D.Double[] forces = calculateForces();

		// adjust the velocity for every body
		updateVelocitiesByForce(forces);
		
		//testing
		gc.clearRect(0, 0, canvasWidth, canvasHeight);

		// move each body according to its new velocity
		for (Body b : bodies)
		{
			b.move(timestep);
			
			//testing
			double xCorner = b.getXPos() - b.getRadius();
			double yCorner = b.getYPos() - b.getRadius();
			double width = b.getRadius() * 2;
			double height = b.getRadius() * 2;
			gc.strokeOval(xCorner, yCorner, width, height);
			
			//testing
			if (b.getOldPosition().equals(b.getPosition()))
				System.out.println("Samesies");
		}

		// check for collisions between any bodies
		checkCollisions();

		// last step is to notify observers of the new state
		//setChangedAndNotifyObservers();
	}
	
	/* This method moves two bodies <b1, b2> by a fraction of a timestep <rewind>. 
	 * This is called when two bodies have overlapped more than an allowed tolerance.
	 * <rewind> should be < 1. */
	public void rewind(Body b1, Body b2, double rewind) {
	
		//Update velocities by force
		Point2D.Double deltaV; // dv = f/m, dv = a
		
		// Velocity = (Force / Mass) * timestep. This is F = ma derived for velocity
		//Recalculate first body's velocity
		deltaV = new Point2D.Double(b1.getOldForce().getX() / b1.getMass(), b1.getOldForce().getY() / b1.getMass());
		b1.changeOldVelocityBy(deltaV, timestep * rewind);
		
		//Recalculate second body's velocity
		deltaV = new Point2D.Double(b2.getOldForce().getX()/ b2.getMass(), b2.getOldForce().getY() / b2.getMass());
		b2.changeOldVelocityBy(deltaV, timestep * rewind);
		
		//move bodies
		b1.moveRewind(timestep * rewind);
		b2.moveRewind(timestep * rewind);
		
		//setChangedAndNotifyObservers();
	}

	/* calculateForces() calculates the net force on every pair of bodies and
	 * returns the array of forces of each associated body. The index of the forces
	 * matches the index of its associated body. */
	public Point2D.Double[] calculateForces()
	{
		double distance, magnitude;
		Point2D.Double direction = new Point2D.Double();
		

		for (int i = 0; i < nBodies - 1; i++)
		{
			for (int j = i + 1; j < nBodies; j++)
			{
				// get the distance of the two bodies
				double temp = (Math.pow((bodies[i].getXPos() - bodies[j].getXPos()), 2)
						+ Math.pow((bodies[i].getYPos() - bodies[j].getYPos()), 2));
				distance = Math.sqrt(temp);

				// Force = (G * m1 * m2) / (distance^2)
				magnitude = (G * bodies[i].getMass() * bodies[j].getMass()) / Math.pow(distance, 2);

				// Direction is the vector of the difference between the two x and y positions
				//direction = new Point2D.Double((bodies[j].getXPos() - bodies[i].getXPos()),
						//(bodies[j].getYPos() - bodies[i].getYPos()));
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

	/* updateVelocitiesByForce() uses the given Point2D.Double[] of forces to update
	 * the velocity of every Body in bodies. The velocities are updated with a call
	 * to changeVelocityBy() and are scaled by the timestep. */
	public void updateVelocitiesByForce(Point2D.Double[] forces)
	{
		Point2D.Double deltaV = new Point2D.Double(); // dv = f/m, dv = a

		for (int i = 0; i < nBodies; i++)
		{
			// Velocity = (Force / Mass) * timestep. This is F = ma derived for velocity
			bodies[i].setOldForce(forces[i]);
			//deltaV = new Point2D.Double(forces[i].getX() / bodies[i].getMass(), forces[i].getY() / bodies[i].getMass());
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
	public void checkCollisions()
	{
		// check bodies for collisions and adjust only collided bodies accordingly

		for (int i = 0; i < bodies.length - 1; i++)
		{
			for (int j = i + 1; j < bodies.length; j++)
			{
				Body b1 = bodies[i];
				Body b2 = bodies[j];
				// if the distance between the bodies is less than the sum of their radii,
				// they've collided. Don't count collisions that have happened on last timestep
				if ((b1.getPosition().distance(b2.getPosition()) < (b1.getRadius() + b2.getRadius())) &&
					!b1.getPrevCollisions().contains(b2) )
				{
					numCollisions++;
					//System.out.println("Num collisions: " + numCollisions);
					//check within tolerance. If it's over the allowed tolerance, rewind until they're not
					double overlap = (b1.getRadius() + b2.getRadius()) - b1.getPosition().distance(b2.getPosition());
					
					
					if (overlap > overlapTolerance ) {
						//TODO: fix rewind value?
						double rewind = (overlapTolerance / overlap); 
						rewind(b1, b2, rewind);
					} 
					
					//add collision to list so we don't include it on the next timestep
					b1.addCollision(b2);
					
					double v1ix = b1.getVelocity().getX(); // initial x-velocity of body 1
					double v1iy = b1.getVelocity().getY(); // initial y-velocity of body 1
					double x1i = b1.getXPos(); // initial x-pos of body 1
					double y1i = b1.getYPos(); // initial y-pos of body 1

					double v2ix = b2.getVelocity().getX(); // initial x-velocity of body 2
					double v2iy = b2.getVelocity().getY(); // initial y-velocity of body 2
					double x2i = b2.getXPos(); // initial x-pos of body 2
					double y2i = b2.getYPos(); // initial y-pos of body 2

					/* blackNumerator, redNumerator, and denominator are variables that correspond
					 * the the portion of the associated letter equation they represent in the
					 * assignment files.*/

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
			
			//Check collisions on border
			if (borderOn) {
				//Check two vertical walls
				if (b1.getXPos() <= b1.getRadius() || b1.getXPos() >= (BORDER_WIDTH - b1.getRadius()) && 
						!b1.getPrevXWallCollision()) {
					//switch x velocity
					Point2D.Double newVel = new Point2D.Double(-b1.getVelocity().getX(), b1.getVelocity().getY());
					b1.setVelocity(newVel);
					b1.setCurrXWallCollision(true);
				}
				//Check two horizontal walls
				if (b1.getYPos() <= b1.getRadius() || b1.getYPos() >= (BORDER_WIDTH - b1.getRadius()) &&
						!b1.getPrevYWallCollision()) {
					//switch y velocity
					Point2D.Double newVel = new Point2D.Double(b1.getVelocity().getX(), -b1.getVelocity().getY());
					b1.setVelocity(newVel);
					b1.setCurrYWallCollision(true);
				}
			}
			b1.resetCollisions();
		}
		
		//check border collisions on last one that gets missed in for loop
		//Check collisions on border
		Body b1 = bodies[bodies.length - 1];
		if (borderOn) {
			if (b1.getXPos() <= b1.getRadius() || b1.getXPos() >= (BORDER_WIDTH - b1.getRadius()) && 
					!b1.getPrevXWallCollision()) {
				 
				Point2D.Double newVel = new Point2D.Double(-b1.getVelocity().getX(), b1.getVelocity().getY());
				b1.setVelocity(newVel);
				b1.setCurrXWallCollision(true);
			}
			if (b1.getYPos() <= b1.getRadius() || b1.getYPos() >= (BORDER_WIDTH - b1.getRadius()) &&
					!b1.getPrevYWallCollision()) {
				Point2D.Double newVel = new Point2D.Double(b1.getVelocity().getX(), -b1.getVelocity().getY());
				b1.setVelocity(newVel);
				b1.setCurrYWallCollision(true);
			} 
		}
		b1.resetCollisions();
		
	}

	/* getBodies() returns the array containing the bodies. */
	public Body[] getBodies()
	{
		return bodies;
	}

	@Override
	public void run() {
		//start timer
		long startTime = System.nanoTime();
		
		for (int i = 0; i < numTimesteps; i++) {
			moveBodies();
			
			//setChangedAndNotifyObservers();
			
			try {
				Thread.sleep(5);
			} catch (InterruptedException e) {
				System.out.println("Problem sleeping");
				e.printStackTrace();
			} 
			
			//For testing purposes, in practice comment this
			if (i % 100 == 0) System.out.println(i);
		}
		
		System.out.println("I have stopped");
		//end timer
		long endTime = System.nanoTime();
		long duration = (endTime - startTime);
		System.out.println("Time is " + duration/1000000000 + " seconds, " + duration/1000 + " microseconds");
		System.out.println("Detected collisions: " + numCollisions);
		
		return;
	}
	
	public int getNumCollisions() {
		return numCollisions;
	}
	
}
