package com.tonic.vitamta.data;

import lombok.RequiredArgsConstructor;

/**
 * What happens once the reward target's points are reached.
 */
@RequiredArgsConstructor
public enum RewardMode
{
    STACK_POINTS("Stack points"),
    STOP_AT_TARGET("Stop at target"),
    BUY_AND_STOP("Buy and stop"),
    BUY_AND_CONTINUE("Buy and continue");

    private final String label;

    /**
     * @return whether the target is bought from the Rewards Guardian
     */
    public boolean buys()
    {
        return this == BUY_AND_STOP || this == BUY_AND_CONTINUE;
    }

    @Override
    public String toString()
    {
        return label;
    }
}
