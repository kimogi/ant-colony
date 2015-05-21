package co.nz.kimogi.antcolony;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
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
		public final static int WIDTH = 400; // pixels
		public final static int HEIGHT = 400; // pixels

		private final static int SZ = 4; // ant side length, pixels

		public Ant[] ants = new Ant[NUM_ANTS];

		public AntPanel()
		{
			Random rand = new Random();
			ants[0] = new Ant(new Rectangle(rand.nextInt(WIDTH), rand.nextInt(HEIGHT), SZ, SZ), true);
			for (int i = 1; i < ants.length; i++)
			{
				ants[i] = new Ant(new Rectangle(rand.nextInt(WIDTH), rand.nextInt(HEIGHT), SZ, SZ), false);
			}

			setPreferredSize(new Dimension(WIDTH, HEIGHT));
			setBackground(Color.WHITE);
		}

		private void checkID(int id)
		{
			if (id < 0 || id >= ants.length)
				throw new IllegalArgumentException("Bad ant id: " + id);
		}

		public synchronized Point getAntLocation(int id)
		{
			checkID(id);
			Ant ant = ants[id];
			return new Point(ant.rect.x, ant.rect.y);
		}

		public synchronized void setAntLocation(int id, int x, int y)
		{
			checkID(id);
			if (x < 0)
				x = (Math.abs(x) / WIDTH + 1) * WIDTH + x;

			if (y < 0)
				y = (Math.abs(y) / HEIGHT + 1) * HEIGHT + y;

			ants[id].rect.setLocation(x % WIDTH, y % HEIGHT);
		}

		public synchronized void moveAnt(int id, int dx, int dy)
		{
			checkID(id);
			setAntLocation(id, ants[id].rect.x + dx, ants[id].rect.y + dy);
		}

		@Override
		protected void paintComponent(Graphics g)
		{
			Graphics2D g2 = (Graphics2D) g;
			g2.clearRect(0, 0, WIDTH, HEIGHT);

			for (Ant ant : ants)
			{
				if (ant.isLeader)
				{
					g2.setColor(Color.BLUE);
					g2.fill(ant.rect);
				} else if (ant.isFollower)
				{
					g2.setColor(Color.GREEN);
					g2.fill(ant.rect);
				} else
				{
					g2.setColor(Color.RED);
					g2.fill(ant.rect);
				}
			}
		}
	}

	private AntPanel antPanel = new AntPanel();
	boolean keepRunning = true;
	private Random rand = new Random();

	public AntColony()
	{
		getContentPane().add(antPanel);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

	private double theta = -Math.PI;
	private Point center = new Point(AntPanel.WIDTH / 2, AntPanel.HEIGHT / 2);
	private int radius = 100;
	private int hookUpRadius = 20;

	public void updateLoop()
	{
		while (keepRunning)
		{
			for (int i = 0; i < AntPanel.NUM_ANTS; i++)
			{
				Ant ant = antPanel.ants[i];
				Ant leader = antPanel.ants[0];
				antPanel.moveAnt(i, rand.nextInt(5) - 2, rand.nextInt(5) - 2);

				if (ant.isLeader)
				{
					theta = theta + Math.PI / 36;
					if (theta > Math.PI)
					{
						theta = -Math.PI;
					}
					ant.rect.x = center.x + (int) (radius * Math.cos(theta));
					ant.rect.y = center.y + (int) (radius * Math.sin(theta));
				} 
				else
				{
					if (Math.abs(ant.rect.x - leader.rect.x) <= hookUpRadius && Math.abs(ant.rect.y - leader.rect.y) <= hookUpRadius)
					{
						antPanel.ants[i].isFollower = true;
					}

					if (ant.isFollower)
					{
						// TODO: follow
					}
				}
			}
			antPanel.repaint();

			try
			{
				Thread.sleep(50);

			} catch (Exception ex)
			{
				ex.printStackTrace();
			}
		}
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
		antWindow.setLocationByPlatform(true);
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