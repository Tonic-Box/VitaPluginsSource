package com.tonic.vitamta;

import com.tonic.vitamta.data.AlchemySpell;
import com.tonic.vitamta.data.Reward;
import com.tonic.vitamta.data.RewardMode;
import com.tonic.vitamta.data.Room;
import com.tonic.vitamta.data.RoomMode;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

/**
 * Settings for Vita MTA.
 */
@ConfigGroup(MtaConfig.GROUP)
public interface MtaConfig extends Config
{
    String GROUP = "vitaMta";
    String RUN = "run";
    String ENABLE_UI = "enableUI";
    String BOUGHT = "bought";

    @ConfigSection(name = "Rooms", description = "Which rooms to play and how to choose between them.", position = 10)
    String roomsSection = "rooms";

    @ConfigSection(name = "Rewards", description = "The reward to aim for and what to do when its points are reached.", position = 20)
    String rewardsSection = "rewards";

    @ConfigSection(name = "Spells", description = "Spell choices.", position = 30)
    String spellsSection = "spells";

    @ConfigSection(name = "Creature Graveyard", description = "Eating and safety in the Creature Graveyard.", position = 40, closedByDefault = true)
    String graveyardSection = "graveyard";

    @ConfigSection(name = "Timing", description = "Adjust action delays. One game tick is about 600 milliseconds.", position = 80, closedByDefault = true)
    String timingSection = "advancedTiming";

    /**
     * @return whether the overlay is shown
     */
    @ConfigItem(keyName = ENABLE_UI, name = "Show overlay", description = "Show runtime, status, the room, every room's points and the reward target. The status line explains what the plugin is waiting for.", position = 0)
    default boolean enableUI()
    {
        return true;
    }

    /**
     * @return whether the plugin is running
     */
    @ConfigItem(keyName = RUN, name = "Run", description = "Switch on to start, and off to stop. It switches off whenever the plugin is enabled, so the plugin never starts on its own.", position = 1)
    default boolean run()
    {
        return false;
    }

    /**
     * @return whether the Telekinetic Theatre is played
     */
    @ConfigItem(keyName = "telekinetic", name = "Telekinetic Theatre", description = "Play the Telekinetic Theatre (Telekinetic Grab, level 33).", position = 0, section = roomsSection)
    default boolean telekinetic()
    {
        return true;
    }

    /**
     * @return whether the Alchemists' Playground is played
     */
    @ConfigItem(keyName = "alchemist", name = "Alchemists' Playground", description = "Play the Alchemists' Playground (Low Level Alchemy, level 21, or High Level Alchemy, level 55).", position = 1, section = roomsSection)
    default boolean alchemist()
    {
        return true;
    }

    /**
     * @return whether the Enchanting Chamber is played
     */
    @ConfigItem(keyName = "enchanting", name = "Enchanting Chamber", description = "Play the Enchanting Chamber with the highest enchant spell you can cast.", position = 2, section = roomsSection)
    default boolean enchanting()
    {
        return true;
    }

    /**
     * @return whether the Creature Graveyard is played
     */
    @ConfigItem(keyName = "graveyard", name = "Creature Graveyard", description = "Play the Creature Graveyard (Bones to Bananas, level 15, or Bones to Peaches once unlocked).", position = 3, section = roomsSection)
    default boolean graveyard()
    {
        return true;
    }

    /**
     * @return how the room is chosen
     */
    @ConfigItem(keyName = "roomMode", name = "Room choice", description = "Reward target: fill the room furthest behind for the target, finishing one room before the next. Lowest points first: play the room with the fewest points, staying until its share of the target is met, or with no target until it leads the lowest room by the margin. Single room: stay in one room.", position = 4, section = roomsSection)
    default RoomMode roomMode()
    {
        return RoomMode.REWARD_TARGET;
    }

    /**
     * @return the room played in single room mode
     */
    @ConfigItem(keyName = "singleRoom", name = "Single room", description = "The room played when Room choice is Single room.", position = 5, section = roomsSection)
    default Room singleRoom()
    {
        return Room.ENCHANTING;
    }

    /**
     * @return how far ahead a room may get before switching, in Telekinetic Theatre points
     */
    @Range(max = 2000)
    @ConfigItem(keyName = "switchMargin", name = "Switch margin", description = "Lowest points first without a target: stay in a room until it leads the lowest room by this many points. Rooms are compared in Telekinetic Theatre points, since Enchanting Chamber prices are about ten times higher.", position = 6, section = roomsSection)
    default int switchMargin()
    {
        return 50;
    }

    /**
     * @return the reward aimed for
     */
    @ConfigItem(keyName = "rewardTarget", name = "Reward target", description = "The Rewards Guardian item to earn points for. Wands include the lower tiers you don't carry; All items is every shop item.", position = 0, section = rewardsSection)
    default Reward rewardTarget()
    {
        return Reward.NONE;
    }

    /**
     * @return what happens when the target is reached
     */
    @ConfigItem(keyName = "rewardMode", name = "When reached", description = "Stack points: never buy or stop, keep earning. Stop at target: switch Run off once the points are there. Buy and stop: buy the target, then switch Run off. Buy and continue: buy the target, then keep earning points.", position = 1, section = rewardsSection)
    default RewardMode rewardMode()
    {
        return RewardMode.STOP_AT_TARGET;
    }

    /**
     * @return whether rooms at their pizazz cap are skipped
     */
    @ConfigItem(keyName = "skipCappedRooms", name = "Skip capped rooms", description = "Skip rooms whose points are at the cap (8,000 Telekinetic, 16,000 Alchemist, 32,000 Enchantment, 8,000 Graveyard).", position = 2, section = rewardsSection)
    default boolean skipCappedRooms()
    {
        return true;
    }

    /**
     * @return the most minutes to run, 0 for no limit
     */
    @Range(max = 1440)
    @ConfigItem(keyName = "maxRuntime", name = "Max runtime (minutes)", description = "Switch Run off after this many minutes. 0 runs until another stop condition.", position = 3, section = rewardsSection)
    default int maxRuntime()
    {
        return 0;
    }

    /**
     * @return the alchemy spell used
     */
    @ConfigItem(keyName = "alchemySpell", name = "Alchemy", description = "Both give the same coins and points; High Level Alchemy gives more XP. Best uses High when you can cast it.", position = 0, section = spellsSection)
    default AlchemySpell alchemySpell()
    {
        return AlchemySpell.BEST;
    }

    /**
     * @return the lowest hitpoints percentage that triggers eating
     */
    @Range(min = 10, max = 95)
    @ConfigItem(keyName = "eatMinPercent", name = "Eat below min (%)", description = "Fruit is eaten when hitpoints fall below a random percentage between this and the max, chosen again after each meal.", position = 0, section = graveyardSection)
    default int eatMinPercent()
    {
        return 40;
    }

    /**
     * @return the highest hitpoints percentage that triggers eating
     */
    @Range(min = 10, max = 95)
    @ConfigItem(keyName = "eatMaxPercent", name = "Eat below max (%)", description = "Fruit is eaten when hitpoints fall below a random percentage between the min and this, chosen again after each meal.", position = 1, section = graveyardSection)
    default int eatMaxPercent()
    {
        return 60;
    }

    /**
     * @return the hitpoints below which the graveyard is left when no fruit can be made
     */
    @Range(min = 1, max = 99)
    @ConfigItem(keyName = "leaveBelowHitpoints", name = "Leave below (HP)", description = "Leave the graveyard for another room when hitpoints are this low and there is no fruit or bones to make it from. It is played again once hitpoints recover.", position = 2, section = graveyardSection)
    default int leaveBelowHitpoints()
    {
        return 6;
    }

    /**
     * @return the lowest action delay in ticks
     */
    @Range(max = 10)
    @ConfigItem(keyName = "tickDelayMin", name = "Minimum delay (ticks)", description = "Lowest action delay, in game ticks.", position = 0, section = timingSection)
    default int tickDelayMin()
    {
        return 0;
    }

    /**
     * @return the highest action delay in ticks
     */
    @Range(max = 10)
    @ConfigItem(keyName = "tickDelayMax", name = "Maximum delay (ticks)", description = "Highest action delay, in game ticks.", position = 1, section = timingSection)
    default int tickDelayMax()
    {
        return 1;
    }

    /**
     * @return the target action delay in ticks
     */
    @Range(max = 10)
    @ConfigItem(keyName = "tickDelayTarget", name = "Target delay (ticks)", description = "Target value used to generate random action delays, in game ticks.", position = 2, section = timingSection)
    default int tickDelayTarget()
    {
        return 0;
    }

    /**
     * @return the variation around the target delay in ticks
     */
    @Range(max = 10)
    @ConfigItem(keyName = "tickDelayDeviation", name = "Delay variation (ticks)", description = "Amount of random variation around the target delay, in game ticks.", position = 3, section = timingSection)
    default int tickDelayDeviation()
    {
        return 0;
    }

    /**
     * @return whether delays use the weighted distribution
     */
    @ConfigItem(keyName = "tickDelayWeightedDistribution", name = "Weighted distribution", description = "Change how random delays are distributed within the minimum and maximum.", position = 4, section = timingSection)
    default boolean tickDelayWeightedDistribution()
    {
        return false;
    }
}
