package view;

import java.util.Observable;
import java.util.Observer;

import javafx.scene.layout.BorderPane;
import model.Space;

public class CanvasView extends BorderPane implements Observer
{
	private Space space;
	
	public CanvasView(Space space, int width, int height)
	{
		
	}
	
	@Override
	public void update(Observable o, Object arg)
	{
		
		
	}
}
