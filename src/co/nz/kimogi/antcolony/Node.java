package co.nz.kimogi.antcolony;

import java.util.ArrayList;

public class Node
{
	public Ant ant;
	public Node first = null;
	public Node second = null;
	public ArrayList<Node> childen = new ArrayList<Node>();
	
	public Node(Ant ant)
	{
		this.ant = ant;
	}
}
