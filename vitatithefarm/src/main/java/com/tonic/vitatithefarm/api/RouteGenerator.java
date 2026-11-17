package com.tonic.vitatithefarm.api;

import com.tonic.vitatithefarm.data.FarmMap;
import net.runelite.api.coords.WorldPoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The planting order for a number of plants, taken from the wiki's 25x4 route: south down the lane west of column 3, round the lone patch, north up the lane east of it, and back across the north end, so the loop closes beside where it began.
 */
public final class RouteGenerator
{
    public static final int MIN_PLANTS = 4;

    public static final int MAX_PLANTS = 25;

    private static final int LONE = -1;

    private static final int[][] ROUTE_25 = {
            {3, 4}, {2, 4}, {2, 3}, {2, 2}, {3, 2}, {2, 1}, {LONE, LONE}, {4, 1}, {3, 1}, {4, 2}, {4, 3}, {3, 3}, {4, 4},
            {4, 5}, {4, 6}, {3, 6}, {4, 7}, {4, 8}, {3, 8}, {2, 8}, {2, 7}, {3, 7}, {2, 6}, {2, 5}, {3, 5}
    };

    private static final int[][] DROP_ORDER = {
            {LONE, LONE}, {3, 5}, {3, 7}, {3, 8}, {3, 6}, {3, 3}, {3, 1}, {3, 2}, {3, 4}, {2, 1}, {4, 1}, {2, 2}
    };

    private static final int[][] ROUTE_12 = {
            {2, 8}, {3, 8}, {2, 7}, {3, 7}, {2, 6}, {3, 6}, {2, 5}, {3, 5}, {4, 5}, {4, 6}, {4, 7}, {4, 8}
    };

    private RouteGenerator()
    {
    }

    /**
     * Builds the route: from 13 plants up, the 25x4 order with column 3 and the lone patch dropped first so the loop keeps its shape; below that, a loop round the north half of column 3.
     *
     * @param plants the plants in the run, from 1 to 25
     * @return the patch centres in planting order, in template coordinates
     */
    public static List<WorldPoint> generate(int plants)
    {
        int count = Math.max(1, Math.min(MAX_PLANTS, plants));
        List<WorldPoint> route = new ArrayList<>();
        if (count <= ROUTE_12.length)
        {
            for (int i = 0; i < count; i++)
            {
                route.add(patch(ROUTE_12[i]));
            }
            return Collections.unmodifiableList(route);
        }
        List<WorldPoint> dropped = new ArrayList<>();
        for (int i = 0; i < MAX_PLANTS - count; i++)
        {
            dropped.add(patch(DROP_ORDER[i]));
        }
        for (int[] patch : ROUTE_25)
        {
            WorldPoint centre = patch(patch);
            if (!dropped.contains(centre))
            {
                route.add(centre);
            }
        }
        return Collections.unmodifiableList(route);
    }

    private static WorldPoint patch(int[] columnRow)
    {
        if (columnRow[0] == LONE)
        {
            return FarmMap.LONE_PATCH;
        }
        return FarmMap.patch(columnRow[0] - 1, columnRow[1] - 1);
    }
}
