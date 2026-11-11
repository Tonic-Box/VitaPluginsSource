package com.tonic.vitaarceuuslibrary.api;

import com.tonic.api.entities.TileObjectAPI;
import com.tonic.data.wrappers.TileObjectEx;
import com.tonic.services.pathfinder.Walker;
import com.tonic.services.pathfinder.collision.CollisionMap;
import com.tonic.vitaarceuuslibrary.library.Bookcase;
import net.runelite.api.coords.WorldPoint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * Walking distances inside the Arceuus Library from the walker's collision map, with the library's staircases joining the floors.
 */
public final class LibraryMap implements Distances
{
    static final int MIN_X = 1600;
    static final int MIN_Y = 3776;
    static final int WIDTH = 68;
    static final int HEIGHT = 64;
    static final int PLANES = 3;

    private static final int LIBRARY_WEST = 1605;
    private static final int LIBRARY_EAST = 1662;
    private static final int LIBRARY_SOUTH = 3781;
    private static final int LIBRARY_NORTH = 3833;
    private static final int STAIRS_COST = 4;
    private static final int[][] STAIRS = {
            {1610, 3794, 1, 1617, 3794, 0}, {1617, 3794, 0, 1610, 3794, 1},
            {1621, 3791, 1, 1621, 3798, 2}, {1621, 3798, 2, 1621, 3791, 1},
            {1608, 3819, 2, 1615, 3819, 1}, {1615, 3819, 1, 1608, 3819, 2},
            {1610, 3828, 1, 1617, 3828, 0}, {1617, 3828, 0, 1610, 3828, 1},
            {1643, 3818, 0, 1643, 3825, 1}, {1643, 3825, 1, 1643, 3818, 0},
            {1643, 3828, 1, 1650, 3828, 2}, {1650, 3828, 2, 1643, 3828, 1},
            {1639, 3803, 2, 1639, 3810, 1}, {1639, 3810, 1, 1639, 3803, 2}
    };
    private static final int[][] STEPS = {{0, 1}, {1, 0}, {0, -1}, {-1, 0}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
    private static final int[][] CARDINALS = {{0, 1}, {1, 0}, {0, -1}, {-1, 0}};

    private final Map<WorldPoint, List<WorldPoint>> shelfStands = new HashMap<>();

    @Override
    public Table from(Collection<WorldPoint> sources)
    {
        CollisionMap collision = Walker.getCollisionMap();
        int[] distance = new int[WIDTH * HEIGHT * PLANES];
        Arrays.fill(distance, UNREACHABLE);
        PriorityQueue<int[]> queue = new PriorityQueue<>((a, b) -> Integer.compare(a[0], b[0]));
        for (WorldPoint source : sources)
        {
            int index = index(source);
            if (index >= 0)
            {
                distance[index] = 0;
                queue.add(new int[]{0, index});
            }
        }

        while (!queue.isEmpty())
        {
            int[] entry = queue.poll();
            int cost = entry[0];
            int index = entry[1];
            if (cost > distance[index])
            {
                continue;
            }
            int plane = index / (WIDTH * HEIGHT);
            int x = MIN_X + index % WIDTH;
            int y = MIN_Y + (index / WIDTH) % HEIGHT;
            for (int[] step : STEPS)
            {
                if (collision != null && !canStep(collision, x, y, plane, step[0], step[1]))
                {
                    continue;
                }
                relax(distance, queue, index(x + step[0], y + step[1], plane), cost + 1);
            }
            for (int[] stairs : STAIRS)
            {
                if (stairs[0] == x && stairs[1] == y && stairs[2] == plane)
                {
                    relax(distance, queue, index(stairs[3], stairs[4], stairs[5]), cost + STAIRS_COST);
                }
            }
        }
        return point -> {
            int index = index(point);
            return index < 0 ? UNREACHABLE : distance[index];
        };
    }

    @Override
    public List<WorldPoint> standTiles(Bookcase bookcase)
    {
        return shelfStands.computeIfAbsent(bookcase.getLocation(), LibraryMap::findShelfStands);
    }

    @Override
    public List<WorldPoint> standTiles(WorldPoint npc)
    {
        List<WorldPoint> tiles = new ArrayList<>();
        CollisionMap collision = Walker.getCollisionMap();
        for (int[] step : CARDINALS)
        {
            WorldPoint tile = npc.dx(step[0]).dy(step[1]);
            if (collision == null || collision.walkable((short) tile.getX(), (short) tile.getY(), (byte) tile.getPlane()))
            {
                tiles.add(tile);
            }
        }
        if (tiles.isEmpty())
        {
            tiles.add(npc);
        }
        return tiles;
    }

    /**
     * Forgets the cached bookshelf stand tiles, for when they were read before the scene had loaded.
     */
    public void clear()
    {
        shelfStands.clear();
    }

    /**
     * Checks whether a tile is inside the library.
     *
     * @param point the tile
     * @return whether it is inside
     */
    public static boolean contains(WorldPoint point)
    {
        return point != null && point.getX() >= LIBRARY_WEST && point.getX() <= LIBRARY_EAST && point.getY() >= LIBRARY_SOUTH && point.getY() <= LIBRARY_NORTH && point.getPlane() >= 0 && point.getPlane() < PLANES;
    }

    private static List<WorldPoint> findShelfStands(WorldPoint shelf)
    {
        TileObjectEx object = TileObjectAPI.search().atLocation(shelf).withAction("Search").first();
        if (object != null)
        {
            Set<WorldPoint> tiles = object.interactableFrom();
            if (tiles != null && !tiles.isEmpty())
            {
                return new ArrayList<>(tiles);
            }
        }
        List<WorldPoint> tiles = new ArrayList<>();
        CollisionMap collision = Walker.getCollisionMap();
        for (int[] step : CARDINALS)
        {
            WorldPoint tile = shelf.dx(step[0]).dy(step[1]);
            if (collision == null || collision.walkable((short) tile.getX(), (short) tile.getY(), (byte) tile.getPlane()))
            {
                tiles.add(tile);
            }
        }
        return tiles;
    }

    private static boolean canStep(CollisionMap collision, int x, int y, int plane, int dx, int dy)
    {
        short sx = (short) x;
        short sy = (short) y;
        byte z = (byte) plane;
        if (dx == 0 && dy == 1)
        {
            return collision.n(sx, sy, z) != 0;
        }
        if (dx == 1 && dy == 0)
        {
            return collision.e(sx, sy, z) != 0;
        }
        if (dx == 0 && dy == -1)
        {
            return collision.s(sx, sy, z) != 0;
        }
        if (dx == -1 && dy == 0)
        {
            return collision.w(sx, sy, z) != 0;
        }
        if (dx == 1 && dy == 1)
        {
            return collision.ne(sx, sy, z) != 0;
        }
        if (dx == 1)
        {
            return collision.se(sx, sy, z) != 0;
        }
        if (dy == 1)
        {
            return collision.nw(sx, sy, z) != 0;
        }
        return collision.sw(sx, sy, z) != 0;
    }

    private static void relax(int[] distance, PriorityQueue<int[]> queue, int index, int cost)
    {
        if (index >= 0 && cost < distance[index])
        {
            distance[index] = cost;
            queue.add(new int[]{cost, index});
        }
    }

    private static int index(WorldPoint point)
    {
        return index(point.getX(), point.getY(), point.getPlane());
    }

    private static int index(int x, int y, int plane)
    {
        int lx = x - MIN_X;
        int ly = y - MIN_Y;
        if (lx < 0 || ly < 0 || lx >= WIDTH || ly >= HEIGHT || plane < 0 || plane >= PLANES)
        {
            return -1;
        }
        return (plane * HEIGHT + ly) * WIDTH + lx;
    }
}
