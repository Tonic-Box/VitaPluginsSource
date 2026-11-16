package com.tonic.vitatempoross.data;

/**
 * How fish are delivered to the cannons.
 */
public enum Strategy
{
    COOK("Cook for permits"),
    RAW("Raw for XP");

    private final String name;

    Strategy(String name)
    {
        this.name = name;
    }

    @Override
    public String toString()
    {
        return name;
    }
}
