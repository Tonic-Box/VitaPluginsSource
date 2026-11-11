package com.tonic.vitaarceuuslibrary.data;

import lombok.RequiredArgsConstructor;

/**
 * Whether spare books are collected so later requests are quicker.
 */
@RequiredArgsConstructor
public enum EfficientMode
{
    OFF("Off"),
    DETOUR("Small detours"),
    STOCK_UP("Stock up");

    private final String label;

    @Override
    public String toString()
    {
        return label;
    }
}
