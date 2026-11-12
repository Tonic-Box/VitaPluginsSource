package com.tonic.vitamta.rooms;

import com.tonic.vitamta.data.AlchemyItem;

import java.util.Arrays;

/**
 * What the six Alchemists' Playground cupboards hold. The items always follow the same order round the room, so one search result places them all, as in RuneLite's plugin.
 */
public final class AlchemistLayout
{
    /**
     * The number of cupboards.
     */
    public static final int CUPBOARDS = 6;

    private final AlchemyItem[] contents = new AlchemyItem[CUPBOARDS];

    /**
     * Records a search result and fills in the rest of the room from it.
     *
     * @param cupboard the cupboard searched, 0 to 5
     * @param item what it held
     */
    public void found(int cupboard, AlchemyItem item)
    {
        int itemIndex = item.ordinal();
        for (int i = 0; i < CUPBOARDS; i++)
        {
            contents[(cupboard + i) % CUPBOARDS] = AlchemyItem.values()[(itemIndex + i) % CUPBOARDS];
        }
    }

    /**
     * @param item an item
     * @return the cupboard holding it, or -1 when unknown
     */
    public int find(AlchemyItem item)
    {
        for (int i = 0; i < CUPBOARDS; i++)
        {
            if (contents[i] == item)
            {
                return i;
            }
        }
        return -1;
    }

    /**
     * @return whether the layout is known
     */
    public boolean isKnown()
    {
        return contents[0] != null;
    }

    /**
     * Forgets the layout, for when the items are shuffled.
     */
    public void reset()
    {
        Arrays.fill(contents, null);
    }
}
