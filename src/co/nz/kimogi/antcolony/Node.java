package co.nz.kimogi.antcolony;

import java.util.ArrayList;
import java.util.Comparator;

public class Node implements Comparable<Node>
{
	public Ant ant;
	public Node first = null;
	public Node second = null;
	public ArrayList<Node> childen = new ArrayList<Node>();

	public Node(Ant ant)
	{
		this.ant = ant;
	}

	@Override
	public int compareTo(Node node)
	{
		return this.childen.size() - node.childen.size();
	}

	public static Comparator<Node> ascendingNodeComparator = new Comparator<Node>()
	{
		public int compare(Node node1, Node node2)
		{
			return node1.compareTo(node2);
		}
	};
	
	public static Comparator<Node> descendingNodeComparator = new Comparator<Node>()
	{
		public int compare(Node node1, Node node2)
		{
			return node2.compareTo(node1);
		}
	};
}
