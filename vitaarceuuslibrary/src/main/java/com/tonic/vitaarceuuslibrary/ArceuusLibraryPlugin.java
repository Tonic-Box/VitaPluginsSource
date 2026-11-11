package com.tonic.vitaarceuuslibrary;

import com.google.inject.Provides;
import com.tonic.util.handler.StepHandler;
import com.tonic.vitaarceuuslibrary.api.LibraryAPI;
import com.tonic.vitaarceuuslibrary.api.LibraryBuilder;
import com.tonic.vitaarceuuslibrary.api.LibraryMap;
import com.tonic.vitaarceuuslibrary.api.LibraryTracker;
import com.tonic.vitaarceuuslibrary.api.Planner;
import com.tonic.vitaarceuuslibrary.data.Action;
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
import java.time.Instant;

/**
 * Plays the Arceuus Library: asks the customers for books, finds them with the layout solver and reads the rewards, one action per game tick through the handler system.
 */
@PluginDescriptor(
        name = "Vita Arceuus Library",
        description = "Finds the books the Arceuus Library customers ask for and reads the rewards",
        tags = {"vita", "arceuus", "kourend", "library", "magic", "runecraft"},
        enabledByDefault = false
)
public class ArceuusLibraryPlugin extends Plugin
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
    private ArceuusLibraryOverlay overlay;

    @Inject
    @Getter
    private ArceuusLibraryConfig config;

    @Getter
    private final LibraryTracker tracker = new LibraryTracker();

    @Getter
    private final LibraryMap map = new LibraryMap();

    private final Planner planner = new Planner(map);

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
    ArceuusLibraryConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(ArceuusLibraryConfig.class);
    }

    @Override
    protected void startUp()
    {
        configManager.setConfiguration(ArceuusLibraryConfig.GROUP, ArceuusLibraryConfig.RUN, false);
        stop(STOPPED);
        tracker.clear();
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

    @Subscribe
    public void onGameTick(GameTick event)
    {
        if (client.getGameState() != GameState.LOGGED_IN || client.getLocalPlayer() == null)
        {
            return;
        }
        track();
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
        }
        step();
    }

    @Subscribe
    public void onChatMessage(ChatMessage event)
    {
        if (event.getType() == ChatMessageType.GAMEMESSAGE || event.getType() == ChatMessageType.SPAM)
        {
            tracker.gameMessage(event.getMessage());
        }
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        if (event.getGameState() == GameState.HOPPING || event.getGameState() == GameState.LOGIN_SCREEN)
        {
            tracker.worldChanged();
            abortAction();
        }
        else if (event.getGameState() == GameState.LOADING)
        {
            map.clear();
        }
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if (ArceuusLibraryConfig.GROUP.equals(event.getGroup()) && ArceuusLibraryConfig.ENABLE_UI.equals(event.getKey()))
        {
            overlayManager.remove(overlay);
            if (config.enableUI())
            {
                overlayManager.add(overlay);
            }
        }
    }

    private void track()
    {
        tracker.inventory(LibraryAPI.inventory());
        String text = LibraryAPI.npcText();
        if (text == null)
        {
            tracker.dialogueClosed();
        }
        else
        {
            tracker.dialogue(LibraryAPI.speaker(), text);
        }
        int item = LibraryAPI.itemBoxItem();
        if (item > 0)
        {
            tracker.found(item);
        }
    }

    private void step()
    {
        if (handler == null)
        {
            action = planner.next(LibraryAPI.playerTile(), tracker, LibraryAPI.customerTiles(), config.efficientMode(), config.maxDetour(), config.restockAt(), config.readMode());
            status = action.toString();
            failed = false;
            handler = LibraryBuilder.forAction(this, action);
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
        configManager.setConfiguration(ArceuusLibraryConfig.GROUP, ArceuusLibraryConfig.RUN, false);
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
        if (action != null && action.getType() == Action.Type.SEARCH)
        {
            tracker.cancelSearch();
        }
        handler = null;
        action = null;
    }
}
