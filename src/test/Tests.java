package test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.Test;

import model.Body;

/* Tests is the JUnit 5 testing class. */
public class Tests
{
	@Test
	public void testBodyNoPositionChangeWithNoVelocity()
	{
		Body b = new Body(0, 0, 0, 0, 0, 0);
		
		b.move();
		
		assertEquals(0.0, b.getPosition().getX());
		assertEquals(0.0, b.getPosition().getY());
	}
	
	@Test
	public void testBodyPositionChangeWithPositiveXVelocity()
	{
		Body b = new Body(0, 0, 0, 0, 10, 0);
		
		b.move();
		
		assertEquals(10.0, b.getPosition().getX());
		assertEquals(0.0, b.getPosition().getY());
	}
	
	@Test
	public void testBodyPositionChangeWithPositiveYVelocity()
	{
		Body b = new Body(0, 0, 0, 0, 0, 10);
		
		b.move();
		
		assertEquals(0.0, b.getPosition().getX());
		assertEquals(10.0, b.getPosition().getY());
	}
	
	@Test
	public void testBodyPositionChangeWithPositiveXandYVelocity()
	{
		Body b = new Body(0, 0, 0, 0, 10, 10);
		
		b.move();
		
		assertEquals(10.0, b.getPosition().getX());
		assertEquals(10.0, b.getPosition().getY());
	}
	
	@Test
	public void testBodyPositionChangeWithNegativeXVelocity()
	{
		Body b = new Body(0, 0, 0, 0, -10, 0);
		
		b.move();
		
		assertEquals(-10.0, b.getPosition().getX());
		assertEquals(0.0, b.getPosition().getY());
	}
	
	@Test
	public void testBodyPositionChangeWithNegativeYVelocity()
	{
		Body b = new Body(0, 0, 0, 0, 0, -10);
		
		b.move();
		
		assertEquals(0.0, b.getPosition().getX());
		assertEquals(-10.0, b.getPosition().getY());
	}
	
	@Test
	public void testBodyPositionChangeWithNegativeXandYVelocity()
	{
		Body b = new Body(0, 0, 0, 0, -10, -10);
		
		b.move();
		
		assertEquals(-10.0, b.getPosition().getX());
		assertEquals(-10.0, b.getPosition().getY());
	}
}
