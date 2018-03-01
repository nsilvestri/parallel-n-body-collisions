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
	public void move(double timestep)
	{
		position.x += velocity.getX() * timestep;
		position.y += velocity.getY() * timestep;
	}

	/* setPosition() this body's position to the given position. */
	public void setPosition(Point2D.Double newPos)
	{
		position = newPos;
	}

	/* setPosition() this body's position to the given position. */
	public void setVelocity(Point2D.Double newVelocity)
	{
		velocity = newVelocity;
	}

	/* changeVelocityBy() adds the components of the given velocity vector to the
	 * current velocity vector. */
	public void changeVelocityBy(Point2D.Double deltaVelocity)
	{
		velocity.setLocation(velocity.getX() + deltaVelocity.getX(), velocity.getY() + deltaVelocity.getY());
	}

	/* getPosition() returns the Point2D.Double that stores this body's position. */
	public Point2D.Double getPosition()
	{
		return position;
	}

	/* getVelocity() returns the Point2D.Double that stores this body's velocity. */
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

	/* returns the X coordinate of this body's position. */
	public double getXPos()
	{
		return position.getX();
	}

	/* returns the Y coordinate of this body's position. */
	public double getYPos()
	{
		return position.getY();
	}

	/* toString() returns a String representation of this Body, as Body[xPos, yPos] */
	public String toString()
	{
		return "Body[" + position.getX() + ", " + position.getY() + "]";

	}
}
