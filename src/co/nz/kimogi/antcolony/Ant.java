package co.nz.kimogi.antcolony;

import java.awt.Color;
import java.awt.Rectangle;

public class Ant
{
	public int id;
	public Rectangle rect;
	public Color color;
	public boolean isBlocked = false;
	
	public Ant(int id, Rectangle rect)
	{
		this.id = id;
		this.rect = rect;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		Ant ant = (Ant)obj;
		return id == ant.id;
	}
	
	@Override
	public int hashCode()
	{
		return id;
	}
}
