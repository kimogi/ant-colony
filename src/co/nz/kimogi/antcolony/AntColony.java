package co.nz.kimogi.antcolony;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class AntColony extends JFrame
{
	private static final long serialVersionUID = -3079870385792199691L;
	public static final Object lock = new Object();
	public static int count = 0;

	public static class AntPanel extends JPanel
	{
		private static final long serialVersionUID = -9027962180934835275L;
		public final static int NUM_ANTS = 100;
		public final static int LIMIT_ANTS = 20;
		public final static int WIDTH = 700;
		public final static int HEIGHT = 700;
		private final static int SZ = 2;
		
		private static LinkedList<Ant> ants = new LinkedList<Ant>();

		public AntPanel()
		{
			Random rand = new Random();
			for (int i = 0; i < NUM_ANTS; i++)
			{
				ants.addLast(new Ant(i, new Rectangle(rand.nextInt(WIDTH), rand.nextInt(HEIGHT), SZ, SZ), rand));
			}

			setPreferredSize(new Dimension(WIDTH, HEIGHT));
			setBackground(Color.WHITE);
		}

		public static void updateAnt(int id, Rectangle rect)
		{
			synchronized (lock)
			{
				Ant ant = ants.get(id);
				ant.rect = rect;
				ants.set(id, ant);
			}
		}

		public static void incrementCount()
		{
			synchronized (lock)
			{
				count++;
			}
		}

		public static int getCount()
		{
			synchronized (lock)
			{
				return count;
			}
		}

		public static ArrayList<Ant> getNearBy(Ant target, int smallRadius, int bigRadius, boolean includeLocked)
		{
			ArrayList<Ant> neighbours = new ArrayList<Ant>();
			synchronized (lock)
			{
				for (Ant ant : ants)
				{
					if (!ant.equals(target) && target.distanceTo(ant) < bigRadius && target.distanceTo(ant) > smallRadius && ant.color != Color.GRAY)
					{
						if (ant.color == Color.RED)
						{
							if (includeLocked)
							{
								neighbours.add(ant);
							}
						}
						else
						{
							neighbours.add(ant);
						}
					}
				}
			}
			return neighbours;
		}

		public static ArrayList<Ant> getNearBy(Point point, int smallRadius, int bigRadius, boolean includeLocked)
		{
			ArrayList<Ant> neighbours = new ArrayList<Ant>();
			synchronized (lock)
			{
				for (Ant ant : ants)
				{
					if (ant.distanceTo(point.x, point.y) < bigRadius && ant.distanceTo(point.x, point.y) > smallRadius && ant.color != Color.GRAY)
					{
						if (ant.color == Color.RED)
						{
							if (includeLocked)
							{
								neighbours.add(ant);
							}
						}
						else
						{
							neighbours.add(ant);
						}
					}
				}
			}
			return neighbours;
		}

		@Override
		protected void paintComponent(Graphics g)
		{
			Graphics2D g2 = (Graphics2D) g;
			g2.clearRect(0, 0, WIDTH, HEIGHT);

			Iterator<Ant> iter = ants.iterator();
			while (iter.hasNext())
			{
				Ant ant = iter.next();
				g2.setColor(ant.color);
				Rectangle rect = new Rectangle((WIDTH + ant.rect.x) % WIDTH, (HEIGHT + ant.rect.y) % HEIGHT, ant.rect.width, ant.rect.height);
				g2.fill(rect);
				
				g2.setColor(Color.BLACK);
				if (ant.first != null)
				{
					Rectangle tRect = new Rectangle((WIDTH + ant.first.rect.x) % WIDTH, (HEIGHT + ant.first.rect.y) % HEIGHT, ant.first.rect.width, ant.first.rect.height);
					g2.drawLine(rect.x, rect.y, tRect.x, tRect.y);
				}
				if (ant.second != null)
				{
					Rectangle tRect = new Rectangle((WIDTH + ant.second.rect.x) % WIDTH, (HEIGHT + ant.second.rect.y) % HEIGHT, ant.second.rect.width, ant.second.rect.height);
					g2.drawLine(rect.x, rect.y, tRect.x, tRect.y);
				}
			}
		}
	}

	private static AntPanel antPanel = new AntPanel();
	private static HashMap<Integer, Double> errors = new HashMap<Integer, Double>();
	private static double currentErrorSampleCount = 0;
	private static double currentErrorSum = 0;
	private static int currentAntCount = 1;
	
	public AntColony()
	{
		getContentPane().add(antPanel);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

	public static void updateView()
	{
		synchronized (lock)
		{
			antPanel.repaint();
			if (currentAntCount != AntPanel.getCount())
			{
				System.out.println(AntPanel.getCount() + " " + currentErrorSum / currentErrorSampleCount);
				errors.put(AntPanel.getCount(), currentErrorSum / currentErrorSampleCount);
				currentErrorSampleCount = 0;
				currentErrorSum = 0;
			}
			else
			{
				currentErrorSampleCount++;
				currentErrorSum += calculateError();
			}
			currentAntCount = AntPanel.getCount();
		}
	}

	private static double calculateError()
	{
		double antMaxDeviation = 0;

		for (Ant ant : AntPanel.ants)
		{
			if (ant.color != Color.GRAY)
			{
				ArrayList<Ant> nearAnts = AntPanel.getNearBy(ant, 0, (int) (1.5 * Ant.KEEP_RADIUS), true);
				for (Ant antLink : nearAnts)
				{
					int distance = ant.distanceTo(antLink);
					if (Math.abs(distance - Ant.KEEP_RADIUS) > antMaxDeviation)
					{
						antMaxDeviation = Math.abs(distance - Ant.KEEP_RADIUS);
					}
				}
			}
		}
		return antMaxDeviation / Ant.KEEP_RADIUS;
	}

/*	private static void checkCommonChildren()
	{
		for (Ant anti : AntPanel.ants)
		{
			for (Ant antj : AntPanel.ants)
			{
				if (anti.color != Color.GRAY && antj.color != Color.GRAY && !anti.equals(antj))
				{
					int commonChildrenCount = 0;
					for (Ant childi : anti.children)
					{
						if (antj.children.contains(childi))
						{
							commonChildrenCount++;
						}
					}
					if (commonChildrenCount > 2)
					{
						throw new IllegalStateException("Algorithmic error");
					} 
				}
			}
		}
	}
*/
	public static void createAndShowGUI()
	{
		final AntColony antWindow = new AntColony();
		antWindow.pack();
		antWindow.setLocation(600, 0);
		antWindow.setVisible(true);
	}

	public static void main(String[] args)
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				createAndShowGUI();
			}
		});
	}
}