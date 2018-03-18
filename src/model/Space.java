package model;

import java.awt.geom.Point2D;
import java.util.Observable;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/* Space represents the 2D space that all objects exist within. This class is
 * the core class within the model, and contains the list of objects that
 * inhabit the space. */
public class Space extends Observable implements Runnable
{
	private final static double G = 6.67e-2; // gravitational constant, currently 10^8 times bigger than real life
	private final static double timestep = 3 ; // tickrate of simulation, can be interpreted as units in "seconds"
	private Body[] bodies;
	private int nBodies;
	private final int BORDER_WIDTH = 600; //width constraint that bodies should stay in
	private final int BORDER_HEIGHT =600; //height constraint that bodies should stay in
	private long numTimesteps;
	private final double overlapTolerance = 3;
	private static int numCollisions = 0;
	
	public void setNumTimesteps(long n) {
		numTimesteps = n;
	}

	/* This constructor will be a random constructor of bodies with the given
	 * properties. */
	public Space(int nBodies, double mass, double radius)
	{
		this.nBodies = nBodies;
		bodies = new Body[nBodies];

		for (int i = 0; i < nBodies; i++)
		{
			// generates random positions and velocities for bodies
			double randX = ThreadLocalRandom.current().nextDouble(radius, BORDER_WIDTH - radius);
			double randY = ThreadLocalRandom.current().nextDouble(radius, BORDER_HEIGHT - radius);
			double randVX = ThreadLocalRandom.current().nextDouble(-8, 8);
			double randVY = ThreadLocalRandom.current().nextDouble(-8, 8);

			bodies[i] = new Body(mass, radius, randX, randY, randVX, randVY);
		}
	}

	/* This constructor allows the initialization of preset bodies. */
	public Space(Body[] bodies)
	{
		this.nBodies = bodies.length;
		this.bodies = bodies;
	}

	/**
	 * This constructor creates nBodies bodies of random mass and size. The mass is
	 * the cube of the radius.
	 */
	public Space(int nBodies)
	{
		bodies = new Body[nBodies];

		for (int i = 0; i < nBodies; i++)
		{
			double randRadius = ThreadLocalRandom.current().nextDouble(2, 50);
			double mass = Math.pow(randRadius, 3);
			double randX = ThreadLocalRandom.current().nextDouble(randRadius, BORDER_WIDTH - randRadius);
			double randY = ThreadLocalRandom.current().nextDouble(randRadius, BORDER_HEIGHT - randRadius);
			double randVX = ThreadLocalRandom.current().nextDouble(-15, 15);
			double randVY = ThreadLocalRandom.current().nextDouble(-15, 15);

			bodies[i] = new Body(mass, randRadius, randX, randY, randVX, randVY);
			
			//This line here to easier save scenarios (prints out the randomly generated parameters)
			System.out.println("new Body(" + mass + ", " + randRadius + ", " + randX + ", " + randY  + ", " + randVX + ", " + randVY + ")");
		}
	}

	/* setChangedAndNotifyObservers() calls setChanged() and notifyObservers(). */
	public void setChangedAndNotifyObservers()
	{
		setChanged();
		notifyObservers();
	}

	/* moveBodies() first recalculates new vx and vy for every body, and then calls
	 * each body's move() method to recalculate their new positions. It does not
	 * take collisions into account. */
	public void moveBodies()
	{
		// get the gravitational forces acting on every body
		Point2D.Double[] forces = calculateForces();

		// adjust the velocity for every body
		updateVelocitiesByForce(forces);

		// move each body according to its new velocity
		for (Body b : bodies)
		{
			b.move(timestep);
		}

		// check for collisions between any bodies
		checkCollisions();

		// last step is to notify observers of the new state
		setChangedAndNotifyObservers();
	}
	
	/* This method moves two bodies (b1, b2) by a fraction of a timestep (rewind). 
	 * This is called when two bodies have overlapped more than an allowed tolerance.
	 * <rewind> should be < 1. */
	public void rewind(Body b1, Body b2, double rewind) {
		//calculate forces
		Point2D.Double[] forces = rewindCalculateForces(b1, b2);
		
		//Update velocities by force
		Point2D.Double deltaV; // dv = f/m, dv = a
		
		// Velocity = (Force / Mass) * timestep. This is F = ma derived for velocity
		//Update first body's velocity
		deltaV = new Point2D.Double(b1.getOldForce().getX() / b1.getMass(), b1.getOldForce().getY() / b1.getMass());
		b1.changeOldVelocityBy(deltaV, timestep * rewind);
		
		//Update second body's velocity
		deltaV = new Point2D.Double(b2.getOldForce().getX()/ b2.getMass(), b2.getOldForce().getY() / b2.getMass());
		b2.changeOldVelocityBy(deltaV, timestep * rewind);
		
		//move bodies
		b1.moveRewind(timestep * rewind);
		b2.moveRewind(timestep * rewind);
		
		setChangedAndNotifyObservers();
	}
	
	public void rewind2(Body b1, Body b2, double rewind) {
		b1.rewind();
		b2.rewind();
		
		Point2D.Double[] forces = rewindCalculateForces(b1, b2);
		
		Point2D.Double deltaV;
		
		//update first body's velocity
		deltaV = new Point2D.Double(forces[0].getX() / b1.getMass(), forces[0].getY() / b1.getMass());
		b1.changeVelocityBy(deltaV,  timestep* rewind);
		
		//update second body's velocity
		deltaV = new Point2D.Double(forces[1].getX() / b2.getMass(), forces[1].getY() / b2.getMass());
		b2.changeVelocityBy(deltaV, timestep * rewind);
		
		b1.moveRewind(timestep * rewind);
		b2.moveRewind(timestep * rewind);
		System.out.println("B1 in rewind: " + b1.toString());
	}
	
	//This method calculates the forces of bodies <b1> <b2>. using their old positions
	//vs their new ones. 
	public Point2D.Double[] rewindCalculateForces(Body b1, Body b2) {
		double distance, magnitude;
		Point2D.Double direction;

		Point2D.Double[] forces = {new Point2D.Double(), new Point2D.Double()};

		
		// get the distance of the two bodies
		double temp = (Math.pow((b1.getOldXPos() - b2.getOldXPos()), 2)
				+ Math.pow((b1.getOldYPos() - b2.getOldYPos()), 2));

		distance = Math.sqrt(temp);

		// Force = (G * m1 * m2) / (distance^2)
		magnitude = (G * b1.getMass() * b2.getMass()) / Math.pow(distance, 2);

		// Direction is the vector of the difference between the two x and y positions
		direction = new Point2D.Double((b2.getOldXPos() - b1.getOldXPos()),
				(b2.getOldYPos() - b1.getOldYPos()));

		// calculate values of the forces for x and y components, and add them to the
		// net forces
		double ix = (forces[0].getX() + (magnitude * direction.getX()) / distance);
		double jx = (forces[1].getX() + (magnitude * -direction.getX()) / distance); // j is opposite direction
		double iy = (forces[0].getY() + (magnitude * direction.getY()) / distance);
		double jy = (forces[1].getY() + (magnitude * -direction.getY()) / distance); // j is opposite direction

		// set the net force to be the net calculated just previously
		forces[0].setLocation(ix, iy);
		forces[1].setLocation(jx, jy);

		return forces;
	}

	/* calculateForces() calculates the net force on every pair of bodies and
	 * returns the array of forces of each associated body. The index of the forces
	 * matches the index of its associated body. */
	public Point2D.Double[] calculateForces()
	{
		double distance, magnitude;
		Point2D.Double direction;

		Point2D.Double[] forces = new Point2D.Double[nBodies];

		// initialize Point2D.Double objects in array
		for (int i = 0; i < nBodies; i++)
		{
			forces[i] = new Point2D.Double();
		}

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
				direction = new Point2D.Double((bodies[j].getXPos() - bodies[i].getXPos()),
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
		Point2D.Double deltaV; // dv = f/m, dv = a

		for (int i = 0; i < nBodies; i++)
		{
			// Velocity = (Force / Mass) * timestep. This is F = ma derived for velocity
			bodies[i].setOldForce(forces[i]);
			deltaV = new Point2D.Double(forces[i].getX() / bodies[i].getMass(), forces[i].getY() / bodies[i].getMass());

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
				System.out.println("B1: " + b1.toString());
				// if the distance between the bodies is less than the sum of their radii,
				// they've collided. Don't count collisions that have happened on last timestep
				if ((b1.getPosition().distance(b2.getPosition()) < (b1.getRadius() + b2.getRadius())) &&
					!b1.getPrevCollisions().contains(b2) )
				{
					numCollisions++;
					System.out.println("Num collisions: " + numCollisions);
					//check within tolerance. If it's over the allowed tolerance, rewind until they're not
					double overlap = (b1.getRadius() + b2.getRadius()) - b1.getPosition().distance(b2.getPosition());
					System.out.println("Overlap is " + overlap);
					
					if (overlap > overlapTolerance ) {
						//TODO: fix rewind value
						System.out.println("Overlap BR: " + overlap); //overlap before rewind. Testing purposes
						System.out.println("B1 BR: " + b1.toString());
						System.out.println("B2 AR: " + b2.toString());
						double rewind = (overlapTolerance / overlap); //needs to be fixed I think
						System.out.println("Rewind is " + rewind); //testing purposes
						rewind(b1, b2, rewind);
						
						//testing purposes
						overlap = (b1.getRadius() + b2.getRadius()) - b1.getPosition().distance(b2.getPosition());
						System.out.println("Overlap AR: " + overlap); //overlap after rewind
						System.out.println("B1 AR: " + b1.toString());
						System.out.println("B2 AR: " + b2.toString());
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
			
			//Check collisions on border
			Body b1 = bodies[i];
			//Check two vertical walls
			if (b1.getXPos() <= b1.getRadius() || b1.getXPos() >= (BORDER_WIDTH - b1.getRadius()) && 
					!b1.getPrevXWallCollision()) {
				 
				Point2D.Double newVel = new Point2D.Double(-b1.getVelocity().getX(), b1.getVelocity().getY());
				b1.setVelocity(newVel);
				b1.setCurrXWallCollision(true);
			}
			//Check two horizontal walls
			if (b1.getYPos() <= b1.getRadius() || b1.getYPos() >= (BORDER_WIDTH - b1.getRadius()) &&
					!b1.getPrevYWallCollision()) {
				Point2D.Double newVel = new Point2D.Double(b1.getVelocity().getX(), -b1.getVelocity().getY());
				b1.setVelocity(newVel);
				b1.setCurrYWallCollision(true);
			}
			b1.resetCollisions();
		}
		
		//check border collisions on last one that gets missed in for loop
		//Check collisions on border
		Body b1 = bodies[bodies.length - 1];
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
		b1.resetCollisions();
		
	}

	/* getBodies() returns the array containing the bodies. */
	public Body[] getBodies()
	{
		return bodies;
	}

	@Override
	public void run() {
		for (int i = 0; i < numTimesteps; i++) {
			moveBodies();
			setChangedAndNotifyObservers();
			
			try {
				Thread.sleep(5);
			} catch (InterruptedException e) {
				System.out.println("Problem sleeping");
				e.printStackTrace();
			}
			
			if (i % 100 == 0) System.out.println(i);
		}
		
		System.out.println("I have stopped");
		//return;
	}
	
}
