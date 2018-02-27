package model;

import java.awt.geom.Point2D;

/* Body is the objects that inhabit Space and orbit around each other. They have
 * mass, radius, position, velocity
 */
public class Body
{
	private double mass;
	private double radius;
	private Point2D.Double position;
	private Point2D.Double velocity;

	public Body(double mass, double radius, double x, double y, double vx, double vy)
	{
		this.mass = mass;
		this.radius = radius;
		position = new Point2D.Double(x, y);
		velocity = new Point2D.Double(vx, vy);
	}

	/* move() changes this body's position based on its velocity. If its velocity
	 * needed to be changed, it should be done before this method is called. */
	public void move()
	{
		position.x += velocity.getX();
		position.y += velocity.getY();
	}
	
	/* getPosition() returns the Point2D.Double that stores this body's
	 * position.
	 */
	public Point2D.Double getPosition()
	{
		return position;
	}
	
	/* getVelocity() returns the Point2D.Double that stores this body's
	 * velocity.
	 */
	public Point2D.Double getVelocity()
	{
		return velocity;
	}
	
	/* getMass() returns this body's mass. */
	public double getMass()
	{
		return mass;
	}
	
	/* getRadius returns this body's radius. */
	public double getRadius()
	{
		return radius;
	}
}