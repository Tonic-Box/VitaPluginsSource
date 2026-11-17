package com.tonic.vitatithefarm;

import com.google.inject.Provides;
import com.tonic.Logger;
import com.tonic.util.MessageUtil;
import com.tonic.util.handler.StepHandler;
import com.tonic.vitatithefarm.api.FarmState;
import com.tonic.vitatithefarm.api.FarmTracker;
import com.tonic.vitatithefarm.api.Planner;
import com.tonic.vitatithefarm.api.Settings;
import com.tonic.vitatithefarm.api.TitheBuilder;
import com.tonic.vitatithefarm.data.Action;
import com.tonic.vitatithefarm.data.Reward;
import com.tonic.vitatithefarm.data.Seed;
import com.tonic.vitatithefarm.data.Where;
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
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.Color;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

/**
 * Plays Tithe Farm: walks there, takes seeds, plants, waters, harvests and deposits a round of patches at a time, refilling cans with Humidify or at the water barrels, one action per game tick through the handler system.
 */
@PluginDescriptor(
        name = "Vita Tithe Farm",
        description = "Plays the Tithe Farm minigame for Farming XP and reward points",
        tags = {"vita", "tithe", "farm", "farming", "skilling", "minigame", "hosidius"},
        enabledByDefault = false
)
public class TitheFarmPlugin extends Plugin
{
    private static final String STOPPED = "Switch on Run in the settings to start";
    private static final int MAX_FAILURES = 5;
    private static final int MIN_FARMING = 34;
    private static final String LOG = "[Vita Tithe Farm] ";
    private static final String[] FATAL_MESSAGES = {"seed dibber", "need a spade"};

    @Inject
    @Getter
    private Client client;

    @Inject
    private ConfigManager configManager;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private TitheFarmOverlay overlay;

    @Inject
    @Getter
    private TitheFarmConfig config;

    @Getter
    private FarmTracker tracker;

    @Getter
    private final Planner planner = new Planner();

    @Getter
    @Setter
    private String status = STOPPED;

    @Getter
    private Action action;

    @Getter
    private Instant start;

    @Getter
    private Seed seed;

    @Getter
    private String stopReason;

    private StepHandler handler;
    private boolean ended;
    private int failures;
    private boolean failed;

    @Provides
    TitheFarmConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(TitheFarmConfig.class);
    }

    @Override
    protected void startUp()
    {
        configManager.setConfiguration(TitheFarmConfig.GROUP, TitheFarmConfig.RUN, false);
        tracker = new FarmTracker(client);
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
     * Notes that the running action has ended this tick, so the next one can start in the same tick.
     */
    public void actionEnded()
    {
        ended = true;
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        if (client.getGameState() != GameState.LOGGED_IN || client.getLocalPlayer() == null)
        {
            return;
        }
        FarmState state = tracker.update();
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
                MessageUtil.sendChatMessage(Color.RED, "Vita Tithe Farm: " + problem);
                switchOff("Not started: " + problem);
                return;
            }
            start = Instant.now();
            failures = 0;
            stopReason = null;
            planner.reset();
            tracker.resetCounters();
        }
        if (state.getWhere() != Where.FARM)
        {
            seed = config.seed().resolve(state.getFarmingLevel());
        }
        if (stopReason == null)
        {
            stopReason = stopCondition(state);
        }
        step(state);
    }

    @Subscribe
    public void onChatMessage(ChatMessage event)
    {
        ChatMessageType type = event.getType();
        if (tracker != null && (type == ChatMessageType.GAMEMESSAGE || type == ChatMessageType.SPAM || type == ChatMessageType.MESBOX || type == ChatMessageType.ENGINE))
        {
            tracker.gameMessage(event.getMessage());
            if (start != null)
            {
                for (String fatal : FATAL_MESSAGES)
                {
                    if (event.getMessage().toLowerCase().contains(fatal))
                    {
                        switchOff("Stopped: " + event.getMessage());
                        return;
                    }
                }
            }
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
        if (TitheFarmConfig.GROUP.equals(event.getGroup()) && TitheFarmConfig.ENABLE_UI.equals(event.getKey()))
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
        Set<Reward> rewards = EnumSet.noneOf(Reward.class);
        if (config.buyCan())
        {
            rewards.add(Reward.GRICOLLERS_CAN);
        }
        if (config.buyOutfit())
        {
            for (Reward reward : Reward.values())
            {
                if (reward.isOutfit())
                {
                    rewards.add(reward);
                }
            }
        }
        if (config.buySeedBox())
        {
            rewards.add(Reward.SEED_BOX);
        }
        if (config.buyHerbSack())
        {
            rewards.add(Reward.HERB_SACK);
        }
        return Settings.builder()
                .seed(seed)
                .plantsPerRun(config.plantsPerRun())
                .drinkBelow(config.drinkBelow())
                .rewards(rewards)
                .pointReserve(config.pointReserve())
                .stopReason(stopReason)
                .build();
    }

    private String problem(FarmState state)
    {
        if (state.getFarmingLevel() < MIN_FARMING)
        {
            return "Tithe Farm needs level " + MIN_FARMING + " Farming";
        }
        if (!state.getWater().hasCan())
        {
            return "no watering can. Bring watering cans or Gricoller's can";
        }
        if (!state.isSpade())
        {
            return "no spade. Bring a spade";
        }
        if (!state.isDibber() && !state.isBarehandedPlanting())
        {
            return "no seed dibber. Bring a seed dibber, or learn barehanded planting from Barbarian Training";
        }
        if (config.seed().resolve(state.getFarmingLevel()) == null)
        {
            return config.seed() + " seeds need a higher Farming level";
        }
        seed = config.seed().resolve(state.getFarmingLevel());
        return null;
    }

    private String stopCondition(FarmState state)
    {
        if (config.maxRuntime() > 0 && Duration.between(start, Instant.now()).toMinutes() >= config.maxRuntime())
        {
            return "the max runtime was reached";
        }
        if (config.pointsTarget() > 0 && state.getPoints() >= config.pointsTarget())
        {
            return config.pointsTarget() + " points were reached";
        }
        if (config.levelTarget() > 0 && state.getFarmingLevel() >= config.levelTarget())
        {
            return "Farming level " + config.levelTarget() + " was reached";
        }
        if (config.maxSacks() > 0 && tracker.getSacks() >= config.maxSacks())
        {
            return tracker.getSacks() + " sacks were filled";
        }
        return null;
    }

    private void step(FarmState state)
    {
        if (handler != null)
        {
            if (stepHandler())
            {
                Action next = planner.next(state, settings());
                if (!next.preempts(action))
                {
                    return;
                }
                abortAction();
            }
            else
            {
                boolean worked = !failed;
                finished();
                if (!worked || start == null)
                {
                    return;
                }
            }
        }
        begin(state);
    }

    private void begin(FarmState state)
    {
        action = planner.next(state, settings());
        if (action.getType() == Action.Type.STOP)
        {
            switchOff(action.getReason());
            return;
        }
        status = action.toString();
        failed = false;
        handler = TitheBuilder.forAction(this, action);
        if (!stepHandler())
        {
            finished();
        }
    }

    private boolean stepHandler()
    {
        ended = false;
        return handler.step() && !ended;
    }

    private void finished()
    {
        if (!failed)
        {
            failures = 0;
        }
        handler = null;
        action = null;
    }

    private void switchOff(String reason)
    {
        Logger.warn(LOG + reason);
        configManager.setConfiguration(TitheFarmConfig.GROUP, TitheFarmConfig.RUN, false);
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
