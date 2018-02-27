package model;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.Observable;

/* Space represents the 2D space that all objects exist within. This class is
 * the core class within the model, and contains the list of objects that
 * inhabit the space. */
public class Space extends Observable
{
	private final static double G = .667; // gravitational constant
	private final static double timestep = .001;

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
		this.bodies = bodies;
	}

	/* This constructor does nothing just for quick initialization. */
	public Space()
	{

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
		moveBodiesByForce(forces);
	}

	/* calculateForces() calculates the net force on every pair of bodies. */
	public Point2D.Double[] calculateForces()
	{
		double distance, magnitude;
		Point2D.Double direction;

		Point2D.Double[] forces = new Point2D.Double[nBodies];

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
				direction = new Point2D.Double((int) (bodies[j].getXPos() - bodies[i].getXPos()),
						(int) (bodies[j].getYPos() - bodies[i].getYPos()));

				// calculate values of the forces for x and y components, and add them to the
				// net forces
				double ix = (forces[i].getX() + magnitude * direction.getX() / distance);
				double jx = (forces[j].getX() + magnitude * direction.getX() / distance);
				double iy = (forces[i].getY() + magnitude * direction.getY() / distance);
				double jy = (forces[i].getY() + magnitude * direction.getY() / distance);

				// set the net force to be the net calculated just previously
				forces[i].setLocation(ix, iy);
				forces[j].setLocation(jx, jy);
			}
		}
		
		return forces;
	}

	// calculate new velocity/position for every body
	public void moveBodiesByForce(Point2D.Double[] forces)
	{
		Point2D.Double deltaV; // dv = f/m * dt
		Point2D.Double deltaP; // dp = (v + dv/2) * DT

		for (int i = 0; i < nBodies; i++)
		{
			// Velocity = (Force / Mass) * timestep. This is just F = ma derived for velocity 
			deltaV = new Point2D.Double(((forces[i].getX() / bodies[i].getMass()) * timestep),
					(forces[i].getY() / bodies[i].getMass() * timestep));

			
			deltaP = new Point2D.Double( ((bodies[i].getVelocity().getX() + deltaV.getX() / 2) * timestep),
					(int) ((bodies[i].getVelocity().getY() + deltaV.getY() / 2) * timestep));

			double newX = (bodies[i].getVelocity().getX() * deltaV.getX());
			double newY = (bodies[i].getVelocity().getX() * deltaV.getX());
			bodies[i].getVelocity().setLocation(newX, newY);

			newX = (bodies[i].getXPos() + deltaP.getX());
			newY = (bodies[i].getYPos() + deltaP.getY());
			bodies[i].getPosition().setLocation(newX, newY);
		}
	}

	/* getBodies() returns the array containing the bodies. */
	public Body[] getBodies()
	{
		return bodies;
	}
}
