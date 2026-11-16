package com.tonic.vitatempoross.api;

import lombok.Getter;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;

/**
 * A storm cloud's shadow and how many ticks remain before its lightning strikes.
 */
@Getter
public final class Shadow
{
    private final WorldArea area;
    private final int ticksLeft;

    /**
     * @param area the tiles under the cloud
     * @param ticksLeft ticks until the strike
     */
    public Shadow(WorldArea area, int ticksLeft)
    {
        this.area = area;
        this.ticksLeft = ticksLeft;
    }

    /**
     * @param point a tile
     * @param within the most ticks before the strike that count
     * @return whether the tile is under or next to the cloud and it strikes within the ticks
     */
    public boolean threatens(WorldPoint point, int within)
    {
        return ticksLeft <= within && area.distanceTo(point) <= 1;
    }
}
