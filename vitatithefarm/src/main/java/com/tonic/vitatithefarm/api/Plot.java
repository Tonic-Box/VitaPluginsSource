package com.tonic.vitatithefarm.api;

import com.tonic.vitatithefarm.data.Condition;
import com.tonic.vitatithefarm.data.PlantState;
import lombok.Value;
import net.runelite.api.coords.WorldPoint;

/**
 * One patch in one tick: where it is in template coordinates and what is in it.
 */
@Value
public class Plot
{
    WorldPoint key;

    PlantState state;

    /**
     * @return whether the plant needs watering
     */
    public boolean isDry()
    {
        return state.getCondition() == Condition.DRY;
    }

    /**
     * @return whether the plant can be harvested
     */
    public boolean isGrown()
    {
        return state.getCondition() == Condition.GROWN;
    }

    /**
     * @return whether the plant died
     */
    public boolean isDead()
    {
        return state.getCondition() == Condition.DEAD;
    }

    /**
     * @return whether the patch is empty
     */
    public boolean isEmpty()
    {
        return state.getCondition() == Condition.EMPTY;
    }
}
