package co.nz.kimogi.antcolony;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Random;

import co.nz.kimogi.antcolony.AntColony.AntPanel;

public class Ant
{
	public final static int HOOK_UP_RADIUS = 70;
	public final static int KEEP_RADIUS = 50;
	public static final int MAX_NEIGHBOUR_COUNT = 6;
		
	public int id;
	public Rectangle rect;
	public Color color;
	public Thread process = null;
	public Runnable runnable = null;
	public Random rand;
	
	public Ant first = null;
	public Ant second = null;
	public ArrayList<Ant> children;
	public static double deltaTheta = Math.PI/180;
	public static int radius = 250;
	public static double theta = 0;

	public Ant(int id, Rectangle rect, Random rand)
	{
		this.id = id;
		this.rect = rect;
		this.rand = rand;
		this.children = new ArrayList<Ant>();
		this.color = id == 0 ? Color.BLUE : Color.GRAY;

		this.runnable = new Runnable()
		{
			@Override
			public void run()
			{
				theta = -Math.PI;
				Point center = new Point(AntPanel.WIDTH / 2, AntPanel.HEIGHT / 2);

				while (true)
				{
					if (Ant.this.id == 0)
					{
						theta = theta + deltaTheta;
						if (theta > Math.PI)
						{
							theta = -Math.PI;
							
/*							if (AntPanel.getCount() == AntPanel.LIMIT_ANTS)
							{
								deltaTheta = deltaTheta +  Math.PI/3000;
							}
*/						}

						Ant.this.rect.x = center.x + (int) (Math.abs(radius * Math.cos(theta)));
						Ant.this.rect.y = center.y + (int) (Math.abs(radius * Math.sin(theta)));
						color = children.size() < MAX_NEIGHBOUR_COUNT ? Color.BLUE : Color.RED;
					}
					else if (color == Color.GRAY)
					{
						if (AntPanel.getCount() < AntPanel.LIMIT_ANTS)
						{
							if (tryHookUp())
							{
								adjustPosition(true);
								AntPanel.incrementCount();
							}
							else
							{
								fluctuatePositionWithAwareness();
							}
						}
						else
						{
							fluctuatePositionWithAwareness();
						}
					}
					else if (color == Color.GREEN || color == Color.RED)
					{
						adjustPosition(false);
					}

					AntPanel.updateAnt(Ant.this.id, Ant.this.rect);

					try
					{
						Thread.sleep(30);
					}
					catch (InterruptedException e)
					{
						e.printStackTrace();
					}
					AntColony.updateView();
				}
			}
		};

		this.process = new Thread(runnable);
		this.process.start();
	}

	private void adjustPosition(boolean checkCollision)
	{
		if (first != null && second != null)
		{
			int distance1 = distanceTo(first);
			int distance2 = distanceTo(second);

			if (distance1 == distance2 && distance1 == KEEP_RADIUS)
			{
				return;
			}

			int tx1 = 0, tx2 = 0, ty1 = 0, ty2 = 0, mx = 0, my = 0;
			int parDx = 0, parDy = 0, perDx = 0, perDy = 0;

			tx1 = first.rect.x;
			tx2 = second.rect.x;
			ty1 = first.rect.y;
			ty2 = second.rect.y;

			parDx = (tx1 - tx2);
			parDy = (ty1 - ty2);

			mx = tx2 + (int) (0.5 * parDx);
			my = ty2 + (int) (0.5 * parDy);

			double targetDistance = KEEP_RADIUS;
			double distance = first.distanceTo(second);
			double scale = targetDistance / distance;

			perDx = (int) (Math.sin(Math.PI / 3) * scale * parDy);
			perDy = (int) (Math.sin(Math.PI / 3) * scale * (-parDx));

			int tx = 0;
			int ty = 0;
			
			if (distanceTo(mx - perDx, my - perDy) < distanceTo(mx + perDx, my + perDy))
			{
				tx = mx - perDx;
				ty = my - perDy;
			}
			else
			{
				tx = mx + perDx;
				ty = my + perDy;
			}

			if (checkCollision)
			{
				ArrayList<Ant> neighbours = AntPanel.getNearBy(new Point(tx, ty), 0, HOOK_UP_RADIUS, true);
				for (Ant neighbour : neighbours)
				{
					if (!neighbour.equals(this) && neighbour.distanceTo(tx, ty) < (int)(0.1 * KEEP_RADIUS))
					{
						tx = -tx;
						ty = -ty;
						break;
					}
				}
			}
			move(tx - rect.x, ty - rect.y);
		}
		else if (first != null && second == null)
		{
			int distance1 = distanceTo(first);

			int tx1 = first.rect.x;
			int ty1 = first.rect.y;
			int tx = rect.x;
			int ty = rect.y;

			int toT1x = (int) ((double) (distance1 - KEEP_RADIUS) / (double) distance1 * (tx1 - tx));
			int toT1y = (int) ((double) (distance1 - KEEP_RADIUS) / (double) distance1 * (ty1 - ty));

			move(toT1x, toT1y);
		}
	}

	private boolean tryHookUp()
	{
		ArrayList<Ant> nearAnts = AntPanel.getNearBy(this, KEEP_RADIUS, HOOK_UP_RADIUS, false);

		if (nearAnts.size() == 1 && nearAnts.get(0).color == Color.BLUE && nearAnts.get(0).children.size() == 0)
		{
			Ant target = nearAnts.get(0);
			first = target;
			color = Color.GREEN;
			target.children.add(this);
			return true;
		}
		else
		{
			ArrayList<Pair<Ant, Ant>> samples = new ArrayList<Pair<Ant, Ant>>();
			for (Ant target1 : nearAnts)
			{
				for (Ant target2 : nearAnts)
				{
					Pair<Ant, Ant> pair = new Pair<Ant, Ant>(target1, target2);
					if (!target1.equals(target2) && target1.distanceTo(target2) == KEEP_RADIUS && distanceTo(pair) >= KEEP_RADIUS)
					{
						samples.add(pair);
					}
				}
			}
			if (samples.isEmpty())
			{
				return false;
			}

			Pair<Ant, Ant> nearest = getNearestPair(samples);
			if (hasCommonChild(nearest))
			{
				return false;
			}

			Ant target1 = nearest.first;
			Ant target2 = nearest.second;

			first = target1;
			second = target2;
			color = Color.GREEN;
			target1.children.add(this);
			target2.children.add(this);

			if (target1.children.size() == MAX_NEIGHBOUR_COUNT - (target1.first != null ? 1 : 0) - (target1.second != null ? 1 : 0))
			{
				target1.color = Color.RED;
			}
			if (target2.children.size() == MAX_NEIGHBOUR_COUNT - (target2.first != null ? 1 : 0) - (target2.second != null ? 1 : 0))
			{
				target2.color = Color.RED;
			}
			return true;
		}
	}

	public void move(int dx, int dy)
	{
		rect.x += dx;
		rect.y += dy;
	}

	public void fluctuatePositionWithAwareness()
	{
		int rx = rand.nextInt(5) - 2;
		int ry = rand.nextInt(5) - 2;
		int outDx = 0;
		int outDy = 0;
		
		ArrayList<Ant> nearBy = AntPanel.getNearBy(this, 0, KEEP_RADIUS, true);
		if (!nearBy.isEmpty())
		{
			Ant nearest = getNearestAnt(nearBy);
			int distance = distanceTo(nearest);
			if (distance < KEEP_RADIUS)
			{
				double scale = (double)(HOOK_UP_RADIUS - distance) / (double)HOOK_UP_RADIUS;
				outDx = (int)(scale * (rect.x - nearest.rect.x));
				outDy = (int)(scale * (rect.y - nearest.rect.y));
			}
		}
		int dx = rx + outDx;
		int dy = ry + outDy;

		move(dx, dy);
	}

	public int distanceTo(Ant ant)
	{
		return (int) Math.sqrt((rect.x - ant.rect.x) * (rect.x - ant.rect.x) + (rect.y - ant.rect.y) * (rect.y - ant.rect.y));
	}

	public int distanceTo(int x, int y)
	{
		return (int) Math.sqrt((rect.x - x) * (rect.x - x) + (rect.y - y) * (rect.y - y));
	}

	public int distanceTo(Pair<Ant, Ant> pair)
	{
		int tx1 = 0, tx2 = 0, ty1 = 0, ty2 = 0, mx = 0, my = 0;
		int parDx = 0, parDy = 0;

		tx1 = pair.first.rect.x;
		tx2 = pair.second.rect.x;
		ty1 = pair.first.rect.y;
		ty2 = pair.second.rect.y;

		parDx = (tx1 - tx2);
		parDy = (ty1 - ty2);

		mx = tx2 + (int) (0.5 * parDx);
		my = ty2 + (int) (0.5 * parDy);

		return distanceTo(mx, my);
	}

	public boolean hasCommonChild(Pair<Ant, Ant> pair)
	{
		Ant target1 = pair.first;
		Ant target2 = pair.second;
		for (Ant child : target1.children)
		{
			if (isHooked(child, target1, target2))
			{
				return true;
			}
		}
		for (Ant child : target2.children)
		{
			if (isHooked(child, target1, target2))
			{
				return true;
			}
		}
		return false;
	}

	private Pair<Ant, Ant> getNearestPair(ArrayList<Pair<Ant, Ant>> pairs)
	{
		Pair<Ant, Ant> nearest = null;
		for (Pair<Ant, Ant> pair : pairs)
		{
			if (nearest == null)
			{
				nearest = pair;
			}
			else if (distanceTo(pair) < distanceTo(nearest))
			{
				nearest = pair;
			}
		}
		return nearest;
	}

	private Ant getNearestAnt(ArrayList<Ant> ants)
	{
		Ant nearest = null;
		for (Ant ant : ants)
		{
			if (nearest == null)
			{
				nearest = ant;
			}
			else if (distanceTo(ant) < distanceTo(nearest))
			{
				nearest = ant;
			}
		}
		return nearest;
	}

	private boolean isHooked(Ant ant, Ant target1, Ant target2)
	{
		if (ant.first != null && ant.second != null)
		{
			if ((ant.first.equals(target1) && ant.second.equals(target2)) || (ant.first.equals(target2) && ant.second.equals(target1)))
			{
				return true;
			}
		}
		return false;
	}

	public boolean isNeighbour(Ant ant)
	{
		return ant.equals(first) || ant.equals(second) || children.contains(ant);
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
			Ant ant = (Ant) obj;
			return id == ant.id;
		}
	}

	@Override
	public int hashCode()
	{
		return id;
	}

	private class Pair<F, S>
	{
		public F first;
		public S second;

		public Pair(F f, S s)
		{
			first = f;
			second = s;
		}
	}
}
