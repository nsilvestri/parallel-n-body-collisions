package controller;

import java.util.Observer;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
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
	
	private static final int ANIMATION_SPEED = 1; // scales the speed of the animation. 1 is 1x, 2 is 2x, etc

	private Space space;

	private Observer currentView;

	// when false, animation is continuous
	// when true, animation is controlled by the user
	private static boolean stepByStepControl = false;

	@Override
	public void start(Stage stage) throws Exception
	{
		/* initialize stage */
		stage.setTitle("n-Body Collisions");
		window = new BorderPane();

		/* Intialize Model */

		Body[] b = {new Body(2e6, 100, 120, 200, -2, 0)};
		space = new Space(10);

		/* intialize observer */

		// window width and height in CanvasView constructor means the canvas
		// will always be the full size of the intial window size.
		currentView = new CanvasView(space, WINDOW_WIDTH, WINDOW_HEIGHT);
		space.addObserver(currentView);
		window.setCenter((Node) currentView);

		/* finish up the stage */
		space.setChangedAndNotifyObservers();
		Scene scene = new Scene(window, WINDOW_WIDTH, WINDOW_HEIGHT);
		stage.setScene(scene);
		stage.show();

		/* Animation Timer */
		if (!stepByStepControl)
		{
			// the AnimationTimer moves the bodies and updates the observers of space 60
			// times per second
			new AnimationTimer()
			{
				@Override
				public void handle(long currentNanoTime)
				{
					// perform ANIMATION_SPEED steps before updating the current frame
					for (int i = 0; i < ANIMATION_SPEED; i++)
					{
						space.moveBodies();						
					}
					space.setChangedAndNotifyObservers();
				}

			}.start();
		}
		/* Pressing spacebar moves bodies */
		else
		{
			scene.setOnKeyPressed(new SpaceKeyListener());
			
		}
	}

	// When the SPACEbar (hahahahaha) is pressed, move bodies and update
	private class SpaceKeyListener implements EventHandler<KeyEvent>
	{

		@Override
		public void handle(KeyEvent key)
		{
			if (key.getCode() == KeyCode.SPACE)
			{
				space.moveBodies();
				space.setChangedAndNotifyObservers();
				System.out.println(space.getBodies()[0].toString());
			}
		}

	}
}
