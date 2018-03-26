import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Semaphore;

/* Main is the class that starts the application. This initializes the stage,
 * scene, observers, and model of the program.
 * 
 */

public class ParallelCollisions {

	// Graphics

	private static SpaceThread[] spaceThreads;
	static int numThreads = 1;
	static long numTimesteps = 1000L; // higher this is, longer it runs
	static boolean graphicsOn = false;
	static boolean dissemination = true; // dissemination or cyclic barrier
	static boolean borderOn = false;
	static boolean zeroVel = false;
	static boolean timingOn = true; // if true, processes log their time in the barrier
	static int numBodies;
	static double bodyRadius;
	static int borderSize = 4000;
	static double bodyMass;

	public static void main(String[] args) throws IOException, InterruptedException {

		/*
		 * TO RUN java ParallelCollisions <numThreads> <numBodies> <radius of bodies>
		 * <numTimesteps> <extra parameter prompts, 'y'>
		 * 
		 */

		if (args.length < 4) {
			System.out.println("To run: java ParallelCollisions <numThreads> <numBodies> <body radius> "
					+ "<numTimesteps> <extra parameters, 'y' (no by default)>");
			return;
		} else {
			numThreads = Integer.parseInt(args[0]);
			numBodies = Integer.parseInt(args[1]);
			bodyRadius = Double.parseDouble(args[2]);
			numTimesteps = Integer.parseInt(args[3]);
			borderSize = (int) ((bodyRadius*2*numBodies));
			// extra parameters prompting
			if (args.length == 5 && args[4].equals("y")) {
				// Barrier option
				System.out.println("<Dissemination 'd' or Cyclic 'c' barrier>\n");
				Scanner scanner = new Scanner(System.in);
				if (scanner.nextLine().equals("c"))
					dissemination = false;

				// Border option
				System.out.println("For a boundary border, enter 'y'");
				System.out.println("Calculated by (diameter * numBodies)");
				if (scanner.nextLine().equals("y")) {
					borderOn = true;
					
					System.out.println("Border size is " + borderSize);
				}

				// Zero velocity option
				System.out.println("Start bodies off with zero velocity? 'y' or 'n'");
				if (scanner.nextLine().equals("y"))
					zeroVel = true;
				
				scanner.close();
			}
		}

		// Calculate mass (radius ^ 3)
		bodyMass = bodyRadius * bodyRadius * bodyRadius;

		System.out.printf("Running with %d threads, %d bodies (radius %.2f, mass %.2f) for %d timesteps.\n", numThreads,
				numBodies, bodyRadius, bodyMass, numTimesteps);

		// print which barrier we're using
		if (dissemination)
			System.out.print("Using a dissemination barrier,");
		else
			System.out.printf("Using a cyclic barrier,");

		// print if border
		if (borderOn)
			System.out.printf(" with a border %d x %d.\n", borderSize, borderSize);
		else
			System.out.println(" with no border.");
		start();
	}

	public static void start() throws IOException, InterruptedException {

		// Four bodies in single line
		Body[] array1 = { new Body(10, 30, 120, 200, -1, 0), new Body(10, 30, 60, 200, 1, 0),
				new Body(10, 30, 200, 200, -1, 0), new Body(10, 30, 400, 200, 1, 0) };

		// set up dissemination barrier for threads
		int semSize = (int) Math.ceil(Math.log(numThreads) / Math.log(2));
		Semaphore[][] dissBarrier = new Semaphore[semSize][numThreads];
		for (int n = 0; n < semSize; n++) {
			for (int i = 0; i < numThreads; i++) {
				dissBarrier[n][i] = new Semaphore(0);
			}
		}

		// set up cyclic barrier for threads
		CyclicBarrier cycBarrier = new CyclicBarrier(numThreads);

		// initialize spacethreads
		spaceThreads = new SpaceThread[numThreads];
		spaceThreads[0] = new SpaceThread(numBodies, bodyMass, bodyRadius, zeroVel, borderSize);

		spaceThreads[0].setOptions(graphicsOn, borderOn, dissemination, timingOn);
		for (int i = 0; i < numThreads; i++) {
			spaceThreads[i] = new SpaceThread(spaceThreads[0].getBodies());
			spaceThreads[i].setNumTimesteps(numTimesteps);
			spaceThreads[i].setParallelmeters(i, numThreads, dissBarrier, cycBarrier);
			//spaceThreads[i].setCanvas(canvas);
		}

		// Print starting locations to a file
		PrintWriter writer = new PrintWriter(new FileWriter("StartingBodies.txt"));
		for (Body b : spaceThreads[0].getBodies()) {
			writer.println(b.toString());
		}
		writer.close();

		// Start the threads
		for (int i = 0; i < numThreads; i++) {
			spaceThreads[i].start();
		}

		//Join the threads
		for (int i = 0; i < numThreads; i++) {
			spaceThreads[i].join();
		}

		// timing
		if (timingOn) {
			long avg = 0;
			for (int i = 0; i < numThreads; i++) {
				avg += spaceThreads[i].getBarrierTime();
			}
			avg /= numThreads;
			System.out.println("Processes spent an average of " + avg / 1000+ " microsecs inside barriers");
		}

		return;

	}
}
