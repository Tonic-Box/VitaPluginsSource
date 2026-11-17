package com.tonic.vitatithefarm;

import com.tonic.vitatithefarm.data.SeedChoice;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

/**
 * Settings for Vita Tithe Farm.
 */
@ConfigGroup(TitheFarmConfig.GROUP)
public interface TitheFarmConfig extends Config
{
    String GROUP = "vitaTitheFarm";
    String RUN = "run";
    String ENABLE_UI = "enableUI";

    @ConfigSection(name = "Farming", description = "What is planted and how many at a time.", position = 20)
    String farmingSection = "farming";

    @ConfigSection(name = "Supplies", description = "Run energy.", position = 30)
    String suppliesSection = "supplies";

    @ConfigSection(name = "Rewards", description = "Spending reward points at Farmer Gricoller's shop.", position = 40)
    String rewardsSection = "rewards";

    @ConfigSection(name = "Stop", description = "When to switch Run off. The plugin finishes the sack it is filling first, so it never leaves with a part-filled sack.", position = 50)
    String stopSection = "stop";

    @ConfigSection(name = "Timing", description = "Adjust action delays. One game tick is about 600 milliseconds.", position = 80, closedByDefault = true)
    String timingSection = "advancedTiming";

    /**
     * @return whether the overlay is shown
     */
    @ConfigItem(keyName = ENABLE_UI, name = "Show overlay", description = "Show runtime, status, water, points, the sack and the patches while running. The status line explains what the plugin is doing.", position = 0)
    default boolean enableUI()
    {
        return true;
    }

    /**
     * @return whether the plugin is running
     */
    @ConfigItem(keyName = RUN, name = "Run", description = "Switch on to start, and off to stop. It switches off whenever the plugin is enabled, so the plugin never starts on its own. It will not start without a watering can or Gricoller's can.", position = 1)
    default boolean run()
    {
        return false;
    }

    /**
     * @return the seed to plant
     */
    @ConfigItem(keyName = "seed", name = "Seed", description = "Automatic plants the best seed for your Farming level: Golovanova at 34, Bologano at 54, Logavano at 74. Pick one to plant a lower tier.", position = 0, section = farmingSection)
    default SeedChoice seed()
    {
        return SeedChoice.AUTOMATIC;
    }

    /**
     * @return the plants grown at a time
     */
    @Range(min = 4, max = 25)
    @ConfigItem(keyName = "plantsPerRun", name = "Plants per run", description = "How many patches each run plants, in the wiki's 25x4 route order. A run plants and waters each in order, waters each again as it dries, twice, then harvests in order. If planting takes over 140 ticks the run plants fewer, so nothing dries out. Without Humidify, fewer are planted if the cans cannot water them all.", position = 1, section = farmingSection)
    default int plantsPerRun()
    {
        return 20;
    }

    /**
     * @return the run energy below which a potion is drunk
     */
    @Range(max = 100)
    @ConfigItem(keyName = "drinkBelow", name = "Drink below energy (%)", description = "Drink a stamina, super energy or energy potion from the inventory when run energy falls below this. Without potions the plugin walks when energy runs out.", position = 0, section = suppliesSection)
    default int drinkBelow()
    {
        return 30;
    }

    /**
     * @return whether Gricoller's can is bought
     */
    @ConfigItem(keyName = "buyCan", name = "Buy Gricoller's can", description = "Buy Gricoller's can (200 points) when you do not own one. Ownership is checked in the inventory, worn items and the bank as last seen.", position = 0, section = rewardsSection)
    default boolean buyCan()
    {
        return false;
    }

    /**
     * @return whether the farmer's outfit is bought and worn
     */
    @ConfigItem(keyName = "buyOutfit", name = "Buy and wear farmer's outfit", description = "Buy the farmer's outfit piece by piece (400 points in all) and wear each piece.", position = 1, section = rewardsSection)
    default boolean buyOutfit()
    {
        return false;
    }

    /**
     * @return whether the seed box is bought
     */
    @ConfigItem(keyName = "buySeedBox", name = "Buy seed box", description = "Buy the seed box (250 points) when you do not own one.", position = 2, section = rewardsSection)
    default boolean buySeedBox()
    {
        return false;
    }

    /**
     * @return whether the herb sack is bought
     */
    @ConfigItem(keyName = "buyHerbSack", name = "Buy herb sack", description = "Buy the herb sack (250 points, needs 58 Herblore) when you do not own one.", position = 3, section = rewardsSection)
    default boolean buyHerbSack()
    {
        return false;
    }

    /**
     * @return the points never spent
     */
    @Range(max = 16000)
    @ConfigItem(keyName = "pointReserve", name = "Keep at least points", description = "Buying never takes the points below this.", position = 4, section = rewardsSection)
    default int pointReserve()
    {
        return 0;
    }

    /**
     * @return the most minutes to run, 0 for no limit
     */
    @Range(max = 1440)
    @ConfigItem(keyName = "maxRuntime", name = "Max runtime (minutes)", description = "Stop after this many minutes, once the sack being filled is full. 0 runs until another stop condition.", position = 0, section = stopSection)
    default int maxRuntime()
    {
        return 0;
    }

    /**
     * @return the points to stop at, 0 for none
     */
    @Range(max = 16000)
    @ConfigItem(keyName = "pointsTarget", name = "Points target", description = "Stop once you hold this many reward points. 0 ignores points.", position = 1, section = stopSection)
    default int pointsTarget()
    {
        return 0;
    }

    /**
     * @return the Farming level to stop at, 0 for none
     */
    @Range(max = 99)
    @ConfigItem(keyName = "levelTarget", name = "Farming level target", description = "Stop at this Farming level. 0 ignores the level.", position = 2, section = stopSection)
    default int levelTarget()
    {
        return 0;
    }

    /**
     * @return the full sacks to stop after, 0 for none
     */
    @Range(max = 1000)
    @ConfigItem(keyName = "maxSacks", name = "Stop after sacks", description = "Stop after filling this many sacks of 100 fruit. 0 ignores sacks.", position = 3, section = stopSection)
    default int maxSacks()
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
