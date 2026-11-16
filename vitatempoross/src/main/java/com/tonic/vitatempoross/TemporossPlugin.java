package com.tonic.vitatempoross;

import com.google.inject.Provides;
import com.tonic.util.MessageUtil;
import com.tonic.util.handler.StepHandler;
import com.tonic.vitatempoross.api.CoveState;
import com.tonic.vitatempoross.api.CoveTracker;
import com.tonic.vitatempoross.api.Planner;
import com.tonic.vitatempoross.api.Settings;
import com.tonic.vitatempoross.api.TemporossBuilder;
import com.tonic.vitatempoross.data.Action;
import com.tonic.vitatempoross.data.Where;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.Color;
import java.time.Duration;
import java.time.Instant;

/**
 * Plays Tempoross on mass worlds with the harpoon and gear the player brings: travels to the dock, boards, fishes, cooks and loads, deals with waves, fires and clouds, and collects rewards, one action per game tick through the handler system.
 */
@PluginDescriptor(
        name = "Vita Tempoross",
        description = "Plays the Tempoross skilling boss on mass worlds with the harpoon and gear you bring",
        tags = {"vita", "tempoross", "fishing", "harpoonfish", "skilling", "boss", "minigame"},
        enabledByDefault = false
)
public class TemporossPlugin extends Plugin
{
    private static final String STOPPED = "Switch on Run in the settings to start";
    private static final int MAX_FAILURES = 5;
    private static final int MIN_FISHING = 35;

    @Inject
    private Client client;

    @Inject
    private ConfigManager configManager;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private TemporossOverlay overlay;

    @Inject
    @Getter
    private TemporossConfig config;

    @Getter
    private CoveTracker tracker;

    private final Planner planner = new Planner();

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
    private int gamesAtStart;

    @Provides
    TemporossConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(TemporossConfig.class);
    }

    @Override
    protected void startUp()
    {
        configManager.setConfiguration(TemporossConfig.GROUP, TemporossConfig.RUN, false);
        tracker = new CoveTracker(client);
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
     * @return the games played since Run was switched on
     */
    public int gamesPlayed()
    {
        return tracker == null ? 0 : tracker.getGamesWon() + tracker.getGamesLost() - gamesAtStart;
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        if (client.getGameState() != GameState.LOGGED_IN || client.getLocalPlayer() == null)
        {
            return;
        }
        CoveState state = tracker.update();
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
            String problem = problem(state);
            if (problem != null)
            {
                MessageUtil.sendChatMessage(Color.RED, "Vita Tempoross: " + problem);
                switchOff("Not started: " + problem);
                return;
            }
            start = Instant.now();
            failures = 0;
            planner.reset();
            tracker.keepNet(state.isNet());
            gamesAtStart = tracker.getGamesWon() + tracker.getGamesLost();
        }
        boolean inCove = state.getWhere() == Where.GAME || state.getWhere() == Where.GAME_OVER;
        if (!inCove && handler == null)
        {
            if (config.maxRuntime() > 0 && Duration.between(start, Instant.now()).toMinutes() >= config.maxRuntime())
            {
                switchOff("Stopped: the max runtime was reached");
                return;
            }
            if (config.maxGames() > 0 && gamesPlayed() >= config.maxGames())
            {
                switchOff("Stopped: " + gamesPlayed() + " games played");
                return;
            }
        }
        if (handler != null && action != null)
        {
            Action next = planner.next(state, settings());
            if (next.getPriority() > action.getPriority())
            {
                abortAction();
            }
        }
        step(state);
    }

    @Subscribe
    public void onChatMessage(ChatMessage event)
    {
        ChatMessageType type = event.getType();
        if (type == ChatMessageType.GAMEMESSAGE || type == ChatMessageType.SPAM || type == ChatMessageType.MESBOX || type == ChatMessageType.ENGINE)
        {
            if (tracker != null)
            {
                tracker.gameMessage(event.getMessage());
            }
        }
    }

    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned event)
    {
        if (tracker != null)
        {
            tracker.objectSpawned(event.getGameObject());
        }
    }

    @Subscribe
    public void onGameObjectDespawned(GameObjectDespawned event)
    {
        if (tracker != null)
        {
            tracker.objectDespawned(event.getGameObject());
        }
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        if (tracker == null)
        {
            return;
        }
        if (event.getGameState() == GameState.LOGIN_SCREEN)
        {
            abortAction();
            tracker.clear();
        }
        else if (event.getGameState() == GameState.HOPPING)
        {
            tracker.clear();
        }
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if (TemporossConfig.GROUP.equals(event.getGroup()) && TemporossConfig.ENABLE_UI.equals(event.getKey()))
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
        return Settings.builder()
                .strategy(config.strategy())
                .fishPerLoad(config.fishPerLoad())
                .loadBelowEnergy(config.loadBelowEnergy())
                .emergencyStorm(config.emergencyStorm())
                .useSpecial(config.useSpecial())
                .buckets(config.buckets())
                .hammer(config.hammer())
                .collectAtPermits(config.collectAtPermits())
                .build();
    }

    private static String problem(CoveState state)
    {
        if (state.getHarpoon() == null)
        {
            return "no harpoon. Equip or carry a harpoon, barb-tail, dragon, infernal or crystal harpoon";
        }
        if (state.getFishingLevel() < MIN_FISHING)
        {
            return "Tempoross needs level " + MIN_FISHING + " Fishing";
        }
        return null;
    }

    private void step(CoveState state)
    {
        if (handler == null)
        {
            action = planner.next(state, settings());
            status = action.toString();
            failed = false;
            handler = TemporossBuilder.forAction(this, action);
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

    private void switchOff(String reason)
    {
        configManager.setConfiguration(TemporossConfig.GROUP, TemporossConfig.RUN, false);
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
        handler = null;
        action = null;
    }
}
