package view;

import java.util.Observable;
import java.util.Observer;

import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.BorderPane;
import model.Body;
import model.Space;

public class CanvasView extends BorderPane implements Observer
{
	private Space space;

	private Canvas canvas;
	private GraphicsContext gc;
	private int canvasWidth;
	private int canvasHeight;

	public CanvasView(Space space, int width, int height)
	{
		this.space = space;
		this.canvasWidth = width;
		this.canvasHeight = height;

		canvas = new Canvas(width, height);
		gc = canvas.getGraphicsContext2D();

		this.setCenter(canvas);
		
		//space.setGC(gc, canvasWidth, canvasHeight);
	}

	/* update() is called whenever the observed Space object calls
	 * notifyObservers(). In this implementation, this happens to be 60 times per
	 * second, because of the AnimationTimer in Main.java. */
	@Override
	public void update(Observable o, Object arg)
	{
		space = (Space) o;

		gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight()); // reset the canvas
		gc.setStroke(Color.BLACK);
		
		gc.strokeRect(0, 0, canvasWidth, canvasHeight);
		
		// draw each body
		
		for (Body b : space.getBodies())
		{
			// x and y coord need to be offset from the center to the corner in
			// order to be drawn with strokeOval().
			double xCorner = b.getXPos() - b.getRadius();
			double yCorner = b.getYPos() - b.getRadius();
			double width = b.getRadius() * 2;
			double height = b.getRadius() * 2;
			gc.strokeOval(xCorner, yCorner, width, height);
		}
		/*
		for (int i = 0; i < space.getBodies().length; i++) {
			gc.setFill(Color.rgb(0, 255/(i+1), 255/(i+1)));
			Body b = space.getBodies()[i];
			double xCorner = b.getXPos() - b.getRadius();
			double yCorner = b.getYPos() - b.getRadius();
			double width = b.getRadius() * 2;
			double height = b.getRadius() * 2;
			gc.fillOval(xCorner, yCorner, width, height);
		} */
	}
}
