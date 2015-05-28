package co.nz.kimogi.antcolony;

import java.util.ArrayList;
import java.util.Iterator;

import co.nz.kimogi.antcolony.AntColony.AntPanel;

public class NodeNetwork
{
	public ArrayList<Node> nodes;
	public AntPanel antPanel;
	private static final int MAX_CHILDREN = 6;

	public NodeNetwork(AntPanel panel)
	{
		this.antPanel = panel;
		nodes = new ArrayList<Node>();
	}

	public boolean contains(Ant ant)
	{
		for (Node node : nodes)
		{
			if (node.ant.equals(ant))
			{
				return true;
			}
		}
		return false;
	}

	public boolean tryHookUpWithRange(Ant ant, int range, int separation)
	{
		if (nodes.size() > 15)
		{
			return false;
		}
		if (nodes.size() == 1)
		{
			Node nearest = nodes.get(0);
			int distance = antPanel.calcDistance(ant, nearest.ant);
			if (distance > range || distance < separation)
			{
				return false;
			}
			else
			{
				Node newNode = new Node(ant);
				newNode.first = nearest;
				nearest.childen.add(newNode);
				nodes.add(newNode);
				return true;
			}
		}
		else if (nodes.size() == 2)
		{
			Node nearest1 = nodes.get(0);
			Node nearest2 = nodes.get(1);
			
			int distance1 = antPanel.calcDistance(ant, nearest1.ant);
			int distance2 = antPanel.calcDistance(ant, nearest2.ant);
			
			if (distance1 < range && distance2 < range)
			{
				Node newNode = new Node(ant);
				newNode.first = nearest1;
				newNode.second = nearest2;
				if (nearest1.second == null)
				{
					nearest1.second = newNode;
				}
				if (nearest2.second == null)
				{
					nearest2.second = newNode;
				}
				nearest1.childen.add(newNode);
				nearest2.childen.add(newNode);

				if (nearest1.childen.size() == MAX_CHILDREN)
				{
					nearest1.ant.isBlocked = true;
				}
				if (nearest2.childen.size() == MAX_CHILDREN)
				{
					nearest2.ant.isBlocked = true;
				}
				
				nodes.add(newNode);
				return true;
			}
			else
			{
				return false;
			}
		}
		else
		{
			Node nearest1 = null;
			
			Iterator<Node> iter = nodes.iterator();
			while (iter.hasNext())
			{
				Node node = iter.next();
				if (!node.ant.isBlocked)
				{
					if (nearest1 == null)
					{
						nearest1 = node;
					}
					else
					{
						if (antPanel.calcDistance(node.ant, ant) < antPanel.calcDistance(nearest1.ant, ant))
						{
							nearest1 = node;
						}
					}
				}
			}

			if (nearest1 == null)
			{
				return false;
			}
			
			Node nearest2 = null;
			
			iter = nodes.iterator();
			while (iter.hasNext())
			{
				Node node = iter.next();
				
				if (!node.ant.isBlocked && node.ant.id != nearest1.ant.id)
				{
					if (nearest2 == null)
					{
						nearest2 = node;
					}
					else
					{
						if (antPanel.calcDistance(node.ant, ant) < antPanel.calcDistance(nearest2.ant, ant))
						{
							nearest2 = node;
						}
					}
				}
			}
			
			if (nearest2 == null)
			{
				return false;
			}
			
			int distance1 = antPanel.calcDistance(ant, nearest1.ant);
			int distance2 = antPanel.calcDistance(ant, nearest2.ant);

			if (distance1 < range && distance1 > separation && distance2 < range && distance2 > separation)
			{
				Node newNode = new Node(ant);
				newNode.first = nearest1;
				newNode.second = nearest2;
				if (nearest1.second == null)
				{
					nearest1.second = newNode;
				}
				if (nearest2.second == null)
				{
					nearest2.second = newNode;
				}
				nearest1.childen.add(newNode);
				nearest2.childen.add(newNode);

				if (nearest1.childen.size() == MAX_CHILDREN)
				{
					nearest1.ant.isBlocked = true;
				}
				if (nearest2.childen.size() == MAX_CHILDREN)
				{
					nearest2.ant.isBlocked = true;
				}

				nodes.add(newNode);
				return true;
			}
			else
			{
				return false;
			}
		}
	}

	public void tryKeepForamtion(int separation)
	{
		Iterator<Node> iter = nodes.iterator();
		while(iter.hasNext())
		{
			Node node = iter.next();
			adjustPosition(node, separation);
		}
	}
	
	private void adjustPosition(Node node, int separation)
	{
		if (node.first != null)
		{
			if (!node.ant.isLeader)
				antPanel.moveAntTowards(node.ant, node.first.ant, separation);
			antPanel.separateAntsBy(node.ant, node.first.ant, separation);
		}
		if (node.second != null)
		{
			if (!node.ant.isLeader)
				antPanel.moveAntTowards(node.ant, node.second.ant, separation);
			antPanel.separateAntsBy(node.ant, node.second.ant, separation);
		}
		for (Node child : node.childen)
		{
			antPanel.separateAntsBy(child.ant, node.ant, separation);
			for (Node childchild : node.childen)
			{
				if (!childchild.equals(child))
				{
					antPanel.separateAntsBy(childchild.ant, child.ant, separation);
				}
			}
		}
	}
}
