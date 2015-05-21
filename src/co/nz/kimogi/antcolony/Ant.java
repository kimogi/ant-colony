package co.nz.kimogi.antcolony;

import java.awt.Rectangle;

public class Ant
{
	public Rectangle rect;
	public boolean isFollower = false;
	public boolean isLeader = false;
	
	public Ant(Rectangle rect, boolean isLeader)
	{
		this.rect = rect;
		this.isLeader = isLeader;
	}
}
