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

	private Space space;

	private Observer currentView;

	// when 0, animation is continuous
	// when 1, animation is controlled by us
	private static int stepByStepControl = 0;

	@Override
	public void start(Stage stage) throws Exception
	{
		/* initialize stage */
		stage.setTitle("n-Body Collisions");
		window = new BorderPane();

		/* Intialize Model */

		// hard coded test array
		Body[] bodies = new Body[2];
		bodies[0] = new Body(2e4, 20, 300, 300, 0, 0);
		bodies[1] = new Body(2e2, 20, 100, 300, 0, 8.167);
		// bodies[2] = new Body(20e4, 6, 300, 450, 9, 0);
		// bodies[3] = new Body(5e4, 2, 100, 300, 0, 8);
		space = new Space(bodies);

		// 2 bodies on collision course towards each other
		Body[] bodies2 = new Body[2];
		bodies2[0] = new Body(20, 20, 400, 300, -10, 0);
		bodies2[1] = new Body(20, 20, 100, 300, 10, 0);
		// space = new Space(bodies2);

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
		if (stepByStepControl == 0)
		{
			// the AnimationTimer moves the bodies and updates the observers of space 60
			// times per second
			new AnimationTimer()
			{
				@Override
				public void handle(long currentNanoTime)
				{
					space.moveBodies();
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
			// TODO Auto-generated method stub
			if (key.getCode() == KeyCode.SPACE)
			{
				space.moveBodies();
				space.setChangedAndNotifyObservers();
			}
		}

	}
}
