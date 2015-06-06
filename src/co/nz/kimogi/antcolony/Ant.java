package co.nz.kimogi.antcolony;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Random;

import co.nz.kimogi.antcolony.AntColony.AntPanel;

public class Ant
{
	public final static int HOOK_UP_RADIUS = 40;
	public final static int KEEP_RADIUS = 30;
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
				double theta = -Math.PI;
				Point center = new Point(AntPanel.WIDTH / 2, AntPanel.HEIGHT / 2);
				int radius = 100;

				while (true)
				{
					if (Ant.this.id == 0)
					{
						theta = theta + Math.PI / 180;
						if (theta > Math.PI)
						{
							theta = -Math.PI;
						}

						Ant.this.rect.x = center.x + (int) (radius * Math.cos(theta));
						Ant.this.rect.y = center.y + (int) (radius * Math.sin(theta));
						color = children.size() < MAX_NEIGHBOUR_COUNT ? Color.BLUE : Color.RED;
					}
					else if (color == Color.GRAY)
					{
						if (AntPanel.getCount() < AntPanel.LIMIT_ANTS)
						{
							if (tryHookUp())
							{
								adjustPosition();
								AntPanel.incrementCount();
							}
							else
							{
								fluctuatePosition();
							}
						}
						else
						{
							fluctuatePosition();
						}
					}
					else if (color == Color.GREEN || color == Color.RED)
					{
						adjustPosition();
					}

					AntPanel.updateAnt(Ant.this.id, Ant.this.rect);

					try
					{
						Thread.sleep(100);
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

	private void adjustPosition()
	{
		if (first != null && second != null)
		{
			int distance1 = distanceTo(first);
			int distance2 = distanceTo(second);

			if (distance1 == distance2 && distance1 == KEEP_RADIUS)
			{
				return;
			}

			int tx1 = 0, tx2 = 0, ty1 = 0, ty2 = 0, tx = 0, ty = 0, mx = 0, my = 0;
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

			tx = mx + perDx;
			ty = my + perDy;

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
		ArrayList<Ant> nearAnts = AntPanel.getNearBy(this, HOOK_UP_RADIUS, false);

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
			for (Ant target1 : nearAnts)
			{
				for (Ant target2 : nearAnts)
				{
					if (!target1.equals(target2) && isHookable(target1, target2))
					{
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
			}
		}
		return false;
	}

	public void move(int dx, int dy)
	{
		rect.x += dx;
		rect.y += dy;
	}

	public void fluctuatePosition()
	{
		int dx = rand.nextInt(5) - 2;
		int dy = rand.nextInt(5) - 2;
		move(dx, dy);
	}

	public int distanceTo(Ant ant)
	{
		return (int) Math.sqrt((rect.x - ant.rect.x) * (rect.x - ant.rect.x) + (rect.y - ant.rect.y) * (rect.y - ant.rect.y));
	}

	public boolean isHookable(Ant target1, Ant target2)
	{
		if (target1.distanceTo(target2) == KEEP_RADIUS)
		{
			for (Ant child : target1.children)
			{
				if (isHooked(child, target1, target2))
				{
					return false;
				}
			}
			for (Ant child : target2.children)
			{
				if (isHooked(child, target1, target2))
				{
					return false;
				}
			}
			return true;
		}
		else
		{
			return false;
		}
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
}
