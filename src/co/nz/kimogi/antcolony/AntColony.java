package co.nz.kimogi.antcolony;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class AntColony extends JFrame
{
	private static final long serialVersionUID = -3079870385792199691L;

	class AntPanel extends JPanel
	{
		private static final long serialVersionUID = -9027962180934835275L;
		public final static int NUM_ANTS = 100;
		public final static int WIDTH = 500;
		public final static int HEIGHT = 500;

		private final static int SZ = 3;

		public LinkedList<Ant> ants = new LinkedList<Ant>();

		public AntPanel()
		{
			Random rand = new Random();
			ants.addLast(new Ant(0, new Rectangle(rand.nextInt(WIDTH), rand.nextInt(HEIGHT), SZ, SZ), true));
			for (int i = 1; i < NUM_ANTS; i++)
			{
				ants.addLast(new Ant(i, new Rectangle(rand.nextInt(WIDTH), rand.nextInt(HEIGHT), SZ, SZ), false));
			}
			
			setPreferredSize(new Dimension(WIDTH, HEIGHT));
			setBackground(Color.WHITE);
		}

		private void checkID(int id)
		{
			if (id < 0 || id >= NUM_ANTS)
			{
				throw new IllegalArgumentException("Bad ant id: " + id);
			}
		}

		public synchronized void setAntLocation(Ant ant, int x, int y)
		{
			checkID(ant.id);
			ant.rect.setLocation(x, y);
		}

		public synchronized void moveAnt(Ant ant, int dx, int dy)
		{
			checkID(ant.id);
			setAntLocation(ant, ant.rect.x + dx, ant.rect.y + dy);
		}

		public synchronized boolean isVisible(Ant visor, Ant target, int range)
		{
			return calcDistance(visor, target) < range;
		}
		
		public int calcDistance(Ant ant1, Ant ant2)
		{
			return (int) Math.sqrt((ant1.rect.x - ant2.rect.x) * (ant1.rect.x - ant2.rect.x) + (ant1.rect.y - ant2.rect.y) * (ant1.rect.y - ant2.rect.y));
		}
		
		public synchronized void moveAntTowards(Ant ant, Ant target, int space)
		{
			checkID(ant.id);
			checkID(target.id);
			
			int x2 = ant.rect.x;
			int y2 = ant.rect.y;
			int x1 = target.rect.x;
			int y1 = target.rect.y;
			int distance = calcDistance(ant, target);
		
			if (distance != 0 && distance > space)
			{
				int dx = (int) (x1 - x2) * (distance - space)/distance;
				int dy = (int) (y1 - y2) * (distance - space)/distance;
				antPanel.moveAnt(ant, dx, dy);
			}
		}

		public synchronized void separateAntsBy(Ant ant, Ant target, int space)
		{
			checkID(ant.id);
			checkID(target.id);
			
			int x2 = ant.rect.x;
			int y2 = ant.rect.y;
			int x1 = target.rect.x;
			int y1 = target.rect.y;
			int distance = calcDistance(ant, target);
		
			int dx = 0;
			int dy = 0;
			
			if (distance != 0 && distance < space)
			{
				dx = (int) (x1 - x2) * (distance - space) / distance;
				dy = (int) (y1 - y2) * (distance - space) / distance;
			} 
			else if (distance == 0)
			{
				dx = (int)(space * Math.sin(Math.PI/6));
				dy = (int)(space * Math.cos(Math.PI/6));
			}
			antPanel.moveAnt(ant, dx, dy);
		}
		
		public void fluctuateAntPosition(Ant ant, Random rand)
		{
			int dx = rand.nextInt(5) - 2;
			int dy = rand.nextInt(5) - 2;
			moveAnt(ant, dx, dy);
		}
		
		@Override
		protected void paintComponent(Graphics g)
		{
			Graphics2D g2 = (Graphics2D) g;
			g2.clearRect(0, 0, WIDTH, HEIGHT);

			//long millis = System.currentTimeMillis();
			
			Iterator<Ant> iter = ants.iterator();
			while (iter.hasNext())
			{
				Ant ant = iter.next();
				g2.setColor(ant.color);
				g2.fill(new Rectangle((WIDTH + ant.rect.x) % WIDTH, (HEIGHT + ant.rect.y) % HEIGHT, ant.rect.width, ant.rect.height));
				
			//	if (followers.contains(ant))
			//	{
			//		System.out.println(millis + " : " + ant.id + " : x : " +  ant.rect.x + " y : " + ant.rect.y);
			//	}
			}
		}
	}

	private AntPanel antPanel = new AntPanel();
	boolean keepRunning = true;
	private Random rand = new Random();
	private NodeNetwork followers = new NodeNetwork(antPanel);

	public AntColony()
	{
		getContentPane().add(antPanel);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		followers.nodes.add(new Node(antPanel.ants.getFirst()));
	}

	private double theta = -Math.PI;
	private Point center = new Point(AntPanel.WIDTH / 2, AntPanel.HEIGHT / 2);
	private int radius = 100;
	private int hookUpRadius = 30;
	private int keepRadius = 20;

	float i = 0;
	public void updateLoop()
	{
		while (keepRunning)
		{
			Ant leader = antPanel.ants.getFirst();
			theta = theta + Math.PI / 90;
			if (theta > Math.PI)
			{
				theta = -Math.PI;
			}
			leader.rect.x = center.x + (int) (radius * Math.cos(theta));
			leader.rect.y = center.y + (int) (radius * Math.sin(theta));

			//System.out.println("Center : " + center.x + " " + center.y);
			
			Iterator<Ant> iter = antPanel.ants.iterator();
			while (iter.hasNext())
			{
				Ant ant = iter.next();

				if (!followers.contains(ant))
				{
					followers.tryHookUpWithRange(ant, hookUpRadius, keepRadius);
				}
			}
			
			iter = antPanel.ants.iterator();
			while (iter.hasNext())
			{
				Ant ant = iter.next();
				
				if (!followers.contains(ant))
				{
					antPanel.fluctuateAntPosition(ant, rand);
					ant.color = Color.GRAY;
				}
				else
				{
					if (ant.isBlocked)
					{
						ant.color = Color.CYAN;
					}
					else
					{
						ant.color = Color.GREEN;
					}
				}
			}
			leader.color = Color.BLUE;

			followers.tryKeepForamtion(keepRadius);
			antPanel.repaint();

			try
			{
				Thread.sleep(50);
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
			
			double allError = calculateError();
			System.out.println(followers.nodes.size() + " " + allError);
		}
	}
	
	private double calculateError()
	{
		double antOverallError = 0;
		long antOverallCount = 0;

		for (Node nodeI : followers.nodes)
		{
			for (Node nodeJ : followers.nodes)
			{
				if (nodeI.isNeighbour(nodeJ))
				{
					double distance = antPanel.calcDistance(nodeI.ant, nodeJ.ant);
					antOverallError += Math.sqrt(Math.abs(distance*distance - keepRadius*keepRadius));
					antOverallCount++;
				}
			}
		}
		antOverallError = antOverallError / antOverallCount;
		return antOverallError / keepRadius;
	}
	
	public void startAnimation()
	{
		Thread animationThread = new Thread(new Runnable()
		{
			public void run()
			{
				updateLoop();
			}
		});
		animationThread.start();
	}

	public static void createAndShowGUI()
	{
		final AntColony antWindow = new AntColony();
		antWindow.pack();
		antWindow.setLocation(600, 100);
		antWindow.setVisible(true);
		antWindow.startAnimation();
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