package com.tonic.vitamta.data;

import lombok.Getter;
import net.runelite.api.gameval.ItemID;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * A Rewards Guardian item to aim for, with its price in each room's points; wands need the tier below.
 */
@Getter
public enum Reward
{
    NONE("None", -1, 0, 0, 0, 0, null),
    INFINITY_HAT("Infinity hat", ItemID.MAGICTRAINING_INFINITYHAT, 350, 400, 3000, 350, null),
    INFINITY_TOP("Infinity top", ItemID.MAGICTRAINING_INFINITYTOP, 400, 450, 4000, 400, null),
    INFINITY_BOTTOMS("Infinity bottoms", ItemID.MAGICTRAINING_INFINITYBOTTOM, 450, 500, 5000, 450, null),
    INFINITY_GLOVES("Infinity gloves", ItemID.MAGICTRAINING_INFINITYGLOVES, 175, 225, 1500, 175, null),
    INFINITY_BOOTS("Infinity boots", ItemID.MAGICTRAINING_INFINITYBOOTS, 120, 120, 1200, 120, null),
    BEGINNER_WAND("Beginner wand", ItemID.MAGICTRAINING_WAND_BEG, 30, 30, 300, 30, null),
    APPRENTICE_WAND("Apprentice wand", ItemID.MAGICTRAINING_WAND_APPR, 60, 60, 600, 60, BEGINNER_WAND),
    TEACHER_WAND("Teacher wand", ItemID.MAGICTRAINING_WAND_TEACH, 150, 200, 1500, 150, APPRENTICE_WAND),
    MASTER_WAND("Master wand", ItemID.MAGICTRAINING_WAND_MASTER, 240, 240, 2400, 240, TEACHER_WAND),
    MAGES_BOOK("Mage's book", ItemID.MAGICTRAINING_BOOKOFMAGIC, 500, 550, 6000, 500, null),
    RUNE_POUCH("Rune pouch", ItemID.BH_RUNE_POUCH, 150, 200, 1500, 150, null),
    BONES_TO_PEACHES("Bones to Peaches", -1, 200, 300, 2000, 200, null),
    ALL_ITEMS("All items", -1, 0, 0, 0, 0, null);

    private final String displayName;
    private final int itemId;
    private final Map<Room, Integer> costs;
    private final Reward previous;

    Reward(String displayName, int itemId, int telekinetic, int alchemist, int enchanting, int graveyard, Reward previous)
    {
        this.displayName = displayName;
        this.itemId = itemId;
        this.previous = previous;
        Map<Room, Integer> map = new EnumMap<>(Room.class);
        map.put(Room.TELEKINETIC, telekinetic);
        map.put(Room.ALCHEMIST, alchemist);
        map.put(Room.ENCHANTING, enchanting);
        map.put(Room.GRAVEYARD, graveyard);
        this.costs = Collections.unmodifiableMap(map);
    }

    /**
     * @return whether this is a single shop item rather than none or all
     */
    public boolean isItem()
    {
        return this != NONE && this != ALL_ITEMS;
    }

    /**
     * The shop items this target stands for, in buying order: the lower wand tiers before a wand, every item for all items.
     *
     * @param owned whether an item is already owned, which ends a wand chain early
     * @return the items still to buy
     */
    public List<Reward> items(Predicate<Reward> owned)
    {
        List<Reward> items = new ArrayList<>();
        if (this == ALL_ITEMS)
        {
            for (Reward reward : values())
            {
                if (reward.isItem() && !owned.test(reward))
                {
                    items.add(reward);
                }
            }
            return items;
        }
        if (!isItem())
        {
            return items;
        }
        Reward reward = this;
        while (reward != null && !owned.test(reward))
        {
            items.add(0, reward);
            reward = reward.previous;
        }
        return items;
    }

    /**
     * The points each room needs for this target.
     *
     * @param owned whether an item is already owned
     * @return the points per room
     */
    public Map<Room, Integer> required(Predicate<Reward> owned)
    {
        Map<Room, Integer> required = new EnumMap<>(Room.class);
        for (Room room : Room.values())
        {
            required.put(room, 0);
        }
        for (Reward item : items(owned))
        {
            for (Room room : Room.values())
            {
                required.merge(room, item.costs.get(room), Integer::sum);
            }
        }
        return required;
    }

    /**
     * @param room the room
     * @return the price in that room's points
     */
    public int cost(Room room)
    {
        return costs.get(room);
    }

    @Override
    public String toString()
    {
        return displayName;
    }
}
