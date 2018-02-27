package view;

import java.util.Observable;
import java.util.Observer;

import javafx.scene.paint.Color;

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
	
	public CanvasView(Space space, int width, int height)
	{
		this.space = space;
		
		canvas = new Canvas(width, height);
		gc = canvas.getGraphicsContext2D();
	}
	
	@Override
	public void update(Observable o, Object arg)
	{
		space = (Space) o;
		
		gc.setStroke(Color.BLACK);
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
	}
}
