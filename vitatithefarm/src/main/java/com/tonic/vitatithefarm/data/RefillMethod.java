package com.tonic.vitatithefarm.data;

/**
 * How empty watering cans are filled again.
 */
public enum RefillMethod
{
    HUMIDIFY("Humidify"),
    BARREL("water barrel");

    private final String name;

    RefillMethod(String name)
    {
        this.name = name;
    }

    @Override
    public String toString()
    {
        return name;
    }
}
