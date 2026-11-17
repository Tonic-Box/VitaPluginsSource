package com.tonic.vitatithefarm.api;

import com.tonic.vitatithefarm.data.Reward;
import com.tonic.vitatithefarm.data.Seed;
import lombok.Builder;
import lombok.Value;

import java.util.Set;

/**
 * The settings the planner works from, copied from the config each tick.
 */
@Value
@Builder
public class Settings
{
    Seed seed;
    int plantsPerRun;
    int drinkBelow;
    Set<Reward> rewards;
    int pointReserve;
    String stopReason;

    /**
     * @return whether a stop condition was met, so no seed is planted past the sack being filled
     */
    public boolean isDraining()
    {
        return stopReason != null;
    }
}
