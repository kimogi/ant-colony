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

public class Main extends JFrame
{
	private static final long serialVersionUID = -3079870385792199691L;
	public static int count = 0;

	public static class MainPanel extends JPanel
	{
		private static final long serialVersionUID = -9027962180934835275L;
		public final static int NUM_UNITS = 100;
		public final static int WIDTH = 500;
		public final static int HEIGHT = 500;
		private final static int SZ = 2;

		public static int EX_FIELD_RADIUS = (WIDTH - 20) / 2;
		public static double EX_FIELD_PREV_THETA = 0;
		public static double EX_FIELD_THETA = 0;
		public static double EX_FIELD_D_THETA = 0;//Math.PI / 90;
		public static double EX_FIELD_CENTER_X = WIDTH / 2.0;
		public static double EX_FIELD_CENTER_Y = HEIGHT / 2.0;
		public static double EX_FIELD_WIDTH = Unit.Re * 5.0;
		public static double EX_FIELD_AMP = 5.0;

		public static final Object unitsLock = new Object();
		public static final Object exFieldLock = new Object();
		public static LinkedList<Unit> units = new LinkedList<Unit>();
		private Thread exFieldThread = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				while (true)
				{
					synchronized (exFieldLock)
					{
						EX_FIELD_PREV_THETA = EX_FIELD_THETA;
						EX_FIELD_THETA += EX_FIELD_D_THETA;
					}
					try
					{
						Thread.sleep(10);
					}
					catch (InterruptedException e)
					{
						e.printStackTrace();
					}
				}
			}
		});

		public static double TEMP_K = 300;
		public static final double CONSTANT_R = 8.31;
		private static final double D_TEMP_K = 0;

		private Thread tempThread = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				while (TEMP_K > 0)
				{
					TEMP_K += D_TEMP_K;
					System.out.println("Temp : " + TEMP_K);
					
					try
					{
						Thread.sleep(200);
					}
					catch (InterruptedException e)
					{
						e.printStackTrace();
					}
				}
			}
		});

		public MainPanel()
		{
			Random rand = new Random();
			synchronized (unitsLock)
			{
				for (int i = 0; i < NUM_UNITS; i++)
				{
					units.addLast(new Unit(i, new Rectangle(rand.nextInt(WIDTH), rand.nextInt(HEIGHT), SZ, SZ), rand));
				}
			}

			setPreferredSize(new Dimension(WIDTH, HEIGHT));
			setBackground(Color.WHITE);
			exFieldThread.start();
			tempThread.start();
		}

		public static ArrayList<Unit> getAtomsCopy()
		{
			ArrayList<Unit> unitsCopy = new ArrayList<Unit>();
			synchronized (unitsLock)
			{
				unitsCopy.addAll(units);
			}
			return unitsCopy;
		}

		public static void updateUnit(int id, Rectangle rect)
		{
			synchronized (unitsLock)
			{
				Unit unit = units.get(id);
				unit.rect = rect;
				units.set(id, unit);
			}
		}

		public static DoublePoint externalCirculatingFieldVelocity(Unit unit)
		{
			double vx = 0.0, vy = 0.0, x1, x2, y1, y2;

			synchronized (exFieldLock)
			{
				x1 = EX_FIELD_CENTER_X + EX_FIELD_RADIUS * Math.cos(EX_FIELD_PREV_THETA);
				y1 = EX_FIELD_CENTER_Y + EX_FIELD_RADIUS * Math.sin(EX_FIELD_PREV_THETA);
				x2 = EX_FIELD_CENTER_X + EX_FIELD_RADIUS * Math.cos(EX_FIELD_THETA);
				y2 = EX_FIELD_CENTER_Y + EX_FIELD_RADIUS * Math.sin(EX_FIELD_THETA);
			}

			int r1 = unit.distanceTo((int) x1, (int) y1);
			int r2 = unit.distanceTo((int) x2, (int) y2);

			vx = (r1 < EX_FIELD_WIDTH && r2 < EX_FIELD_WIDTH) ? x1 - x2 : 0;
			vy = (r1 < EX_FIELD_WIDTH && r2 < EX_FIELD_WIDTH) ? y1 - y2 : 0;
			return new DoublePoint(vx, vy);
		}

		public static DoublePoint externalOscillatingFieldVelocity()
		{
			double vx = EX_FIELD_AMP * Math.sin(EX_FIELD_THETA);
			double vy = 0;
			return new DoublePoint(vx, vy);
		}

		public static void incrementCount()
		{
			synchronized (unitsLock)
			{
				count++;
			}
		}

		public static int getCount()
		{
			synchronized (unitsLock)
			{
				return count;
			}
		}

		public static ArrayList<Unit> getNearBy(Unit target, int smallRadius, int bigRadius, boolean includeLocked)
		{
			ArrayList<Unit> neighbours = new ArrayList<Unit>();
			synchronized (unitsLock)
			{
				for (Unit unit : units)
				{
					if (!unit.equals(target) && target.distanceTo(unit) < bigRadius && target.distanceTo(unit) > smallRadius && unit.color != Color.GRAY)
					{
						if (unit.color == Color.RED)
						{
							if (includeLocked)
							{
								neighbours.add(unit);
							}
						}
						else
						{
							neighbours.add(unit);
						}
					}
				}
			}
			return neighbours;
		}

		public static ArrayList<Unit> getNearBy(Point point, int smallRadius, int bigRadius, boolean includeLocked)
		{
			ArrayList<Unit> neighbours = new ArrayList<Unit>();
			synchronized (unitsLock)
			{
				for (Unit unit : units)
				{
					if (unit.distanceTo(point.x, point.y) < bigRadius && unit.distanceTo(point.x, point.y) > smallRadius && unit.color != Color.GRAY)
					{
						if (unit.color == Color.RED)
						{
							if (includeLocked)
							{
								neighbours.add(unit);
							}
						}
						else
						{
							neighbours.add(unit);
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

			Iterator<Unit> iter = MainPanel.getAtomsCopy().iterator();
			while (iter.hasNext())
			{
				Unit ant = iter.next();

				double exFieldX = 0;
				double exFieldY = 0;

				synchronized (exFieldLock)
				{
					exFieldX = EX_FIELD_CENTER_X + EX_FIELD_RADIUS * Math.cos(EX_FIELD_THETA);
					exFieldY = EX_FIELD_CENTER_Y + EX_FIELD_RADIUS * Math.sin(EX_FIELD_THETA);
				}
				g2.setColor(Color.BLUE);
				g2.fill(new Ellipse2D.Double(exFieldX - SZ, exFieldY - SZ, 2 * SZ, 2 * SZ));

				g2.setColor(ant.color);
				g2.draw(new Ellipse2D.Double(ant.rect.x - Unit.Re / 2, ant.rect.y - Unit.Re / 2, Unit.Re, Unit.Re));
				g2.fill(ant.rect);
			}
		}
	}

	private static MainPanel mainPanel = new MainPanel();
	private static double currentErrorSampleCount = 0;
	private static double currentErrorSum = 0;
	private static int currentCount = 1;
	private static double currentSpeed = MainPanel.EX_FIELD_THETA;

	public Main()
	{
		getContentPane().add(mainPanel);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

	public static void updateView()
	{
		mainPanel.repaint();
	}

	@SuppressWarnings("unused")
	private static void updateSpeedTriggeredError()
	{
		if (currentSpeed != MainPanel.EX_FIELD_D_THETA)
		{
			System.out.println(MainPanel.EX_FIELD_D_THETA + " " + currentErrorSum / currentErrorSampleCount);
			currentErrorSampleCount = 0;
			currentErrorSum = 0;
		}
		else
		{
			currentErrorSampleCount++;
			currentErrorSum += calculateMaxDeviationError();
		}
		currentSpeed = MainPanel.EX_FIELD_D_THETA;
	}

	@SuppressWarnings("unused")
	private static void updateNumTriggeredError()
	{
		if (currentCount != MainPanel.getCount())
		{
			System.out.println(MainPanel.getCount() + " " + currentErrorSum / currentErrorSampleCount);
			currentErrorSampleCount = 0;
			currentErrorSum = 0;
		}
		else
		{
			currentErrorSampleCount++;
			currentErrorSum += calculateMaxDeviationError();
		}
		currentCount = MainPanel.getCount();
	}

	private static double calculateMaxDeviationError()
	{
		double unitMaxDeviation = 0;
		for (Unit unit : MainPanel.getAtomsCopy())
		{
			ArrayList<Unit> nearAtoms = MainPanel.getNearBy(unit, 0, (int) (1.5 * Unit.Re), true);
			for (Unit atomLink : nearAtoms)
			{
				int distance = unit.distanceTo(atomLink);
				if (Math.abs(distance - Unit.Re) > unitMaxDeviation)
				{
					unitMaxDeviation = Math.abs(distance - Unit.Re);
				}
			}
		}
		return unitMaxDeviation / (double) Unit.Re;
	}

	public static void createAndShowGUI()
	{
		final Main window = new Main();
		window.pack();
		window.setLocation(600, 0);
		window.setVisible(true);
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