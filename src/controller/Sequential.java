package controller;

import java.awt.Point;

public class Sequential
{

	static Point point;
	static Point[] positions, velocities, forces;
	static double[] masses;
	static final double G = .6; // TODO: change this
	static int numBodies;
	static double dt; // delta time. Length of time step

	// parameters:
	// <number of bodies> <size of each body> <time steps>
	public static void main(String[] args)
	{
		numBodies = Integer.parseInt(args[0]);

		double massSize = Double.parseDouble(args[1]);

		// initialize positions
		// TODO: put in actual values
		positions = new Point[numBodies];

		// initialize velocities
		// TODO: put in actual values
		velocities = new Point[numBodies];

		// initialize forces
		// Don't need to initialize values because we calculate them
		forces = new Point[numBodies];

		// initialize masses
		// Assumed that bodies have equal masses
		masses = new double[numBodies];
		for (int i = 0; i < numBodies; i++)
		{
			masses[i] = massSize;
		}

		// initialize timesteps
		// Not sure what this value should be
		dt = Double.parseDouble(args[2]);

		// run the simulation
		for (int i = 0; i < dt; i++)
		{
			calculateForces();
			moveBodies();
		}
	}

	// calculate total force for every pair of bodies
	public static void calculateForces()
	{
		double distance, magnitude;
		Point direction;

		for (int i = 0; i < numBodies - 1; i++)
		{
			for (int j = i + 1; j < numBodies; j++)
			{
				double temp = (Math.pow((positions[i].getX() - positions[j].getX()), 2)
						+ Math.pow((positions[i].getY() - positions[j].getY()), 2));
				distance = Math.sqrt(temp);

				magnitude = (G * masses[i] * masses[j]) / Math.pow(distance, 2);

				direction = new Point((int) (positions[j].getX() - positions[i].getX()),
						(int) (positions[j].getY() - positions[i].getY()));

				int ix = (int) (forces[i].getX() + magnitude * direction.getX() / distance);
				int jx = (int) (forces[j].getX() + magnitude * direction.getX() / distance);
				int iy = (int) (forces[i].getY() + magnitude * direction.getY() / distance);
				int jy = (int) (forces[i].getY() + magnitude * direction.getY() / distance);

				forces[i].move(ix, iy);
				forces[j].move(jx, jy);
			}
		}
	}

	// calculate new velocity/position for every body
	public static void moveBodies()
	{
		Point deltaV; // dv = f/m * DT
		Point deltaP; // dp = (v + dv/2) * DT

		for (int i = 0; i < numBodies; i++)
		{
			deltaV = new Point((int) (forces[i].getX() / masses[i] * dt), (int) (forces[i].getY() / masses[i] * dt));

			deltaP = new Point((int) ((velocities[i].getX() + deltaV.getX() / 2) * dt),
					(int) ((velocities[i].getY() + deltaV.getY() / 2) * dt));

			int newX = (int) (velocities[i].getX() * deltaV.getX());
			int newY = (int) (velocities[i].getX() * deltaV.getX());
			velocities[i].move(newX, newY);

			newX = (int) (positions[i].getX() + deltaP.getX());
			newY = (int) (positions[i].getY() + deltaP.getY());
			positions[i].move(newX, newY);

			forces[i].move(0, 0);
		}
	}

}
