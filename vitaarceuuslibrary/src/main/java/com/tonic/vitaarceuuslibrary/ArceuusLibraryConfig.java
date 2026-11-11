package com.tonic.vitaarceuuslibrary;

import com.tonic.vitaarceuuslibrary.data.EfficientMode;
import com.tonic.vitaarceuuslibrary.data.ReadMode;
import com.tonic.vitaarceuuslibrary.data.XpChoice;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

/**
 * Settings for Vita Arceuus Library.
 */
@ConfigGroup(ArceuusLibraryConfig.GROUP)
public interface ArceuusLibraryConfig extends Config
{
    String GROUP = "vitaArceuusLibrary";
    String RUN = "run";
    String ENABLE_UI = "enableUI";

    @ConfigSection(name = "General", description = "Which XP to take and how to collect books.", position = 20, closedByDefault = false)
    String generalSection = "general";

    @ConfigSection(name = "Timing", description = "Adjust action delays. One game tick is about 600 milliseconds.", position = 80, closedByDefault = true)
    String timingSection = "advancedTiming";

    /**
     * @return whether the overlay is shown
     */
    @ConfigItem(keyName = ENABLE_UI, name = "Show overlay", description = "Show runtime, status, the current request and books delivered. The status line explains what the plugin is waiting for.", position = 0)
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
     * @return the skill rewards are read for
     */
    @ConfigItem(keyName = "xpChoice", name = "XP", description = "Which skill Books of arcane knowledge are read for: Magic (15x level), Runecraft (5x level), the one with the lower level (Magic on a tie), or a random one each time.", position = 0, section = generalSection)
    default XpChoice xpChoice()
    {
        return XpChoice.MAGIC;
    }

    /**
     * @return when rewards are read
     */
    @ConfigItem(keyName = "readMode", name = "Read rewards", description = "Read each Book of arcane knowledge right after a hand-in, or keep them until inventory space is needed.", position = 1, section = generalSection)
    default ReadMode readMode()
    {
        return ReadMode.AFTER_HAND_IN;
    }

    /**
     * @return how spare books are collected
     */
    @ConfigItem(keyName = "efficientMode", name = "Efficient mode", description = "Off: only fetch the requested book. Small detours: pick up books you don't have when they are close to the route. Stock up: collect every book you don't have while inventory space allows, serve requests from that stock, and refill it in one trip once enough books are missing.", position = 2, section = generalSection)
    default EfficientMode efficientMode()
    {
        return EfficientMode.OFF;
    }

    /**
     * @return the most tiles a detour for a spare book may add
     */
    @Range(min = 1, max = 30)
    @ConfigItem(keyName = "maxDetour", name = "Max detour (tiles)", description = "Small detours: the most tiles picking up a spare book may add to a trip. Taking another copy of the book just handed in may add three times this.", position = 3, section = generalSection)
    default int maxDetour()
    {
        return 6;
    }

    /**
     * @return how many books may be missing from the stock before a refill trip
     */
    @Range(min = 1, max = 15)
    @ConfigItem(keyName = "restockAt", name = "Restock at (missing)", description = "Stock up: requests are served from the books carried, and books handed in are only picked up again on small detours until this many are missing. Then every missing book is collected in one trip.", position = 4, section = generalSection)
    default int restockAt()
    {
        return 4;
    }

    /**
     * @return the lowest action delay in ticks
     */
    @Range(min = 0, max = 10)
    @ConfigItem(keyName = "tickDelayMin", name = "Minimum delay (ticks)", description = "Lowest action delay, in game ticks.", position = 0, section = timingSection)
    default int tickDelayMin()
    {
        return 0;
    }

    /**
     * @return the highest action delay in ticks
     */
    @Range(min = 0, max = 10)
    @ConfigItem(keyName = "tickDelayMax", name = "Maximum delay (ticks)", description = "Highest action delay, in game ticks.", position = 1, section = timingSection)
    default int tickDelayMax()
    {
        return 1;
    }

    /**
     * @return the target action delay in ticks
     */
    @Range(min = 0, max = 10)
    @ConfigItem(keyName = "tickDelayTarget", name = "Target delay (ticks)", description = "Target value used to generate random action delays, in game ticks.", position = 2, section = timingSection)
    default int tickDelayTarget()
    {
        return 1;
    }

    /**
     * @return the variation around the target delay in ticks
     */
    @Range(min = 0, max = 10)
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
