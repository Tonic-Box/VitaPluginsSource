package com.tonic.vitamta.rooms;

/**
 * How many bones the Creature Graveyard takes before each cast: enough fruit for the most points a deposit can give, without making more fruit than the inventory holds.
 */
public final class GraveyardPlan
{
    /**
     * The most fruit worth one deposit with Bones to Peaches: 8 peaches a point, 3 points at most.
     */
    public static final int PEACH_TARGET = 24;

    /**
     * The most bananas worth one deposit: 16 a point, 3 points at most.
     */
    public static final int BANANA_TARGET = 48;

    /**
     * The fruit the most valuable bone makes.
     */
    public static final int MAX_BONE_VALUE = 4;

    private GraveyardPlan()
    {
    }

    /**
     * Decides whether to grab another bone or cast now.
     *
     * @param fruitFromBones the fruit the bones held will make, each bone making its value in fruit
     * @param bonesHeld the bones held
     * @param freeSlots the empty inventory slots
     * @param peaches whether Bones to Peaches is used
     * @return whether to grab another bone
     */
    public static boolean grabMore(int fruitFromBones, int bonesHeld, int freeSlots, boolean peaches)
    {
        int target = peaches ? PEACH_TARGET : BANANA_TARGET;
        if (fruitFromBones >= target || freeSlots == 0)
        {
            return false;
        }
        int capacity = bonesHeld + freeSlots;
        return fruitFromBones + MAX_BONE_VALUE <= capacity;
    }
}
