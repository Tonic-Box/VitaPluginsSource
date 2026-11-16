package com.tonic.vitatempoross.api;

import com.tonic.vitatempoross.data.Strategy;
import lombok.Builder;
import lombok.Value;

/**
 * The settings the planner works from, copied from the config each tick.
 */
@Value
@Builder
public class Settings
{
    Strategy strategy;
    int fishPerLoad;
    int loadBelowEnergy;
    int emergencyStorm;
    boolean useSpecial;
    int buckets;
    boolean hammer;
    int collectAtPermits;
}
