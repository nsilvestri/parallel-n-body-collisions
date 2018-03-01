package model;

import java.awt.geom.Point2D;
import java.util.Observable;

/* Space represents the 2D space that all objects exist within. This class is
 * the core class within the model, and contains the list of objects that
 * inhabit the space. */
public class Space extends Observable
{
	private final static double G = 6.67e-1; // gravitational constant, currently 10^10 times bigger than real life
	private final static double timestep = .1; // how quickly and accurately the simulation runs
	private Body[] bodies;
	private int nBodies;

	/* This constructor will be a random constructor of bodies with the given
	 * properties. */
	public Space(int nBodies, double mass, double radius)
	{
		this.nBodies = nBodies;
		bodies = new Body[nBodies];

		for (int i = 0; i < nBodies; i++)
		{
			// TODO: construct bodies with probably-random positions and velocities
		}
	}

	/* This constructor allows the initialization of preset bodies. */
	public Space(Body[] bodies)
	{
		this.nBodies = bodies.length;
		this.bodies = bodies;
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
		Point2D.Double[] forces = calculateForces();

		// currently this just adjusts the velocity for every body, and then each body
		// moves with the following for loop
		updateVelocitiesByForce(forces);

		for (Body b : bodies)
		{
			b.move(timestep);
		}

		setChangedAndNotifyObservers();
	}

	/* calculateForces() calculates the net force on every pair of bodies and
	 * returns the array of forces of each associated body. The index of the forces
	 * matches the index of its associated body. */
	public Point2D.Double[] calculateForces()
	{
		double distance, magnitude;
		Point2D.Double direction;

		Point2D.Double[] forces = new Point2D.Double[nBodies];

		// intialize Point2D.Double objects in array
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
		Point2D.Double deltaP; // dp = (v + dv/2)

		for (int i = 0; i < nBodies; i++)
		{
			// Velocity = (Force / Mass) * timestep. This is F = ma derived for velocity
			deltaV = new Point2D.Double(forces[i].getX() / bodies[i].getMass(), forces[i].getY() / bodies[i].getMass());

			deltaP = new Point2D.Double(((bodies[i].getVelocity().getX() + deltaV.getX()) / 2),
					((bodies[i].getVelocity().getY() + deltaV.getY()) / 2));

			bodies[i].changeVelocityBy(deltaV, timestep);
			
			// bodies[i].setVelocity(new Point2D.Double(newX, newY));

			// newX = (bodies[i].getXPos() + deltaP.getX());
			// newY = (bodies[i].getYPos() + deltaP.getY());
			// bodies[i].setPosition(new Point2D.Double(newX, newY));
		}
	}

	/* getBodies() returns the array containing the bodies. */
	public Body[] getBodies()
	{
		return bodies;
	}
}
