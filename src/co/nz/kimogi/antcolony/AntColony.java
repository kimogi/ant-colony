package co.nz.kimogi.antcolony;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class AntColony extends JFrame
{
	private static final long serialVersionUID = -3079870385792199691L;
	public static int count = 0;

	public static class AntPanel extends JPanel
	{
		private static final long serialVersionUID = -9027962180934835275L;
		public final static int NUM_ANTS = 100;
		public final static int WIDTH = 500;
		public final static int HEIGHT = 500;
		private final static int SZ = 2;

		public static final Object antsLock = new Object();
		public static LinkedList<Ant> ants = new LinkedList<Ant>();

		public AntPanel()
		{
			Random rand = new Random();
			synchronized (antsLock)
			{
				for (int i = 0; i < NUM_ANTS; i++)
				{
					ants.addLast(new Ant(i, new Rectangle(rand.nextInt(WIDTH), rand.nextInt(HEIGHT), SZ, SZ), rand));
				}
			}

			setPreferredSize(new Dimension(WIDTH, HEIGHT));
			setBackground(Color.WHITE);
		}
		
		public static ArrayList<Ant> getAntsCopy()
		{
			ArrayList<Ant> antsCopy = new ArrayList<Ant>();
			synchronized (antsLock)
			{
				antsCopy.addAll(ants);
			}
			return antsCopy;
		}
				
		public static void updateAnt(int id, Rectangle rect)
		{
			synchronized (antsLock)
			{
				Ant ant = ants.get(id);
				ant.rect = rect;
				ants.set(id, ant);
			}
		}

		public static void incrementCount()
		{
			synchronized (antsLock)
			{
				count++;
			}
		}

		public static int getCount()
		{
			synchronized (antsLock)
			{
				return count;
			}
		}

		public static ArrayList<Ant> getNearBy(Ant target, int smallRadius, int bigRadius, boolean includeLocked)
		{
			ArrayList<Ant> neighbours = new ArrayList<Ant>();
			synchronized (antsLock)
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
			synchronized (antsLock)
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

			Iterator<Ant> iter = AntPanel.getAntsCopy().iterator();
			while (iter.hasNext())
			{
				Ant ant = iter.next();
				g2.setColor(ant.color);

				g2.draw(new Ellipse2D.Double(ant.rect.x - Ant.Re/2, ant.rect.y - Ant.Re/2, Ant.Re, Ant.Re));
				g2.fill(ant.rect);
			}
		}
	}

	private static AntPanel antPanel = new AntPanel();
	private static double currentErrorSampleCount = 0;
	private static double currentErrorSum = 0;
	private static int currentAntCount = 1;
	private static double currentAntSpeed = Ant.THETA;
	private static long currentAntTime = Ant.time;
	
	public AntColony()
	{
		getContentPane().add(antPanel);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

	public static void updateView()
	{
		antPanel.repaint();
	}

	@SuppressWarnings("unused")
	private static void updateTimeTriggeredError()
	{
		if (Math.abs(currentAntTime - Ant.time) > 10)
		{
			System.out.println(String.format("%d %.4f", Ant.time, calculateMaxDeviationError()));
			currentErrorSampleCount = 0;
			currentErrorSum = 0;
			currentAntTime = Ant.time;
		}
		else
		{
			currentErrorSampleCount++;
			currentErrorSum += calculateMaxDeviationError();
		}
	}

	@SuppressWarnings("unused")
	private static void updateSpeedTriggeredError()
	{
		if (currentAntSpeed != Ant.D_THETA)
		{
			System.out.println(Ant.D_THETA + " " + currentErrorSum / currentErrorSampleCount);
			currentErrorSampleCount = 0;
			currentErrorSum = 0;
		}
		else
		{
			currentErrorSampleCount++;
			currentErrorSum += calculateMaxDeviationError();
		}
		currentAntSpeed = Ant.D_THETA;		
	}

	@SuppressWarnings("unused")
	private static void updateNumTriggeredError()
	{
		if (currentAntCount != AntPanel.getCount())
		{
			System.out.println(AntPanel.getCount() + " " + currentErrorSum / currentErrorSampleCount);
			currentErrorSampleCount = 0;
			currentErrorSum = 0;
		}
		else
		{
			currentErrorSampleCount++;
			currentErrorSum += calculateMaxDeviationError();
		}
		currentAntCount = AntPanel.getCount();		
	}
	
	private static double calculateMaxDeviationError()
	{
		double antMaxDeviation = 0;

		for (Ant ant : AntPanel.getAntsCopy())
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
		return antMaxDeviation / (double)Ant.KEEP_RADIUS;
	}

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