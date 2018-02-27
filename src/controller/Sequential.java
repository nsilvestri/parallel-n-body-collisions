package controller;

import java.awt.Point;

public class Sequential {
	
	static Point point;
	static Point[] positions, velocities, forces;
	static double[] masses;
	static final double G = .6; 
	static int numBodies;
	static double dt; //delta time. Length of time step

	//parameters: 
	//<number of bodies> <size of each body> <time steps>
	public static void main(String[] args) {
		numBodies = Integer.parseInt(args[0]);
		
		double massSize = Double.parseDouble(args[1]);
		
		//initialize positions
		//TODO: put in actual values
		positions = new Point[numBodies];
		
		//initialize velocities
		//TODO: put in actual values
		velocities = new Point[numBodies];
		
		//initialize forces
		//TODO: put in actual values??
		forces = new Point[numBodies];
		
		//initialize masses
		masses = new double[numBodies];
		for (int i = 0; i < numBodies; i++) {
			masses[i] = massSize;
		}
		
		//initialize timesteps 
		//Not sure what this value is
		dt = Double.parseDouble(args[2]);
		
		for (int i = 0; i < dt; i++) {
			calculateForces();
			moveBodies();
		}
	}
	
	public static void calculateForces() {
		double distance, magnitude;
		Point direction;
		
		for (int i = 0; i < numBodies-1; i++) {
			for (int j = i+1; j < numBodies; j++) {
				double temp = (Math.pow((positions[i].getX() - positions[j].getX()), 2) +
				Math.pow((positions[i].getY() - positions[j].getY()), 2));
				distance = Math.sqrt(temp);
				
				magnitude = (G*masses[i]*masses[j]) / Math.pow(distance, 2);
				
				direction = new Point((int) (positions[j].getX() - positions[i].getX()), 
						(int) (positions[j].getY() - positions[i].getY()));
				
				forces[i].x = (int) (forces[i].getX() + magnitude*direction.getX() / distance);
				forces[j].x = (int) (forces[j].getX() + magnitude*direction.getX() / distance);
				forces[i].y = (int) (forces[i].getY() + magnitude*direction.getY() / distance);
				forces[i].y = (int) (forces[i].getY() + magnitude*direction.getY() / distance);
				
			}
		}
	}
	
	public static void moveBodies() {
		Point deltaV; //dv = f/m * DT
		Point deltaP; //dp = (v + dv/2) * DT
		
		for (int i = 0; i < numBodies; i++) {
			deltaV = new Point((int) (forces[i].getX() / masses[i] * dt),
							   (int) (forces[i].getY() / masses[i] * dt));
			
			deltaP = new Point((int) ((velocities[i].getX() + deltaV.getX() / 2) * dt),
			(int) ((velocities[i].getY() + deltaV.getY() / 2) * dt));
			
			velocities[i].x = (int) (velocities[i].getX() * deltaV.getX());
			velocities[i].y = (int) (velocities[i].getX() * deltaV.getX());
			positions[i].x = (int) (positions[i].getX() + deltaP.getX());
			positions[i].y = (int) (positions[i].getY() + deltaP.getY());
			forces[i].x = 0; //reset force vector
			forces[i].y = 0;
		}
	}
	
	
}
