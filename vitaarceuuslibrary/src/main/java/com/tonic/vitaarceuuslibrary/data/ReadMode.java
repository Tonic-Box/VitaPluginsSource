package com.tonic.vitaarceuuslibrary.data;

import lombok.RequiredArgsConstructor;

/**
 * When Books of arcane knowledge are read.
 */
@RequiredArgsConstructor
public enum ReadMode
{
    AFTER_HAND_IN("After each hand-in"),
    WHEN_SPACE_NEEDED("When space is needed");

    private final String label;

    @Override
    public String toString()
    {
        return label;
    }
}
