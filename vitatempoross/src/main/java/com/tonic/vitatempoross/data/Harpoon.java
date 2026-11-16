package com.tonic.vitatempoross.data;

import net.runelite.api.gameval.ItemID;

/**
 * A harpoon that can fish harpoonfish, ordered from worst to best, with its special attack if it has one.
 */
public enum Harpoon
{
    HARPOON("Harpoon", 0, ItemID.HARPOON),
    BARB_TAIL("Barb-tail harpoon", 0, ItemID.HUNTING_BARBED_HARPOON),
    DRAGON("Dragon harpoon", 87, ItemID.DRAGON_HARPOON),
    CRYSTAL_INACTIVE("Crystal harpoon (inactive)", 87, ItemID.CRYSTAL_HARPOON_INACTIVE),
    INFERNAL("Infernal harpoon", 87, ItemID.INFERNAL_HARPOON, ItemID.INFERNAL_HARPOON_EMPTY),
    TRAILBLAZER("Infernal harpoon (or)", 87, ItemID.TRAILBLAZER_HARPOON, ItemID.TRAILBLAZER_HARPOON_EMPTY, ItemID.TRAILBLAZER_HARPOON_NO_INFERNAL),
    TRAILBLAZER_RELOADED("Infernal harpoon (or)", 87, ItemID.TRAILBLAZER_RELOADED_HARPOON, ItemID.TRAILBLAZER_RELOADED_HARPOON_EMPTY, ItemID.TRAILBLAZER_RELOADED_HARPOON_NO_INFERNAL),
    CRYSTAL("Crystal harpoon", 74, ItemID.CRYSTAL_HARPOON);

    private final String name;
    private final int specBelow;
    private final int[] ids;

    Harpoon(String name, int specBelow, int... ids)
    {
        this.name = name;
        this.specBelow = specBelow;
        this.ids = ids;
    }

    /**
     * @return whether it has the Fishstabber special attack
     */
    public boolean hasSpecial()
    {
        return specBelow > 0;
    }

    /**
     * @param fishingLevel the boosted Fishing level
     * @return whether the special attack still raises the catch rate at that level
     */
    public boolean specialHelps(int fishingLevel)
    {
        return fishingLevel < specBelow;
    }

    /**
     * Finds the harpoon an item id belongs to.
     *
     * @param itemId the item id
     * @return the harpoon, or null when the item is not one
     */
    public static Harpoon of(int itemId)
    {
        for (Harpoon harpoon : values())
        {
            for (int id : harpoon.ids)
            {
                if (id == itemId)
                {
                    return harpoon;
                }
            }
        }
        return null;
    }

    /**
     * @return every harpoon item id
     */
    public static int[] allIds()
    {
        int size = 0;
        for (Harpoon harpoon : values())
        {
            size += harpoon.ids.length;
        }
        int[] all = new int[size];
        int next = 0;
        for (Harpoon harpoon : values())
        {
            for (int id : harpoon.ids)
            {
                all[next++] = id;
            }
        }
        return all;
    }

    @Override
    public String toString()
    {
        return name;
    }
}
