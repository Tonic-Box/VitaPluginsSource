package com.tonic.vitaarceuuslibrary.api;

import com.tonic.vitaarceuuslibrary.library.Bookcase;
import net.runelite.api.coords.WorldPoint;

import java.util.Collection;
import java.util.List;

/**
 * Walking distances inside the library, across its floors.
 */
public interface Distances
{
    int UNREACHABLE = Integer.MAX_VALUE;

    /**
     * Measures the distance from the nearest of some tiles to every tile.
     *
     * @param sources the tiles to measure from
     * @return the distances
     */
    Table from(Collection<WorldPoint> sources);

    /**
     * Gets the tiles a bookshelf can be searched from.
     *
     * @param bookcase the bookshelf
     * @return the tiles
     */
    List<WorldPoint> standTiles(Bookcase bookcase);

    /**
     * Gets the tiles an NPC standing on a tile can be talked to from.
     *
     * @param npc the NPC's tile
     * @return the tiles
     */
    List<WorldPoint> standTiles(WorldPoint npc);

    /**
     * Distances from a set of source tiles.
     */
    interface Table
    {
        /**
         * Gets the distance to a tile.
         *
         * @param point the tile
         * @return the distance, or UNREACHABLE
         */
        int to(WorldPoint point);

        /**
         * Gets the distance to the nearest of some tiles.
         *
         * @param points the tiles
         * @return the distance, or UNREACHABLE
         */
        default int toAny(Collection<WorldPoint> points)
        {
            int best = UNREACHABLE;
            for (WorldPoint point : points)
            {
                best = Math.min(best, to(point));
            }
            return best;
        }

        /**
         * Gets the nearest of some tiles.
         *
         * @param points the tiles
         * @return the nearest reachable tile, or null if none is reachable
         */
        default WorldPoint nearest(Collection<WorldPoint> points)
        {
            WorldPoint best = null;
            int bestDistance = UNREACHABLE;
            for (WorldPoint point : points)
            {
                int distance = to(point);
                if (distance < bestDistance)
                {
                    best = point;
                    bestDistance = distance;
                }
            }
            return best;
        }
    }
}
