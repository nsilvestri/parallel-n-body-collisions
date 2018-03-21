package controller;

import java.util.Observer;
import java.util.concurrent.Semaphore;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import model.Body;
import model.Space;
import model.SpaceThread;
import view.CanvasView;

/* Main is the class that starts the application. This initializes the stage,
 * scene, observers, and model of the program.
 * 
 */

public class ParallelMain extends Application
{
	public static void main(String[] args)
	{
		/* TODO: eventually make this work from command line and not visually, but for
		 * now this is easier to develop so we can see the stuff go. */
		launch(args);
	}

	private BorderPane window;

	private static final int WINDOW_WIDTH = 600;
	private static final int WINDOW_HEIGHT = 600;
	
	private static final int ANIMATION_SPEED = 1; // scales the speed of the animation. 1 is 1x, 2 is 2x, etc

	private SpaceThread[] spaceThreads;

	// when false, animation is continuous
	// when true, animation is controlled by the user
	private static boolean stepByStepControl = true;

	@Override
	public void start(Stage stage) throws Exception
	{
		/* initialize stage */
		stage.setTitle("n-Body Collisions");
		window = new BorderPane();

		/* Initialize Model */

		Body[] array = 
		{new Body(25388.441286344863, 29.390840964489193, 448.9467795643991, 564.5256188402906, -9.291226629287745, -6.7910985178494006),
		new Body(187.05890681361907, 5.7190794606209305, 560.2787346228214, 554.6582250802596, -6.620672415580502, 2.482329560612527),
		new Body(11467.14417820409, 22.55027059732616, 442.126881645475, 437.32199775139327, -5.017058998361533, -12.143126544126945),
		new Body(348.84174578094377, 7.03951627092418, 533.1709300693376, 253.36976522346126, -1.073590481525681, 1.8662525049244323),
		new Body(39364.607674990024, 34.017467286285886, 265.19624294561584, 40.4990576995452, -11.32506249147888, 7.914679504073565),
		new Body(2390.2772048938787, 13.370554644426129, 69.10206141819627, 19.72765613216687, -1.4227619587879783, -8.238036887458327),
		new Body(84736.41202364455, 43.922800584843884, 185.80587961282615, 311.99644955987344, -7.19690127613096, -8.20265328376879),
		new Body(36895.79379853321, 33.29090638331068, 408.6185796661888, 312.0772879068162, -9.030191563134974, -12.937019779130122),
		new Body(419.7934102355361, 7.487644309346576, 550.5450315452638, 15.422124159358741, 0.473104024527343, 1.8473554825013672),
		new Body(22115.946501668193, 28.06953233286172, 328.1435961310157, 259.27366348952455, 10.833319076798809, -3.469452207277472)};
		
		//Two bodies that will overlap over allowed tolerance
		Body[] array1 = {
				new Body(10, 30, 120, 200, -1, 0),
				new Body(10, 30, 60, 200, 1, 0),
				new Body(10, 30, 200, 200, -1, 0),
				new Body(10, 30, 400, 200, 1, 0)
				};
		
		Space setup = new Space(8, 3, 10, false);
		//Space setup = new Space(array);
		
		int numThreads = 2;
		long numTimesteps = 50000L; //higher this is, longer it runs
		
		//set up dissemination barrier for threads
		int semSize = (int) Math.ceil(Math.log(numThreads) / Math.log(2)) ;
		Semaphore[][] barrier = new Semaphore[semSize][numThreads];
		for (int n = 0; n < semSize; n++) {
			for (int i = 0; i < numThreads; i++) {
				barrier[n][i] = new Semaphore(0);
			}
		}
		
		Canvas canvas = new Canvas(WINDOW_WIDTH, WINDOW_HEIGHT);
		
		//initialize spacethreads
		spaceThreads = new SpaceThread[numThreads];
		for (int i = 0; i < numThreads; i++) {
			spaceThreads[i] = new SpaceThread(setup.getBodies());
			spaceThreads[i].setNumTimesteps(numTimesteps);
			spaceThreads[i].setParallelmeters(i, numThreads, barrier);
			spaceThreads[i].setCanvas(canvas);
		}
		
		window.setCenter(canvas);

		/* finish up the stage */
		//space.setChangedAndNotifyObservers();
		Scene scene = new Scene(window, WINDOW_WIDTH, WINDOW_HEIGHT);
		stage.setScene(scene);
		stage.show();

		
		
		//Start the threads 
		for (int i = 0; i < numThreads; i++) {
			spaceThreads[i].start();
		}
		
		
	}

	
}
