package com.tonic.vitamta.rooms;

import net.runelite.api.coords.Direction;
import net.runelite.api.coords.WorldPoint;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Solves a Telekinetic Theatre maze, ported from RuneLite's Mage Training Arena plugin: each cast slides the guardian until it is blocked, so the fewest casts come from a breadth-first search over slides, and each cast is made from the side of the maze it pulls towards.
 */
public final class TelekineticSolver
{
    private static final Direction[] DIRECTIONS = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};

    /**
     * Slides the guardian from a tile in a direction until it is blocked.
     */
    @FunctionalInterface
    public interface Slider
    {
        /**
         * @param from where the guardian stands
         * @param direction where it is pulled
         * @return where it stops, or the same tile when it cannot move
         */
        WorldPoint slide(WorldPoint from, Direction direction);
    }

    private final int minX;
    private final int minY;
    private final int maxX;
    private final int maxY;
    private final int plane;

    /**
     * Creates a solver for a maze bounded by its wall objects.
     *
     * @param walls the tiles of the maze's wall objects
     */
    public TelekineticSolver(Collection<WorldPoint> walls)
    {
        int lowX = Integer.MAX_VALUE;
        int lowY = Integer.MAX_VALUE;
        int highX = Integer.MIN_VALUE;
        int highY = Integer.MIN_VALUE;
        int z = 0;
        for (WorldPoint wall : walls)
        {
            lowX = Math.min(lowX, wall.getX());
            lowY = Math.min(lowY, wall.getY());
            highX = Math.max(highX, wall.getX());
            highY = Math.max(highY, wall.getY());
            z = wall.getPlane();
        }
        this.minX = lowX;
        this.minY = lowY;
        this.maxX = highX;
        this.maxY = highY;
        this.plane = z;
    }

    /**
     * Finds the fewest casts that bring the guardian to the finish.
     *
     * @param start where the guardian stands, or will stop if it is moving
     * @param finish the finish tile
     * @param slider the slide rule
     * @return the directions to pull it in, first cast first; empty when it is already there or cannot get there
     */
    public static List<Direction> solve(WorldPoint start, WorldPoint finish, Slider slider)
    {
        Map<WorldPoint, WorldPoint> previous = new HashMap<>();
        Map<WorldPoint, Direction> moves = new HashMap<>();
        Deque<WorldPoint> queue = new ArrayDeque<>();
        previous.put(start, start);
        queue.add(start);
        while (!queue.isEmpty() && !previous.containsKey(finish))
        {
            WorldPoint tile = queue.poll();
            for (Direction direction : DIRECTIONS)
            {
                WorldPoint next = slider.slide(tile, direction);
                if (next.equals(tile) || previous.containsKey(next))
                {
                    continue;
                }
                previous.put(next, tile);
                moves.put(next, direction);
                queue.add(next);
            }
        }
        List<Direction> path = new ArrayList<>();
        if (!previous.containsKey(finish))
        {
            return path;
        }
        WorldPoint tile = finish;
        while (!tile.equals(start))
        {
            path.add(moves.get(tile));
            tile = previous.get(tile);
        }
        Collections.reverse(path);
        return path;
    }

    /**
     * @param player the player's tile
     * @return the side of the maze the player stands on, or null on a corner or elsewhere
     */
    public Direction side(WorldPoint player)
    {
        int x = player.getX();
        int y = player.getY();
        if (y >= maxY && x < maxX && x > minX)
        {
            return Direction.NORTH;
        }
        if (y <= minY && x < maxX && x > minX)
        {
            return Direction.SOUTH;
        }
        if (x >= maxX && y < maxY && y > minY)
        {
            return Direction.EAST;
        }
        if (x <= minX && y < maxY && y > minY)
        {
            return Direction.WEST;
        }
        return null;
    }

    /**
     * Picks the tile to cast the next pull from: on that pull's side, as close as possible to both the player and the side of the pull after it.
     *
     * @param player the player's tile
     * @param next the next pull
     * @param after the pull after it, or null
     * @return the tile
     */
    public WorldPoint standTile(WorldPoint player, Direction next, Direction after)
    {
        List<WorldPoint> line = line(next);
        WorldPoint nearestNext = nearest(line, player);
        if (after == null)
        {
            return nearestNext;
        }
        WorldPoint nearestAfter = nearest(line(after), nearestNext);
        return nearest(line, nearestAfter);
    }

    /**
     * @param direction a side of the maze
     * @return the tiles along that side, corners excluded
     */
    public List<WorldPoint> line(Direction direction)
    {
        List<WorldPoint> tiles = new ArrayList<>();
        switch (direction)
        {
            case NORTH:
            case SOUTH:
                for (int x = minX + 1; x < maxX; x++)
                {
                    tiles.add(new WorldPoint(x, direction == Direction.NORTH ? maxY : minY, plane));
                }
                break;
            default:
                for (int y = minY + 1; y < maxY; y++)
                {
                    tiles.add(new WorldPoint(direction == Direction.EAST ? maxX : minX, y, plane));
                }
                break;
        }
        return tiles;
    }

    private static WorldPoint nearest(List<WorldPoint> tiles, WorldPoint from)
    {
        WorldPoint best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (WorldPoint tile : tiles)
        {
            int distance = Math.abs(tile.getX() - from.getX()) + Math.abs(tile.getY() - from.getY());
            if (distance < bestDistance)
            {
                best = tile;
                bestDistance = distance;
            }
        }
        return best;
    }
}
