package com.tonic.vitatithefarm.api;

import com.tonic.vitatithefarm.data.FarmMap;
import lombok.Getter;
import net.runelite.api.CollisionDataFlag;
import net.runelite.api.coords.WorldPoint;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The farm's walkable tiles in template coordinates, with walking distances that go around patches, walls and barrels.
 */
public final class FarmGrid
{
    public static final int MIN_X = 1798;

    public static final int MIN_Y = 3476;

    public static final int WIDTH = 44;

    public static final int HEIGHT = 46;

    private static final int BLOCKED = CollisionDataFlag.BLOCK_MOVEMENT_FULL;
    private static final int UNREACHABLE = Integer.MAX_VALUE;
    private static final int[][] STEPS = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};

    private final int[][] flags;
    @Getter
    private final boolean live;
    private final Map<Integer, int[]> fields = new HashMap<>();

    /**
     * @param flags collision flags by grid column then row, as the client keeps them
     * @param live whether they were read from the game rather than made up from the layout
     */
    public FarmGrid(int[][] flags, boolean live)
    {
        this.flags = flags;
        this.live = live;
    }

    /**
     * @return a grid made from the layout alone, where only the patches, barrels and sacks are in the way
     */
    public static FarmGrid fromLayout()
    {
        int[][] flags = new int[WIDTH][HEIGHT];
        int half = FarmMap.PATCH_SIZE / 2;
        for (WorldPoint centre : FarmMap.PATCHES)
        {
            for (int dx = -half; dx <= half; dx++)
            {
                for (int dy = -half; dy <= half; dy++)
                {
                    block(flags, centre.getX() + dx, centre.getY() + dy);
                }
            }
        }
        for (WorldPoint point : FarmMap.BARRELS)
        {
            block(flags, point.getX(), point.getY());
        }
        for (WorldPoint point : FarmMap.SACKS)
        {
            block(flags, point.getX(), point.getY());
        }
        return new FarmGrid(flags, false);
    }

    private static void block(int[][] flags, int x, int y)
    {
        int gx = x - MIN_X;
        int gy = y - MIN_Y;
        if (gx >= 0 && gy >= 0 && gx < WIDTH && gy < HEIGHT)
        {
            flags[gx][gy] = BLOCKED;
        }
    }

    /**
     * Finds the nearest of some tiles by walking distance.
     *
     * @param from where the walk starts, in template coordinates
     * @param targets the tiles that would do
     * @return the nearest target and the tiles walked, or a straight-line guess when none can be reached
     */
    public Reach reach(WorldPoint from, List<WorldPoint> targets)
    {
        int[] field = field(from);
        WorldPoint best = null;
        int bestSteps = UNREACHABLE;
        for (WorldPoint target : targets)
        {
            int index = index(target.getX(), target.getY());
            int steps = field == null || index < 0 ? UNREACHABLE : field[index];
            if (steps < bestSteps)
            {
                best = target;
                bestSteps = steps;
            }
        }
        if (best != null)
        {
            return new Reach(best, bestSteps);
        }
        WorldPoint nearest = FarmMap.nearest(from, targets);
        return new Reach(nearest, nearest == null ? 0 : FarmMap.distance(from, nearest));
    }

    private int[] field(WorldPoint from)
    {
        int start = index(from.getX(), from.getY());
        if (start < 0)
        {
            return null;
        }
        int[] cached = fields.get(start);
        if (cached != null)
        {
            return cached;
        }
        int[] field = new int[WIDTH * HEIGHT];
        Arrays.fill(field, UNREACHABLE);
        field[start] = 0;
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        queue.add(start);
        while (!queue.isEmpty())
        {
            int at = queue.poll();
            int x = at / HEIGHT;
            int y = at % HEIGHT;
            for (int[] step : STEPS)
            {
                if (!canStep(x, y, step[0], step[1]))
                {
                    continue;
                }
                int next = (x + step[0]) * HEIGHT + y + step[1];
                if (field[next] == UNREACHABLE)
                {
                    field[next] = field[at] + 1;
                    queue.add(next);
                }
            }
        }
        fields.put(start, field);
        return field;
    }

    private boolean canStep(int x, int y, int dx, int dy)
    {
        if (dx != 0 && dy != 0)
        {
            return canStep(x, y, dx, 0) && canStep(x, y, 0, dy) && canStep(x + dx, y, 0, dy) && canStep(x, y + dy, dx, 0);
        }
        int toX = x + dx;
        int toY = y + dy;
        if (toX < 0 || toY < 0 || toX >= WIDTH || toY >= HEIGHT || (flags[toX][toY] & BLOCKED) != 0)
        {
            return false;
        }
        int wall;
        if (dx > 0)
        {
            wall = CollisionDataFlag.BLOCK_MOVEMENT_EAST;
        }
        else if (dx < 0)
        {
            wall = CollisionDataFlag.BLOCK_MOVEMENT_WEST;
        }
        else if (dy > 0)
        {
            wall = CollisionDataFlag.BLOCK_MOVEMENT_NORTH;
        }
        else
        {
            wall = CollisionDataFlag.BLOCK_MOVEMENT_SOUTH;
        }
        return (flags[x][y] & wall) == 0;
    }

    private static int index(int x, int y)
    {
        int gx = x - MIN_X;
        int gy = y - MIN_Y;
        if (gx < 0 || gy < 0 || gx >= WIDTH || gy >= HEIGHT)
        {
            return -1;
        }
        return gx * HEIGHT + gy;
    }

    /**
     * The tile a walk ends on and how many tiles it takes.
     */
    @lombok.Value
    public static class Reach
    {
        WorldPoint tile;
        int steps;
    }
}
