package com.tonic.vitatithefarm.data;

import lombok.Value;
import net.runelite.api.gameval.ObjectID;

/**
 * A patch decoded from its object id: the seed, the growth stage from 1 to 4 and its condition.
 */
@Value
public class PlantState
{
    public static final PlantState EMPTY = new PlantState(null, 0, Condition.EMPTY);

    private static final int STAGES = 3;
    private static final int GROWN_STAGE = 4;

    Seed seed;
    int stage;
    Condition condition;

    /**
     * @param objectId a patch object id
     * @return the state, or null when the id is not an empty patch or one of our plants
     */
    public static PlantState decode(int objectId)
    {
        if (objectId == ObjectID.HOSIDIUS_TITHE_EMPTY)
        {
            return EMPTY;
        }
        Seed seed = Seed.ofObject(objectId);
        if (seed == null)
        {
            return null;
        }
        int offset = objectId - seed.getFirstObjectId();
        if (offset >= STAGES * 3)
        {
            return new PlantState(seed, GROWN_STAGE, offset == STAGES * 3 ? Condition.GROWN : Condition.DEAD);
        }
        int stage = offset / 3 + 1;
        switch (offset % 3)
        {
            case 0:
                return new PlantState(seed, stage, Condition.DRY);
            case 1:
                return new PlantState(seed, stage, Condition.WET);
            default:
                return new PlantState(seed, stage, Condition.DEAD);
        }
    }

    /**
     * @param objectId an object id
     * @return whether it is an empty patch or one of our plants
     */
    public static boolean isPatch(int objectId)
    {
        return decode(objectId) != null;
    }

    /**
     * @return whether a living plant is in the patch
     */
    public boolean isGrowing()
    {
        return condition == Condition.DRY || condition == Condition.WET || condition == Condition.GROWN;
    }

    @Override
    public String toString()
    {
        if (condition == Condition.EMPTY)
        {
            return "empty";
        }
        return seed + " stage " + stage + " " + condition.name().toLowerCase();
    }
}
