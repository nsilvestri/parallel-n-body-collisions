

import java.awt.geom.Point2D;
import java.text.DecimalFormat;
import java.util.ArrayList;

/* Body is the objects that inhabit Space and orbit around each other. They have
 * mass, radius, position, velocity
 */
public class Body
{
	private double mass;
	private double radius;
	private Point2D.Double position;
	private Point2D.Double oldPosition;
	private Point2D.Double velocity;
	private Point2D.Double oldVelocity;
	private Point2D.Double oldForce;
	private Point2D.Double newForce;
	private ArrayList<Body> currCollisions;
	private ArrayList<Body> prevCollisions;
	private boolean prevXWallCollision;
	private boolean prevYWallCollision;
	private boolean currXWallCollision;
	private boolean currYWallCollision;
	

	/* Creates a new body with the given properties. */
	public Body(double mass, double radius, double x, double y, double vx, double vy)
	{
		this.mass = mass;
		this.radius = radius;
		position = new Point2D.Double(x, y);
		velocity = new Point2D.Double(vx, vy);
		oldForce = new Point2D.Double();
		newForce = new Point2D.Double();
		currCollisions = new ArrayList<Body>();
		prevCollisions = new ArrayList<Body>();
		oldPosition = new Point2D.Double(position.getX(), position.getY());
		oldVelocity = new Point2D.Double(velocity.getX(), velocity.getY());
		prevXWallCollision = false;
		prevYWallCollision = false;
		
	}
	
	public void addCollision(Body b) {
		currCollisions.add(b);
	}
	
	public ArrayList<Body> getPrevCollisions() {
		return prevCollisions;
	}
	
	public void resetCollisions() {
		prevCollisions = currCollisions;
		currCollisions = new ArrayList<>();
		prevXWallCollision = currXWallCollision;
		prevYWallCollision = currYWallCollision;
	}

	/* move() changes this body's position based on its velocity. If its velocity
	 * needed to be changed, it should be done before this method is called. */
	public void move(double timestep)
	{
		oldPosition.setLocation(position.getX(), position.getY());
		oldVelocity.setLocation(velocity.getX(), velocity.getY());
		position.x += velocity.getX() * timestep;
		position.y += velocity.getY() * timestep;
	}
	
	/*Similar to move, but uses oldPosition and oldVelocity. When this is
	 *called, the timestep will be a fraction of what it is originally to
	 *move the bodies in a smaller amount. */
	public void moveRewind(double timestep) {
		position.x = oldPosition.x + oldVelocity.getX() * timestep;
		position.y = oldPosition.y + oldVelocity.getY() * timestep;
	}
	

	/* setPosition() this body's position to the given position. */
	public void setPosition(Point2D.Double newPos)
	{
		oldPosition = position;
		position = newPos;
	}
	
	public void setOldForce(Point2D.Double f) {
		oldForce = newForce;
		newForce = f;
	}
	
	public Point2D.Double getOldForce() {
		return oldForce;
	}

	/* setPosition() this body's position to the given position. */
	public void setVelocity(Point2D.Double newVelocity)
	{
		oldVelocity = velocity;
		velocity = newVelocity;
	}
	


	/* changeVelocityBy() adds the components of the given velocity vector scaled by
	 * timestep to the current velocity vector. */
	public void changeVelocityBy(Point2D.Double deltaVelocity, double timestep)
	{
		double newVX = velocity.getX() + (deltaVelocity.getX() * timestep);
		double newVY = velocity.getY() + (deltaVelocity.getY() * timestep);
		oldVelocity.setLocation(velocity.getX(), velocity.getY());;
		velocity.setLocation(newVX, newVY);
	}
	
	/* Similar to changeVelocityBy , but uses old value. When this method is called,
	 * timestep will be a fraction of what it normally is to change the velocity in
	 * a smaller amount. */
	public void changeOldVelocityBy(Point2D.Double deltaVelocity, double timestep) {
		double newVX = oldVelocity.getX() + (deltaVelocity.getX() * timestep);
		double newVY = oldVelocity.getY() + (deltaVelocity.getY() * timestep);
		velocity.setLocation(newVX, newVY);
	}
	
	/* getPosition() returns the Point2D.Double that stores this body's position. */
	public Point2D.Double getOldPosition()
	{
		return oldPosition;
	}

	/* getVelocity() returns the Point2D.Double that stores this body's velocity. */
	public Point2D.Double getOldVelocity()
	{
		return oldVelocity;
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
	
	public double getOldXPos() {
		return oldPosition.getX();
	}
	
	public double getOldYPos() {
		return oldPosition.getY();
	}

	public boolean getPrevXWallCollision() {
		return prevXWallCollision;
	}


	public boolean getPrevYWallCollision() {
		return prevYWallCollision;
	}

	public void setCurrYWallCollision(boolean collision) {
		this.prevYWallCollision = collision;
	}
	
	public void setCurrXWallCollision(boolean collision) {
		this.prevXWallCollision = collision;
	}

	/* toString() returns a String representation of this Body, as Body Pos[xPos,
	 * yPos] Vel[xVel, yVel]*/
	public String toString()
	{
		DecimalFormat df = new DecimalFormat("#.000"); 
		//return "Body Pos(" + df.format(position.getX()) + ", " + df.format(position.getY()) + ")\t\t\t" +
					//"Vel[" + df.format(velocity.getX()) + ", " + df.format(velocity.getY()) + "]";
	
		return String.format("Body Pos(%11.3f, %11.3f)\t\tVel[%11.3f, %11.3f]", position.getX(), position.getY(),
				velocity.getX(), velocity.getY());
	}
}
