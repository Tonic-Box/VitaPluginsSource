package com.tonic.vitamta;

import com.google.inject.Provides;
import com.tonic.util.handler.StepHandler;
import com.tonic.vitamta.api.ArenaState;
import com.tonic.vitamta.api.ArenaTracker;
import com.tonic.vitamta.api.MtaBuilder;
import com.tonic.vitamta.api.Planner;
import com.tonic.vitamta.api.Settings;
import com.tonic.vitamta.data.Action;
import com.tonic.vitamta.data.Reward;
import com.tonic.vitamta.data.Room;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Plays the Mage Training Arena with the runes and staves carried: picks a room by the room mode, plays it, and buys or stops at the reward target, one action per game tick through the handler system.
 */
@PluginDescriptor(
        name = "Vita MTA",
        description = "Plays the Mage Training Arena rooms for pizazz points with the runes and staves you carry",
        tags = {"vita", "mta", "mage", "training", "arena", "magic", "pizazz"},
        enabledByDefault = false
)
public class MtaPlugin extends Plugin
{
    private static final String STOPPED = "Switch on Run in the settings to start";
    private static final int MAX_FAILURES = 5;

    @Inject
    private Client client;

    @Inject
    private ConfigManager configManager;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private ItemManager itemManager;

    @Inject
    private MtaOverlay overlay;

    @Inject
    @Getter
    private MtaConfig config;

    @Getter
    private ArenaTracker tracker;

    private final Planner planner = new Planner(new Random());

    @Getter
    @Setter
    private String status = STOPPED;

    @Getter
    private Action action;

    @Getter
    private Instant start;

    private StepHandler handler;
    private int failures;
    private boolean failed;

    @Provides
    MtaConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(MtaConfig.class);
    }

    @Override
    protected void startUp()
    {
        configManager.setConfiguration(MtaConfig.GROUP, MtaConfig.RUN, false);
        tracker = new ArenaTracker(client, itemManager);
        tracker.restoreBought(loadBought());
        stop(STOPPED);
        if (config.enableUI())
        {
            overlayManager.add(overlay);
        }
    }

    @Override
    protected void shutDown()
    {
        overlayManager.remove(overlay);
        stop(STOPPED);
    }

    /**
     * Reports that the current action could not be done; the next tick plans again, and repeated failures stop the plugin.
     *
     * @param reason what went wrong
     */
    public void actionFailed(String reason)
    {
        failed = true;
        failures++;
        status = reason;
        if (failures >= MAX_FAILURES)
        {
            switchOff("Stopped: " + reason);
        }
    }

    /**
     * Records a reward bought, remembered across sessions so it is not bought twice.
     *
     * @param reward the item bought
     */
    public void bought(Reward reward)
    {
        tracker.bought(reward);
        String value = tracker.getBought().stream().map(Enum::name).collect(Collectors.joining(","));
        configManager.setConfiguration(MtaConfig.GROUP, MtaConfig.BOUGHT, value);
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        if (client.getGameState() != GameState.LOGGED_IN || client.getLocalPlayer() == null)
        {
            return;
        }
        ArenaState state = tracker.update(config.alchemySpell());
        if (!config.run())
        {
            if (start != null)
            {
                stop(STOPPED);
            }
            return;
        }
        if (start == null)
        {
            start = Instant.now();
            failures = 0;
            planner.reset();
        }
        if (config.maxRuntime() > 0 && Duration.between(start, Instant.now()).toMinutes() >= config.maxRuntime())
        {
            switchOff("Stopped: the max runtime was reached");
            return;
        }
        if (handler != null && state.isBoneFalling() && action != null && action.getType() != Action.Type.DODGE)
        {
            abortAction();
        }
        step(state);
    }

    @Subscribe
    public void onChatMessage(ChatMessage event)
    {
        ChatMessageType type = event.getType();
        if (type == ChatMessageType.GAMEMESSAGE || type == ChatMessageType.SPAM || type == ChatMessageType.MESBOX || type == ChatMessageType.ENGINE)
        {
            tracker.gameMessage(event.getMessage());
        }
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        if (event.getGameState() == GameState.HOPPING || event.getGameState() == GameState.LOGIN_SCREEN)
        {
            abortAction();
            tracker.clear();
            tracker.restoreBought(loadBought());
        }
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if (MtaConfig.GROUP.equals(event.getGroup()) && MtaConfig.ENABLE_UI.equals(event.getKey()))
        {
            overlayManager.remove(overlay);
            if (config.enableUI())
            {
                overlayManager.add(overlay);
            }
        }
    }

    /**
     * @return the planner settings from the config
     */
    public Settings settings()
    {
        Settings.SettingsBuilder settings = Settings.builder()
                .roomMode(config.roomMode())
                .singleRoom(config.singleRoom())
                .switchMargin(config.switchMargin())
                .rewardMode(config.rewardMode())
                .target(config.rewardTarget())
                .skipCappedRooms(config.skipCappedRooms())
                .eatMinPercent(config.eatMinPercent())
                .eatMaxPercent(config.eatMaxPercent())
                .leaveBelowHitpoints(config.leaveBelowHitpoints());
        if (config.telekinetic())
        {
            settings.enabled(Room.TELEKINETIC);
        }
        if (config.alchemist())
        {
            settings.enabled(Room.ALCHEMIST);
        }
        if (config.enchanting())
        {
            settings.enabled(Room.ENCHANTING);
        }
        if (config.graveyard())
        {
            settings.enabled(Room.GRAVEYARD);
        }
        return settings.build();
    }

    private void step(ArenaState state)
    {
        if (handler == null)
        {
            action = planner.next(state, settings());
            status = action.toString();
            failed = false;
            if (action.getType() == Action.Type.STOP)
            {
                switchOff("Stopped: " + action);
                return;
            }
            handler = MtaBuilder.forAction(this, action);
        }
        if (!handler.step())
        {
            finished();
        }
    }

    private void finished()
    {
        handler = null;
        action = null;
        if (!failed)
        {
            failures = 0;
        }
    }

    private Set<Reward> loadBought()
    {
        Set<Reward> rewards = EnumSet.noneOf(Reward.class);
        String value = configManager.getConfiguration(MtaConfig.GROUP, MtaConfig.BOUGHT);
        if (value == null || value.isEmpty())
        {
            return rewards;
        }
        for (String name : value.split(","))
        {
            try
            {
                rewards.add(Reward.valueOf(name));
            }
            catch (IllegalArgumentException ignored)
            {
            }
        }
        return rewards;
    }

    private void switchOff(String reason)
    {
        configManager.setConfiguration(MtaConfig.GROUP, MtaConfig.RUN, false);
        stop(reason);
    }

    private void stop(String reason)
    {
        abortAction();
        start = null;
        status = reason;
    }

    private void abortAction()
    {
        if (handler != null)
        {
            handler.reset();
        }
        if (tracker != null)
        {
            tracker.cancelSearch();
        }
        handler = null;
        action = null;
    }
}
