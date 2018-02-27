package controller;

import java.util.Observer;

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
		stage.setTitle("N-Body Collisions");
		window = new BorderPane();

		// TODO: initialize model
		
		Body[] bodies = new Body[1];
		bodies[0] = new Body(0, 50, 0, 0, 0, 0);
		space = new Space(bodies);
		
		
		/* intialize observer */
		
		// window width and height in CanvasView constructor means the canvas
		// will always be the full size of the intial window size.
		currentView = new CanvasView(space, WINDOW_WIDTH, WINDOW_HEIGHT);
		window.setCenter((Node) currentView);

		/* finish up the stage */
		Scene scene = new Scene(window, WINDOW_WIDTH, WINDOW_HEIGHT);
		stage.setScene(scene);
		stage.show();
	}

}
