package controller;

import java.util.Observer;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import model.Body;
import model.Space;
import view.CanvasView;

/* Main is the class that starts the application. This initializes the stage,
 * scene, observers, and model of the program.
 * 
 */

public class Main extends Application
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
	
	private Space space;

	private Observer currentView;

	@Override
	public void start(Stage stage) throws Exception
	{
		/* initialize stage */
		stage.setTitle("n-Body Collisions");
		window = new BorderPane();

		/* Intialize Model */
		
		Body[] bodies = new Body[1];
		bodies[0] = new Body(0, 10, 0, 0, 1, 1);
		space = new Space(bodies);
		
		/* intialize observer */
		
		// window width and height in CanvasView constructor means the canvas
		// will always be the full size of the intial window size.
		currentView = new CanvasView(space, WINDOW_WIDTH, WINDOW_HEIGHT);
		space.addObserver(currentView);
		window.setCenter((Node) currentView);
		
		/* Animation Timer */
		
		// the AnimationTimer moves the bodies and updates the observers of space 60 times per second
		new AnimationTimer()
		{
			@Override
			public void handle(long currentNanoTime)
			{
				space.moveBodies();
				space.update();
			}
			
		}.start();

		/* finish up the stage */
		space.update();
		Scene scene = new Scene(window, WINDOW_WIDTH, WINDOW_HEIGHT);
		stage.setScene(scene);
		stage.show();
	}
}
