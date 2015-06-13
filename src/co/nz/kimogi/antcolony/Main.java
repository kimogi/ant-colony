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

	public static class AtomPanel extends JPanel
	{
		private static final long serialVersionUID = -9027962180934835275L;
		public final static int NUM_ATOMS = 100;
		public final static int WIDTH = 500;
		public final static int HEIGHT = 500;
		private final static int SZ = 2;

		public static final Object atomsLock = new Object();
		public static LinkedList<Atom> atoms = new LinkedList<Atom>();

		public AtomPanel()
		{
			Random rand = new Random();
			synchronized (atomsLock)
			{
				for (int i = 0; i < NUM_ATOMS; i++)
				{
					atoms.addLast(new Atom(i, new Rectangle(rand.nextInt(WIDTH), rand.nextInt(HEIGHT), SZ, SZ), rand));
				}
			}

			setPreferredSize(new Dimension(WIDTH, HEIGHT));
			setBackground(Color.WHITE);
		}

		public static ArrayList<Atom> getAtomsCopy()
		{
			ArrayList<Atom> atomsCopy = new ArrayList<Atom>();
			synchronized (atomsLock)
			{
				atomsCopy.addAll(atoms);
			}
			return atomsCopy;
		}

		public static void updateAnt(int id, Rectangle rect)
		{
			synchronized (atomsLock)
			{
				Atom atom = atoms.get(id);
				atom.rect = rect;
				atoms.set(id, atom);
			}
		}

		public static void incrementCount()
		{
			synchronized (atomsLock)
			{
				count++;
			}
		}

		public static int getCount()
		{
			synchronized (atomsLock)
			{
				return count;
			}
		}

		public static ArrayList<Atom> getNearBy(Atom target, int smallRadius, int bigRadius, boolean includeLocked)
		{
			ArrayList<Atom> neighbours = new ArrayList<Atom>();
			synchronized (atomsLock)
			{
				for (Atom atom : atoms)
				{
					if (!atom.equals(target) && target.distanceTo(atom) < bigRadius && target.distanceTo(atom) > smallRadius && atom.color != Color.GRAY)
					{
						if (atom.color == Color.RED)
						{
							if (includeLocked)
							{
								neighbours.add(atom);
							}
						}
						else
						{
							neighbours.add(atom);
						}
					}
				}
			}
			return neighbours;
		}

		public static ArrayList<Atom> getNearBy(Point point, int smallRadius, int bigRadius, boolean includeLocked)
		{
			ArrayList<Atom> neighbours = new ArrayList<Atom>();
			synchronized (atomsLock)
			{
				for (Atom atom : atoms)
				{
					if (atom.distanceTo(point.x, point.y) < bigRadius && atom.distanceTo(point.x, point.y) > smallRadius && atom.color != Color.GRAY)
					{
						if (atom.color == Color.RED)
						{
							if (includeLocked)
							{
								neighbours.add(atom);
							}
						}
						else
						{
							neighbours.add(atom);
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

			Iterator<Atom> iter = AtomPanel.getAtomsCopy().iterator();
			while (iter.hasNext())
			{
				Atom ant = iter.next();
				g2.setColor(ant.color);

				g2.draw(new Ellipse2D.Double(ant.rect.x - Atom.Re / 2, ant.rect.y - Atom.Re / 2, Atom.Re, Atom.Re));
				g2.fill(ant.rect);
			}
		}
	}

	private static AtomPanel atomPanel = new AtomPanel();
	private static double currentErrorSampleCount = 0;
	private static double currentErrorSum = 0;
	private static int currentCount = 1;
	private static double currentSpeed = Atom.THETA;
	private static long currentTime = Atom.time;

	public Main()
	{
		getContentPane().add(atomPanel);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

	public static void updateView()
	{
		atomPanel.repaint();
	}

	@SuppressWarnings("unused")
	private static void updateTimeTriggeredError()
	{
		if (Math.abs(currentTime - Atom.time) > 10)
		{
			System.out.println(String.format("%d %.4f", Atom.time, calculateMaxDeviationError()));
			currentErrorSampleCount = 0;
			currentErrorSum = 0;
			currentTime = Atom.time;
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
		if (currentSpeed != Atom.D_THETA)
		{
			System.out.println(Atom.D_THETA + " " + currentErrorSum / currentErrorSampleCount);
			currentErrorSampleCount = 0;
			currentErrorSum = 0;
		}
		else
		{
			currentErrorSampleCount++;
			currentErrorSum += calculateMaxDeviationError();
		}
		currentSpeed = Atom.D_THETA;
	}

	@SuppressWarnings("unused")
	private static void updateNumTriggeredError()
	{
		if (currentCount != AtomPanel.getCount())
		{
			System.out.println(AtomPanel.getCount() + " " + currentErrorSum / currentErrorSampleCount);
			currentErrorSampleCount = 0;
			currentErrorSum = 0;
		}
		else
		{
			currentErrorSampleCount++;
			currentErrorSum += calculateMaxDeviationError();
		}
		currentCount = AtomPanel.getCount();
	}

	private static double calculateMaxDeviationError()
	{
		double atomMaxDeviation = 0;
		for (Atom atom : AtomPanel.getAtomsCopy())
		{
			ArrayList<Atom> nearAtoms = AtomPanel.getNearBy(atom, 0, (int) (1.5 * Atom.Re), true);
			for (Atom atomLink : nearAtoms)
			{
				int distance = atom.distanceTo(atomLink);
				if (Math.abs(distance - Atom.Re) > atomMaxDeviation)
				{
					atomMaxDeviation = Math.abs(distance - Atom.Re);
				}
			}
		}
		return atomMaxDeviation / (double) Atom.Re;
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