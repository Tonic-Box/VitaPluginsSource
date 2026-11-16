package com.tonic.vitatempoross;

import com.tonic.vitatempoross.data.Strategy;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

/**
 * Settings for Vita Tempoross.
 */
@ConfigGroup(TemporossConfig.GROUP)
public interface TemporossConfig extends Config
{
    String GROUP = "vitaTempoross";
    String RUN = "run";
    String ENABLE_UI = "enableUI";

    @ConfigSection(name = "Strategy", description = "How fish are caught and delivered.", position = 20)
    String strategySection = "strategy";

    @ConfigSection(name = "Supplies", description = "What is taken from the cove's crates.", position = 30)
    String suppliesSection = "supplies";

    @ConfigSection(name = "Rewards", description = "Spending reward permits at the reward pool.", position = 40)
    String rewardsSection = "rewards";

    @ConfigSection(name = "Stop", description = "When to switch Run off.", position = 50)
    String stopSection = "stop";

    @ConfigSection(name = "Timing", description = "Adjust action delays. One game tick is about 600 milliseconds.", position = 80, closedByDefault = true)
    String timingSection = "advancedTiming";

    /**
     * @return whether the overlay is shown
     */
    @ConfigItem(keyName = ENABLE_UI, name = "Show overlay", description = "Show runtime, status, the game's meters, points, permits and games played. The status line explains what the plugin is doing.", position = 0)
    default boolean enableUI()
    {
        return true;
    }

    /**
     * @return whether the plugin is running
     */
    @ConfigItem(keyName = RUN, name = "Run", description = "Switch on to start, and off to stop. It switches off whenever the plugin is enabled, so the plugin never starts on its own. It will not start without a harpoon.", position = 1)
    default boolean run()
    {
        return false;
    }

    /**
     * @return how fish are delivered
     */
    @ConfigItem(keyName = "strategy", name = "Strategy", description = "Cook for permits: catch a batch, cook it at the shrine and load it; cooked fish are worth more points. Raw for XP: fill the inventory and load raw fish.", position = 0, section = strategySection)
    default Strategy strategy()
    {
        return Strategy.COOK;
    }

    /**
     * @return the fish caught before cooking and loading, 0 for automatic
     */
    @Range(max = 26)
    @ConfigItem(keyName = "fishPerLoad", name = "Fish per load", description = "Cook for permits: how many fish to catch before cooking and loading. 0 picks 7, or 9 at 85 Fishing.", position = 1, section = strategySection)
    default int fishPerLoad()
    {
        return 0;
    }

    /**
     * @return the energy at or below which fish are loaded straight away
     */
    @Range(max = 100)
    @ConfigItem(keyName = "loadBelowEnergy", name = "Load below energy (%)", description = "Once Tempoross's energy is this low, load fish as soon as a few are held instead of waiting for a full batch.", position = 2, section = strategySection)
    default int loadBelowEnergy()
    {
        return 50;
    }

    /**
     * @return the storm intensity at which everything is loaded
     */
    @Range(min = 50, max = 100)
    @ConfigItem(keyName = "emergencyStorm", name = "Emergency load at storm (%)", description = "When the storm reaches this intensity, load every fish held, raw or cooked, and stop fetching supplies.", position = 3, section = strategySection)
    default int emergencyStorm()
    {
        return 90;
    }

    /**
     * @return whether the harpoon's special attack is used
     */
    @ConfigItem(keyName = "useSpecial", name = "Use harpoon special", description = "Use the dragon, infernal or crystal harpoon's special attack before fishing when it is wielded, special energy is full and it still raises the catch rate.", position = 4, section = strategySection)
    default boolean useSpecial()
    {
        return true;
    }

    /**
     * @return the buckets kept filled with water
     */
    @Range(max = 10)
    @ConfigItem(keyName = "buckets", name = "Water buckets", description = "Buckets to take from the cove's crate and fill at the pump, used to put out fires.", position = 0, section = suppliesSection)
    default int buckets()
    {
        return 3;
    }

    /**
     * @return whether a hammer is taken for repairs
     */
    @ConfigItem(keyName = "hammer", name = "Take a hammer", description = "Take a hammer from the cove's crate to repair broken masts and totems. An Imcando hammer counts.", position = 1, section = suppliesSection)
    default boolean hammer()
    {
        return true;
    }

    /**
     * @return the permits at which rewards are collected, 0 for never
     */
    @Range(max = 8000)
    @ConfigItem(keyName = "collectAtPermits", name = "Collect at permits", description = "Between games, once this many reward permits are held, take a net from the Spirit Angler, search the reward pool until no permits are left and bank the loot. 0 never collects.", position = 0, section = rewardsSection)
    default int collectAtPermits()
    {
        return 0;
    }

    /**
     * @return the most minutes to run, 0 for no limit
     */
    @Range(max = 1440)
    @ConfigItem(keyName = "maxRuntime", name = "Max runtime (minutes)", description = "Switch Run off after this many minutes, once the current game is over. 0 runs until another stop condition.", position = 0, section = stopSection)
    default int maxRuntime()
    {
        return 0;
    }

    /**
     * @return the games to play, 0 for no limit
     */
    @Range(max = 1000)
    @ConfigItem(keyName = "maxGames", name = "Stop after games", description = "Switch Run off after this many games, won or lost. 0 plays until another stop condition.", position = 1, section = stopSection)
    default int maxGames()
    {
        return 0;
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
