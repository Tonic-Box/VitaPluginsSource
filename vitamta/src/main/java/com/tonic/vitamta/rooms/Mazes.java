package com.tonic.vitamta.rooms;

import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;

/**
 * The ten Telekinetic Theatre mazes. Several are loaded at once, so a maze's walls and finish are told apart by the maze area holding the guardian.
 */
public final class Mazes
{
    private static final WorldArea[] AREAS = {
        new WorldArea(3324, 9697, 34, 30, 0),
        new WorldArea(3358, 9704, 34, 23, 0),
        new WorldArea(3327, 9669, 31, 27, 0),
        new WorldArea(3361, 9664, 28, 39, 0),
        new WorldArea(3331, 9702, 37, 24, 1),
        new WorldArea(3368, 9704, 22, 22, 1),
        new WorldArea(3332, 9671, 35, 29, 1),
        new WorldArea(3367, 9669, 25, 34, 1),
        new WorldArea(3332, 9696, 42, 31, 2),
        new WorldArea(3331, 9667, 45, 27, 2)
    };

    private Mazes()
    {
    }

    /**
     * @param tile a tile
     * @return the maze area holding it, or null
     */
    public static WorldArea areaOf(WorldPoint tile)
    {
        if (tile == null)
        {
            return null;
        }
        for (WorldArea area : AREAS)
        {
            if (area.contains(tile))
            {
                return area;
            }
        }
        return null;
    }
}
