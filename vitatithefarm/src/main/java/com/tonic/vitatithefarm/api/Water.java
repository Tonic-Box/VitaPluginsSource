package com.tonic.vitatithefarm.api;

import lombok.Value;

/**
 * The water held in the tick: charges and capacity over every watering can and Gricoller's can.
 */
@Value
public class Water
{
    public static final int CAN_CHARGES = 8;

    public static final int GRICOLLER_CHARGES = 1000;

    public static final int PER_PLANT = 3;

    int charges;
    int capacity;
    int cans;
    boolean gricoller;
    boolean emptyVessel;
    int refillItem;

    /**
     * @return whether any can is held
     */
    public boolean hasCan()
    {
        return cans > 0 || gricoller;
    }

    /**
     * @return whether every can is full
     */
    public boolean isFull()
    {
        return charges >= capacity;
    }
}
