package com.tonic.vitaarceuuslibrary.data;

import lombok.RequiredArgsConstructor;

/**
 * The skill a Book of arcane knowledge is read for.
 */
@RequiredArgsConstructor
public enum XpChoice
{
    MAGIC("Magic"),
    RUNECRAFT("Runecraft"),
    LOWEST("Lowest level"),
    RANDOM("Random");

    private final String label;

    @Override
    public String toString()
    {
        return label;
    }
}
