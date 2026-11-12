package com.tonic.vitamta.data;

import lombok.RequiredArgsConstructor;

/**
 * Which alchemy spell the Alchemists' Playground uses.
 */
@RequiredArgsConstructor
public enum AlchemySpell
{
    BEST("Best castable"),
    HIGH("High Level Alchemy"),
    LOW("Low Level Alchemy");

    private final String label;

    @Override
    public String toString()
    {
        return label;
    }
}
