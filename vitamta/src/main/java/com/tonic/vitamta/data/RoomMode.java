package com.tonic.vitamta.data;

import lombok.RequiredArgsConstructor;

/**
 * How the room to train in is chosen.
 */
@RequiredArgsConstructor
public enum RoomMode
{
    REWARD_TARGET("Reward target"),
    LOWEST_FIRST("Lowest points first"),
    SINGLE_ROOM("Single room");

    private final String label;

    @Override
    public String toString()
    {
        return label;
    }
}
