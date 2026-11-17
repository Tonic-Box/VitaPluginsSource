package com.tonic.vitatithefarm.data;

import lombok.Getter;
import net.runelite.api.gameval.ItemID;

/**
 * A reward from Farmer Gricoller's shop that can be bought once, in buying order, with its point cost and every item id it can come as.
 */
@Getter
public enum Reward
{
    GRICOLLERS_CAN("Gricoller's can", 200, false, ItemID.ZEAH_WATERINGCAN),
    FARMERS_STRAWHAT("Farmer's strawhat", 75, true, ItemID.TITHE_REWARD_HAT_MALE, ItemID.TITHE_REWARD_HAT_FEMALE),
    FARMERS_TOP("Farmer's jacket or shirt", 150, true, ItemID.TITHE_REWARD_TORSO_MALE, ItemID.TITHE_REWARD_TORSO_FEMALE),
    FARMERS_TROUSERS("Farmer's boro trousers", 125, true, ItemID.TITHE_REWARD_LEGS_MALE, ItemID.TITHE_REWARD_LEGS_FEMALE),
    FARMERS_BOOTS("Farmer's boots", 50, true, ItemID.TITHE_REWARD_FEET_MALE, ItemID.TITHE_REWARD_FEET_FEMALE),
    SEED_BOX("Seed box", 250, false, ItemID.SEED_BOX, ItemID.SEED_BOX_OPEN),
    HERB_SACK("Herb sack", 250, false, ItemID.SLAYER_HERB_SACK, ItemID.SLAYER_HERB_SACK_OPEN);

    public static final int HERB_SACK_HERBLORE = 58;

    private final String name;
    private final int cost;
    private final boolean outfit;
    private final int[] ids;

    Reward(String name, int cost, boolean outfit, int... ids)
    {
        this.name = name;
        this.cost = cost;
        this.outfit = outfit;
        this.ids = ids;
    }

    /**
     * @param itemId an item id
     * @return the reward the item is, or null
     */
    public static Reward of(int itemId)
    {
        for (Reward reward : values())
        {
            if (reward.matches(itemId))
            {
                return reward;
            }
        }
        return null;
    }

    /**
     * @param itemId an item id
     * @return whether the item is this reward
     */
    public boolean matches(int itemId)
    {
        for (int id : ids)
        {
            if (id == itemId)
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString()
    {
        return name;
    }
}
