package com.tonic.vitamta.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.gameval.ItemID;

/**
 * What an Alchemists' Playground cupboard can hold, in the fixed order the cupboards follow round the room.
 */
@Getter
@RequiredArgsConstructor
public enum AlchemyItem
{
    LEATHER_BOOTS("Leather Boots", ItemID.MAGICTRAINING_LEATHER_BOOTS),
    ADAMANT_KITESHIELD("Adamant Kiteshield", ItemID.MAGICTRAINING_ADAMANT_KITESHIELD),
    ADAMANT_MED_HELM("Helm", ItemID.MAGICTRAINING_ADAMANT_MED_HELM),
    EMERALD("Emerald", ItemID.MAGICTRAINING_EMERALD),
    RUNE_LONGSWORD("Rune Longsword", ItemID.MAGICTRAINING_RUNE_LONGSWORD),
    EMPTY("", -1);

    private final String displayName;
    private final int itemId;

    /**
     * Finds the item a cupboard or the value table names.
     *
     * @param text the text naming it
     * @return the item, or null if none matches
     */
    public static AlchemyItem find(String text)
    {
        if (text == null)
        {
            return null;
        }
        String lower = text.toLowerCase();
        for (AlchemyItem item : values())
        {
            if (item != EMPTY && lower.contains(item.displayName.toLowerCase()))
            {
                return item;
            }
        }
        return null;
    }

    @Override
    public String toString()
    {
        return displayName.isEmpty() ? "Empty" : displayName;
    }
}
