package com.tonic.vitatempoross.api;

import lombok.Getter;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;

/**
 * An NPC or object in the cove as it stood on one tick: its id, tiles and one flag whose meaning depends on the kind, such as a double fishing spot, a broken mast or an electrified cannon.
 */
@Getter
public final class Thing
{
    private final int id;
    private final WorldArea area;
    private final boolean npc;
    private final boolean flagged;

    /**
     * @param id the NPC or object id
     * @param area the tiles it covers
     * @param npc whether it is an NPC
     * @param flagged the kind-specific flag
     */
    public Thing(int id, WorldArea area, boolean npc, boolean flagged)
    {
        this.id = id;
        this.area = area;
        this.npc = npc;
        this.flagged = flagged;
    }

    /**
     * @return the south-west tile
     */
    public WorldPoint getTile()
    {
        return area.toWorldPoint();
    }

    /**
     * @param point a tile
     * @return the tiles between the point and the nearest tile of this, 0 when inside
     */
    public int distanceTo(WorldPoint point)
    {
        return area.distanceTo(point);
    }

    /**
     * @param point a tile
     * @return whether the tile is covered
     */
    public boolean covers(WorldPoint point)
    {
        return area.contains(point);
    }
}
