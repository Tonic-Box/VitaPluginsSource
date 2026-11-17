package com.tonic.vitatithefarm.data;

import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ObjectID;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Tithe Farm's layout in template coordinates (region 7222), the only place farm positions are written down.
 */
public final class FarmMap
{
    public static final int REGION = 7222;

    public static final int PATCH_SIZE = 3;

    public static final int[] COLUMNS = {1811, 1816, 1821, 1826, 1831};

    public static final int[] ROWS = {3489, 3492, 3495, 3498, 3504, 3507, 3510, 3513};

    public static final WorldPoint LONE_PATCH = new WorldPoint(1821, 3483, 0);

    public static final WorldPoint SEED_TABLE = new WorldPoint(1802, 3503, 0);

    public static final WorldPoint LOBBY_TILE = new WorldPoint(1802, 3501, 0);

    public static final int LOBBY_RADIUS = 15;

    public static final List<WorldPoint> SACKS = List.of(new WorldPoint(1809, 3502, 0), new WorldPoint(1820, 3500, 0), new WorldPoint(1835, 3500, 0));

    public static final List<WorldPoint> BARRELS = List.of(new WorldPoint(1809, 3500, 0), new WorldPoint(1823, 3485, 0), new WorldPoint(1835, 3502, 0), new WorldPoint(1821, 3516, 0));

    public static final int[] BARREL_IDS = {ObjectID.WATER_BARREL1, ObjectID.WATER_BARREL2, ObjectID.FARM_WATER_BARREL1, ObjectID.FARM_WATER_BARREL2};

    public static final int[] SACK_IDS = {ObjectID.TITHE_SACK_OF_FRUIT_EMPTY, ObjectID.TITHE_SACK_OF_FRUIT};

    public static final List<WorldPoint> PATCHES = patches();

    private FarmMap()
    {
    }

    /**
     * @param column the column index, 0 to 4
     * @param row the row index, 0 to 7
     * @return the patch centre
     */
    public static WorldPoint patch(int column, int row)
    {
        return new WorldPoint(COLUMNS[column], ROWS[row], 0);
    }

    /**
     * @param a a tile
     * @param b another tile
     * @return the tiles between them, moving diagonally
     */
    public static int distance(WorldPoint a, WorldPoint b)
    {
        return Math.max(Math.abs(a.getX() - b.getX()), Math.abs(a.getY() - b.getY()));
    }

    /**
     * @param from a tile
     * @param centre a patch centre
     * @return the tiles from the tile to the nearest tile beside the patch, 0 when already beside it
     */
    public static int distanceToPatch(WorldPoint from, WorldPoint centre)
    {
        int reach = PATCH_SIZE / 2 + 1;
        int dx = Math.max(0, Math.abs(from.getX() - centre.getX()) - reach);
        int dy = Math.max(0, Math.abs(from.getY() - centre.getY()) - reach);
        return Math.max(dx, dy);
    }

    /**
     * @param from a tile
     * @param points candidate tiles
     * @return the nearest candidate
     */
    public static WorldPoint nearest(WorldPoint from, List<WorldPoint> points)
    {
        WorldPoint best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (WorldPoint point : points)
        {
            int distance = distance(from, point);
            if (distance < bestDistance)
            {
                best = point;
                bestDistance = distance;
            }
        }
        return best;
    }

    /**
     * @param centre a patch centre
     * @return the tiles beside the patch's edges, where the player stands to tend it
     */
    public static List<WorldPoint> approach(WorldPoint centre)
    {
        int reach = PATCH_SIZE / 2 + 1;
        List<WorldPoint> tiles = new ArrayList<>();
        for (int i = -PATCH_SIZE / 2; i <= PATCH_SIZE / 2; i++)
        {
            tiles.add(centre.dx(i).dy(reach));
            tiles.add(centre.dx(i).dy(-reach));
            tiles.add(centre.dx(reach).dy(i));
            tiles.add(centre.dx(-reach).dy(i));
        }
        return tiles;
    }

    private static List<WorldPoint> patches()
    {
        List<WorldPoint> patches = new ArrayList<>();
        for (int column = 0; column < COLUMNS.length; column++)
        {
            for (int row = 0; row < ROWS.length; row++)
            {
                patches.add(patch(column, row));
            }
        }
        patches.add(LONE_PATCH);
        return Collections.unmodifiableList(patches);
    }
}
