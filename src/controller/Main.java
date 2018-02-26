package controller;

import javafx.application.Application;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

/* Main is the class that starts the application. This initializes the stage, scene, observers, and model of the program.
 * 
 */

public class Main extends Application
{

	private static final int WINDOW_WIDTH = 600;
	private static final int WINDOW_HEIGHT = 600;
	
	
	public static void main(String[] args)
	{
		/* TODO: eventually make this work from command line and not visually, but for
		 * now this is easier to develop so we can see the stuff go. */
		launch(args);
	}

	private BorderPane window;

	@Override
	public void start(Stage stage) throws Exception
	{
		/* initialize stage */
		stage.setTitle("N-Body Collisions");
		window = new BorderPane();

		// TODO: initialize model

		// TODO: set up observers

		// finish up the stage
		Scene scene = new Scene(window, WINDOW_WIDTH, WINDOW_HEIGHT);
		stage.setScene(scene);
		stage.show();
	}

}
