package co.nz.kimogi.antcolony;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Random;

import co.nz.kimogi.antcolony.Main.MainPanel;

public class Unit
{
	public static int SQUARE_SIDE = 0;
	public static final int STEP = 2;

	private static final double De = 300.0;
	private static final double SCALE = 1.0;
	public static final double Re = 30;
	private static final double RANGE_OF_INTEREST_MORSE = 2*Re;
	private static final double E_EPSILON = 1.0;
	private static final double Ke = 10.0;
	private static final double A = Math.sqrt(Ke / (2.0*De));
	private static final double DT = 1.0;
	private static final double M = 4500.0;
	private static final double Nu = 1.0;
	private static final double Sigma = Re / Math.pow(2, 0.17);
	private static final double RANGE_OF_INTEREST_LEONARD_JOHNS = Re + Sigma;

	private static final double GAUSS_1_DEVIDED_BY_SIGMA = 6;
	private static final double LINKS_COUNT = 3;
	
	public int id;
	public Rectangle rect;
	public Rectangle prevrect = null;
	public Color color;
	public Thread process = null;
	public Runnable runnable = null;
	public Random rand;

	public Unit(int id, Rectangle rectangle, Random rand)
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
					prevrect.x = rect.x;
					prevrect.y = rect.y;

					fluctuatePositionByMorse();
					MainPanel.updateUnit(Unit.this.id, Unit.this.rect);

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

	private double linkForceTo(Unit unit, double amp)
	{
		double distance = Double.valueOf(distanceTo(unit));
		DoublePoint e = new DoublePoint((unit.rect.x - rect.x) / distance, (unit.rect.y - rect.y) / distance);
		double theta = Math.atan(e.y / e.x);
		
		return amp * Math.exp(-Math.pow(GAUSS_1_DEVIDED_BY_SIGMA * Math.sin(LINKS_COUNT * theta / 2), 2));
	}
	
	private double termEk()
	{
		return 3.0 * Nu * MainPanel.CONSTANT_R * MainPanel.TEMP_K / 2.0;
	}

	private double potentialMorse(int r)
	{
		return SCALE * (-2 * De * Math.exp(-A * (r - Re)) + De * Math.exp(-2 * A * (r - Re)));
	}

	private double potentialLeonardJohns(int r)
	{
		return 4 * De * (Math.pow(Sigma / r, 12) - Math.pow(Sigma / r, 6));
	}

	private DoublePoint leonardJohnsVelocityTo(Unit unit)
	{
		int distance = distanceTo(unit);
		double vx = 0.0;
		double vy = 0.0;

		if (distance < RANGE_OF_INTEREST_LEONARD_JOHNS)
		{
			double Ep = potentialLeonardJohns(distance);
			double phi = Math.atan((unit.rect.x - rect.x) / (unit.rect.y - rect.y));
			Ep = Ep * Math.cos(phi % Math.PI/3);
			
			if (Math.abs(Ep) > E_EPSILON)
			{
				double speed = Math.signum(Ep) * Math.sqrt(2 * Math.abs(Ep) / M);
				vx = speed * (unit.rect.x - rect.x) / (double) distance;
				vy = speed * (unit.rect.y - rect.y) / (double) distance;
			}
		}		
		return new DoublePoint(vx, vy);
	}

	@SuppressWarnings("unused")
	private DoublePoint randDirection()
	{
		double theta = Math.toRadians((double) rand.nextInt(360));
		return new DoublePoint(Math.cos(theta), Math.sin(theta));
	}

	private DoublePoint eToCenterFrom(Unit unit)
	{
		double x = unit.rect.x - MainPanel.WIDTH/2;
		double y = unit.rect.y - MainPanel.HEIGHT/2;
		x = x / (double)(unit.distanceTo(MainPanel.WIDTH / 2, MainPanel.HEIGHT / 2));
		y = y / (double)(unit.distanceTo(MainPanel.WIDTH / 2, MainPanel.HEIGHT / 2));
		return new DoublePoint(x, y);
	}

	private DoublePoint termPotentialVelocity()
	{
		double Ek = termEk();
		double speed = Math.sqrt(2 * Ek / M);

		//DoublePoint e = randDirection();
		DoublePoint e = eToCenterFrom(this);
		
		System.out.println(speed);
		
		double vx = speed * e.x;
		double vy = speed * e.y;

		return new DoublePoint(vx, vy);
	}

	@SuppressWarnings("unused")
	private DoublePoint morseVelocityTo(Unit unit)
	{
		int distance = distanceTo(unit);
		double vx = 0.0;
		double vy = 0.0;

		if (distance < RANGE_OF_INTEREST_MORSE)
		{
			double Ep = potentialMorse(distance);
			if (Math.abs(Ep) > E_EPSILON)
			{
				double speed = Math.signum(Ep) * Math.sqrt(2 * Math.abs(Ep) / M);
				vx = speed * (unit.rect.x - rect.x) / (double) distance;
				vy = speed * (unit.rect.y - rect.y) / (double) distance;
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

		for (Unit ant : MainPanel.getAtomsCopy())
		{
			if (ant.id != this.id)
			{
				DoublePoint velocityToAnt = leonardJohnsVelocityTo(ant);
				vx += velocityToAnt.x;
				vy += velocityToAnt.y;
			}
		}

		DoublePoint externalFieldVelocity = MainPanel.externalOscillatingFieldVelocity();
		vx += externalFieldVelocity.x;
		vy += externalFieldVelocity.y;
		
		return new DoublePoint(vx, vy);
	}

	public void fluctuatePositionByMorse()
	{
		DoublePoint velocity = completeVelocity();

		int dx = (int) (velocity.x * DT);
		int dy = (int) (velocity.y * DT);

		move(dx, dy);
	}

	public Point getMotionGradient()
	{
		int prevX = this.prevrect.x;
		int prevY = this.prevrect.y;
		int x = this.rect.x;
		int y = this.rect.y;

		int gradX = (int)((x - prevX) * Re / 4.0);
		int gradY = (int)((y - prevY) * Re / 4.0);
		return new Point(gradX, gradY);
	}

	@SuppressWarnings("unused")
	private boolean isAlongWithMotionGradientOf(Unit target)
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
		double vectFirstZ = normVectorProductMagnitude(edgeFirstVect, antVect);
		double vectSecondZ = normVectorProductMagnitude(antVect, edgeSecondVect);

		return Math.signum(vectFirstZ) == Math.signum(vectSecondZ);
	}

	private double normVectorProductMagnitude(Point a, Point b)
	{
		double len = (Math.sqrt((double) (a.x * a.x + a.y * a.y)) * Math.sqrt((double) (b.x * b.x + b.y * b.y)));
		double vect = (double) (a.x * b.y - a.y * b.x);
		double normed = vect / len;
		return normed;
	}

	@SuppressWarnings("unused")
	private void addExternalCirculationInSquare()
	{
		int x = Unit.this.rect.x;
		int y = Unit.this.rect.y;

		if (SQUARE_SIDE == 0)
		{
			if (x < 600)
			{
				Unit.this.rect.x = x + STEP;
				Unit.this.rect.y = 200;
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
				Unit.this.rect.x = 600;
				Unit.this.rect.y = y + STEP;
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
				Unit.this.rect.x = x - STEP;
				Unit.this.rect.y = 600;
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
				Unit.this.rect.x = 200;
				Unit.this.rect.y = y - STEP;
			}
			else
			{
				SQUARE_SIDE = 0;
			}
		}
	}

	public void move(int dx, int dy)
	{
		rect.x = (MainPanel.WIDTH + rect.x - dx) % MainPanel.WIDTH;
		rect.y = (MainPanel.HEIGHT + rect.y - dy) % MainPanel.HEIGHT;
	}

	public int distanceTo(Unit unit)
	{
		return (int) Math.sqrt((rect.x - unit.rect.x) * (rect.x - unit.rect.x) + (rect.y - unit.rect.y) * (rect.y - unit.rect.y));
	}

	public int distanceTo(int x, int y)
	{
		return (int) Math.sqrt((rect.x - x) * (rect.x - x) + (rect.y - y) * (rect.y - y));
	}

	@SuppressWarnings("unused")
	private Unit getNearestUnitFrom(ArrayList<Unit> units)
	{
		Unit nearest = null;
		for (Unit unit : units)
		{
			if (nearest == null)
			{
				nearest = unit;
			}
			else if (distanceTo(unit) < distanceTo(nearest))
			{
				nearest = unit;
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
			Unit unit = (Unit) obj;
			return id == unit.id;
		}
	}

	@Override
	public int hashCode()
	{
		return id;
	}
}
