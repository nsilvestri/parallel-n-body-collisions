package model;

import java.util.Observable;

/* Space represents the 2D space that all objects exist within. This class is
 * the core class within the model, and contains the list of objects that
 * inhabit the space. */
public class Space extends Observable
{
	private Body[] bodies;

	/* This constructor will be a random constructor of bodies with the given
	 * properties. */
	public Space(int nBodies, double mass, double radius)
	{
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
	
	public void update()
	{
		setChanged();
		notifyObservers();
	}

	/* moveBodies calls each body's move() method to recalculate their new
	 * positions. It does not take collisions into account. */
	public void moveBodies()
	{
		for (Body b : bodies)
		{
			b.move();
		}
	}
	
	/* getBodies() returns the array containing the bodies. */
	public Body[] getBodies()
	{
		return bodies;
	}
}
