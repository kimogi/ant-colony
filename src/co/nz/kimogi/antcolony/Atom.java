package co.nz.kimogi.antcolony;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Random;

import co.nz.kimogi.antcolony.Main.AtomPanel;

public class Atom
{
	public static int SQUARE_SIDE = 0;
	public static final int STEP = 2;

	private static final double De = 100.0;
	private static final double SCALE = 1.0;
	public static final double Re = 30.0;
	private static final double RANGE_OF_INTEREST_MORSE = 2*Re;
	private static final double E_EPSILON = 1.0;
	private static final double Ke = 10.0;
	private static final double A = Math.sqrt(Ke / (2.0*De));
	private static final double DT = 1.0;
	private static final double M = 500.0;
	private static final double Nu = 0.1;
	private static final double Sigma = Re / Math.pow(2, 0.17);
	private static final double RANGE_OF_INTEREST_LEONARD_JHONES = Re + Sigma;

	private static double TEMP_K = 500;
	private static final double D_TEMP_K =  0.0;
	private static final double CONSTANT_R = 8.31;

	public int id;
	public Rectangle rect;
	public Rectangle prevrect = null;
	public Color color;
	public Thread process = null;
	public Runnable runnable = null;
	public Random rand;

	public static long time = 0;

	public Atom(int id, Rectangle rectangle, Random rand)
	{
		this.id = id;
		this.rect = rectangle;
		this.prevrect = (Rectangle) rect.clone();
		this.rand = rand;
		this.color = Color.RED;

		this.runnable = new Runnable()
		{
			@Override
			public void run()
			{
				while (true)
				{
					TEMP_K += D_TEMP_K;
				//	System.out.println("Temp : " + TEMP_K);
					
					prevrect.x = rect.x;
					prevrect.y = rect.y;

					fluctuatePositionByMorse();
					AtomPanel.updateAnt(Atom.this.id, Atom.this.rect);

					try
					{
						Thread.sleep(30);
					}
					catch (InterruptedException e)
					{
						e.printStackTrace();
					}
					Main.updateView();
				}
			}
		};

		this.process = new Thread(runnable);
		this.process.start();
	}

	private double termPotential()
	{
		return 3.0 * Nu * CONSTANT_R * TEMP_K / 2.0;
	}

	private double potentialMorse(int r)
	{
		return SCALE * (-2 * De * Math.exp(-A * (r - Re)) + De * Math.exp(-2 * A * (r - Re)));
	}

	private double potentialLeonardJhones(int r)
	{
		return 4 * De * (Math.pow(Sigma / r, 12) - Math.pow(Sigma / r, 6));
	}

	private DoublePoint leonardJhonesVelocityTo(Atom ant)
	{
		int distance = distanceTo(ant);
		double vx = 0.0;
		double vy = 0.0;

		if (distance < RANGE_OF_INTEREST_LEONARD_JHONES)
		{
			double Ep = potentialLeonardJhones(distance);
			if (Math.abs(Ep) > E_EPSILON)
			{
				double speed = Math.signum(Ep) * Math.sqrt(2 * Math.abs(Ep) / M);
				vx = speed * (ant.rect.x - rect.x) / (double) distance;
				vy = speed * (ant.rect.y - rect.y) / (double) distance;
			}
		}		
		return new DoublePoint(vx, vy);
	}

	private DoublePoint randDirection()
	{
		double theta = Math.toRadians((double) rand.nextInt(360));
		return new DoublePoint(Math.cos(theta), Math.sin(theta));
	}

	private DoublePoint termPotentialVelocity()
	{
		double U = termPotential();
		double speed = Math.sqrt(2 * U / M);

		DoublePoint e = randDirection();

		double vx = speed * e.x;
		double vy = speed * e.y;

		return new DoublePoint(vx, vy);
	}

	private DoublePoint morseVelocityTo(Atom ant)
	{
		int distance = distanceTo(ant);
		double vx = 0.0;
		double vy = 0.0;

		if (distance < RANGE_OF_INTEREST_MORSE)
		{
			double Ep = potentialMorse(distance);
			if (Math.abs(Ep) > E_EPSILON)
			{
				double speed = Math.signum(Ep) * Math.sqrt(2 * Math.abs(Ep) / M);
				vx = speed * (ant.rect.x - rect.x) / (double) distance;
				vy = speed * (ant.rect.y - rect.y) / (double) distance;
			}
		}		
		return new DoublePoint(vx, vy);
	}

	private DoublePoint completeVelocity()
	{
		double vx = 0.0;
		double vy = 0.0;

		DoublePoint termV = termPotentialVelocity();
		vx = termV.x;
		vy = termV.y;

		for (Atom ant : AtomPanel.getAtomsCopy())
		{
			if (ant.id != this.id)
			{
			//	DoublePoint velocityToAnt = morseVelocityTo(ant);
				DoublePoint velocityToAnt = leonardJhonesVelocityTo(ant);
				vx += velocityToAnt.x;
				vy += velocityToAnt.y;
			}
		}

/*//		DoublePoint externalFieldVelocity = AtomPanel.externalCirculatingFieldVelocity(this);
		DoublePoint externalFieldVelocity = AtomPanel.externalOscillatingFieldVelocity();
		vx += externalFieldVelocity.x;
		vy += externalFieldVelocity.y;
*/		
		return new DoublePoint(vx, vy);
	}

	public void fluctuatePositionByMorse()
	{
		DoublePoint velocity = completeVelocity();

		int dx = (int) (velocity.x * DT);
		int dy = (int) (velocity.y * DT);

		move(dx, dy);
	}

	public Point getDGrad()
	{
		int prevX = this.prevrect.x;
		int prevY = this.prevrect.y;
		int x = this.rect.x;
		int y = this.rect.y;

		int dGradX = (int)((x - prevX) * Re / 4.0);
		int dGradY = (int)((y - prevY) * Re / 4.0);
		return new Point(dGradX, dGradY);
	}

	@SuppressWarnings("unused")
	private boolean isAlongWithGradOf(Atom target)
	{
		int prevX = target.prevrect.x;
		int prevY = target.prevrect.y;
		int x = target.rect.x;
		int y = target.rect.y;

		int gradX = (int)((x - prevX) * Re);
		int gradY = (int)((y - prevY) * Re);

		int gradPerFirstX = (int) (Math.sin(Math.PI / 3) * gradY);
		int gradPerFirstY = (int) (Math.sin(Math.PI / 3) * gradX);
		int gradPerSecondX = (int) (Math.sin(Math.PI / 3) * (-gradY));
		int gradPerSecondY = (int) (Math.sin(Math.PI / 3) * (-gradX));

		int edgeFirstX = gradX + gradPerFirstX;
		int edgeFirstY = gradY + gradPerFirstY;
		int edgeSecondX = gradX + gradPerSecondX;
		int edgeSecondY = gradY + gradPerSecondY;

		Point edgeFirstVect = new Point(edgeFirstX, edgeFirstY);
		Point edgeSecondVect = new Point(edgeSecondX, edgeSecondY);

		Point antVect = new Point(this.rect.x, this.rect.y);
		double vectFirstZ = normVectorProductZ(edgeFirstVect, antVect);
		double vectSecondZ = normVectorProductZ(antVect, edgeSecondVect);

		return Math.signum(vectFirstZ) == Math.signum(vectSecondZ);
	}

	private double normVectorProductZ(Point a, Point b)
	{
		double len = (Math.sqrt((double) (a.x * a.x + a.y * a.y)) * Math.sqrt((double) (b.x * b.x + b.y * b.y)));
		double vect = (double) (a.x * b.y - a.y * b.x);
		double normed = vect / len;
		return normed;
	}

	@SuppressWarnings("unused")
	private void addExternalCirculationInSquare()
	{
		int x = Atom.this.rect.x;
		int y = Atom.this.rect.y;

		if (SQUARE_SIDE == 0)
		{
			if (x < 600)
			{
				Atom.this.rect.x = x + STEP;
				Atom.this.rect.y = 200;
			}
			else
			{
				SQUARE_SIDE = 1;
			}
		}
		else if (SQUARE_SIDE == 1)
		{
			if (y < 600)
			{
				Atom.this.rect.x = 600;
				Atom.this.rect.y = y + STEP;
			}
			else
			{
				SQUARE_SIDE = 2;
			}
		}
		else if (SQUARE_SIDE == 2)
		{
			if (x > 200)
			{
				Atom.this.rect.x = x - STEP;
				Atom.this.rect.y = 600;
			}
			else
			{
				SQUARE_SIDE = 3;
			}
		}
		else if (SQUARE_SIDE == 3)
		{
			if (y > 200)
			{
				Atom.this.rect.x = 200;
				Atom.this.rect.y = y - STEP;
			}
			else
			{
				SQUARE_SIDE = 0;
			}
		}
	}

	public void move(int dx, int dy)
	{
		rect.x = (rect.x - dx) % AtomPanel.WIDTH; 
		rect.y = (rect.y - dy) % AtomPanel.HEIGHT;
	}

	public int distanceTo(Atom atom)
	{
		return (int) Math.sqrt((rect.x - atom.rect.x) * (rect.x - atom.rect.x) + (rect.y - atom.rect.y) * (rect.y - atom.rect.y));
	}

	public int distanceTo(int x, int y)
	{
		return (int) Math.sqrt((rect.x - x) * (rect.x - x) + (rect.y - y) * (rect.y - y));
	}

	@SuppressWarnings("unused")
	private Atom getNearestAnt(ArrayList<Atom> atoms)
	{
		Atom nearest = null;
		for (Atom atom : atoms)
		{
			if (nearest == null)
			{
				nearest = atom;
			}
			else if (distanceTo(atom) < distanceTo(nearest))
			{
				nearest = atom;
			}
		}
		return nearest;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj == null)
		{
			return false;
		}
		else
		{
			Atom atom = (Atom) obj;
			return id == atom.id;
		}
	}

	@Override
	public int hashCode()
	{
		return id;
	}
}
