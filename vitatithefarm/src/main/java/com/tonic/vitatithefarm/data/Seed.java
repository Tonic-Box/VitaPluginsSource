package com.tonic.vitatithefarm.data;

import lombok.Getter;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.ObjectID;

/**
 * A Tithe Farm seed tier with its Farming level, items and the first of its eleven plant object ids.
 */
@Getter
public enum Seed
{
    GOLOVANOVA("Golovanova", 34, ItemID.HOSIDIUS_TITHE_SEED_A, ItemID.HOSIDIUS_TITHE_FRUIT_A, ObjectID.HOSIDIUS_TITHE_A_1_DRY),
    BOLOGANO("Bologano", 54, ItemID.HOSIDIUS_TITHE_SEED_B, ItemID.HOSIDIUS_TITHE_FRUIT_B, ObjectID.HOSIDIUS_TITHE_B_1_DRY),
    LOGAVANO("Logavano", 74, ItemID.HOSIDIUS_TITHE_SEED_C, ItemID.HOSIDIUS_TITHE_FRUIT_C, ObjectID.HOSIDIUS_TITHE_C_1_DRY);

    public static final int OBJECTS_PER_SEED = 11;

    private final String name;
    private final int level;
    private final int seedId;
    private final int fruitId;
    private final int firstObjectId;

    Seed(String name, int level, int seedId, int fruitId, int firstObjectId)
    {
        this.name = name;
        this.level = level;
        this.seedId = seedId;
        this.fruitId = fruitId;
        this.firstObjectId = firstObjectId;
    }

    /**
     * @param farmingLevel the Farming level
     * @return the best seed for the level, or null below the lowest
     */
    public static Seed best(int farmingLevel)
    {
        Seed best = null;
        for (Seed seed : values())
        {
            if (farmingLevel >= seed.level)
            {
                best = seed;
            }
        }
        return best;
    }

    /**
     * @param objectId a plant object id
     * @return the seed it belongs to, or null when it is not a plant of ours
     */
    public static Seed ofObject(int objectId)
    {
        for (Seed seed : values())
        {
            if (objectId >= seed.firstObjectId && objectId < seed.firstObjectId + OBJECTS_PER_SEED)
            {
                return seed;
            }
        }
        return null;
    }

    /**
     * @param itemId an item id
     * @return the seed whose seed item it is, or null
     */
    public static Seed ofSeedItem(int itemId)
    {
        for (Seed seed : values())
        {
            if (seed.seedId == itemId)
            {
                return seed;
            }
        }
        return null;
    }

    @Override
    public String toString()
    {
        return name;
    }
}
