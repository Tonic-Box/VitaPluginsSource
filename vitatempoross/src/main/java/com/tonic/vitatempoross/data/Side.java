package com.tonic.vitatempoross.data;

import net.runelite.api.gameval.NpcID;

/**
 * One of the two ships in Tempoross Cove, known by its hosts: the ship's captain and the first mate at its totem. Everything else is matched to a ship by which hosts it stands nearer.
 */
public enum Side
{
    WEST("West ship",
            new int[]{NpcID.TEMPOROSS_INSTANCE_HOST_W, NpcID.TEMPOROSS_INSTANCE_HOST_N},
            new int[]{NpcID.TEMPOROSS_INSTANCE_HOST_W_VICTORY, NpcID.TEMPOROSS_INSTANCE_HOST_N_VICTORY}),
    EAST("East ship",
            new int[]{NpcID.TEMPOROSS_INSTANCE_HOST_E, NpcID.TEMPOROSS_INSTANCE_HOST_S},
            new int[]{NpcID.TEMPOROSS_INSTANCE_HOST_E_VICTORY, NpcID.TEMPOROSS_INSTANCE_HOST_S_VICTORY});

    private final String name;
    private final int[] hosts;
    private final int[] victoryHosts;

    Side(String name, int[] hosts, int[] victoryHosts)
    {
        this.name = name;
        this.hosts = hosts;
        this.victoryHosts = victoryHosts;
    }

    /**
     * Finds the side whose host has an id.
     *
     * @param npcId an NPC id
     * @return the side, or null when it is not a host
     */
    public static Side ofHost(int npcId)
    {
        for (Side side : values())
        {
            if (contains(side.hosts, npcId) || contains(side.victoryHosts, npcId))
            {
                return side;
            }
        }
        return null;
    }

    private static boolean contains(int[] ids, int id)
    {
        for (int value : ids)
        {
            if (value == id)
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString()
    {
        return name;
    }
}
