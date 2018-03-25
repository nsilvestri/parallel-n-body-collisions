package controller;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Scanner;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Semaphore;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import model.Body;
import model.SpaceThread;

/* Main is the class that starts the application. This initializes the stage,
 * scene, observers, and model of the program.
 * 
 */

public class ParallelCollisions extends Application {

	// Graphics
	private BorderPane window;
	private static final int WINDOW_WIDTH = 600;
	private static final int WINDOW_HEIGHT = 600;

	private SpaceThread[] spaceThreads;
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

	public static void main(String[] args) {

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

				System.out.println("Graphics on 'y'?");
				if (scanner.nextLine().equals("y"))
					graphicsOn = true;
				
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
		launch(args);
	}

	@Override
	public void start(Stage stage) throws Exception {

		// Randomized array that looked decent
		Body[] array = {
				new Body(25388.441286344863, 29.390840964489193, 448.9467795643991, 564.5256188402906,
						-9.291226629287745, -6.7910985178494006),
				new Body(187.05890681361907, 5.7190794606209305, 560.2787346228214, 554.6582250802596,
						-6.620672415580502, 2.482329560612527),
				new Body(11467.14417820409, 22.55027059732616, 442.126881645475, 437.32199775139327, -5.017058998361533,
						-12.143126544126945),
				new Body(348.84174578094377, 7.03951627092418, 533.1709300693376, 253.36976522346126,
						-1.073590481525681, 1.8662525049244323),
				new Body(39364.607674990024, 34.017467286285886, 265.19624294561584, 40.4990576995452,
						-11.32506249147888, 7.914679504073565),
				new Body(2390.2772048938787, 13.370554644426129, 69.10206141819627, 19.72765613216687,
						-1.4227619587879783, -8.238036887458327),
				new Body(84736.41202364455, 43.922800584843884, 185.80587961282615, 311.99644955987344,
						-7.19690127613096, -8.20265328376879),
				new Body(36895.79379853321, 33.29090638331068, 408.6185796661888, 312.0772879068162, -9.030191563134974,
						-12.937019779130122),
				new Body(419.7934102355361, 7.487644309346576, 550.5450315452638, 15.422124159358741, 0.473104024527343,
						1.8473554825013672),
				new Body(22115.946501668193, 28.06953233286172, 328.1435961310157, 259.27366348952455,
						10.833319076798809, -3.469452207277472) };

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

		Canvas canvas = new Canvas(WINDOW_WIDTH, WINDOW_HEIGHT);

		// initialize spacethreads
		spaceThreads = new SpaceThread[numThreads];
		spaceThreads[0] = new SpaceThread(numBodies, bodyMass, bodyRadius, zeroVel, borderSize);
		// spaceThreads[0] = new SpaceThread(array1);
		// spaceThreads[0] = new SpaceThread(3, 3, 10, false);

		spaceThreads[0].setOptions(graphicsOn, borderOn, dissemination, timingOn);
		for (int i = 0; i < numThreads; i++) {
			spaceThreads[i] = new SpaceThread(spaceThreads[0].getBodies());
			spaceThreads[i].setNumTimesteps(numTimesteps);
			spaceThreads[i].setParallelmeters(i, numThreads, dissBarrier, cycBarrier);
			spaceThreads[i].setCanvas(canvas);
		}

		/* initialize stage */

		if (graphicsOn) {
			stage.setTitle("n-Body Collisions");
			window = new BorderPane();
			window.setTop(canvas);
			Scene scene = new Scene(window, WINDOW_WIDTH, WINDOW_HEIGHT);
			stage.setScene(scene);
			stage.show();
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

		// if we join the threads, the graphics doesn't work

		if (!graphicsOn) {
			for (int i = 0; i < numThreads; i++) {
				spaceThreads[i].join();
			}
		} 

		// write final body positions / velocities to file
		/*
		writer = new PrintWriter(new FileWriter("FinalBodies.txt"));
		for (Body b : spaceThreads[0].getBodies()) {
			writer.println(b.toString());
		}
		writer.close();
		System.out.println("Printed final positions and velocities to FinalBodies.txt");
		*/

		// timing
		if (timingOn) {
			long avg = 0;
			for (int i = 0; i < numThreads; i++) {
				avg += spaceThreads[i].getBarrierTime();
				System.out.println(
						"Process " + i + " spent " + spaceThreads[i].getBarrierTime() + " nanoseconds in the barrier");
			}
			avg /= numThreads;
			System.out.println("The average time was " + avg + " nanoseconds.");
		}

		return;

	}
}
