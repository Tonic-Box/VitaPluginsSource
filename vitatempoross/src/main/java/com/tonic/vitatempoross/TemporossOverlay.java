package com.tonic.vitatempoross;

import com.tonic.vitatempoross.api.CoveState;
import com.tonic.vitatempoross.api.CoveTracker;
import com.tonic.vitatempoross.data.Where;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;
import org.apache.commons.lang3.time.DurationFormatUtils;

import javax.inject.Inject;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.time.Duration;
import java.time.Instant;

/**
 * Shows runtime, status, where the player is, the game's meters, points, permits and games played.
 */
class TemporossOverlay extends OverlayPanel
{
    private final TemporossPlugin plugin;

    @Inject
    TemporossOverlay(TemporossPlugin plugin)
    {
        this.plugin = plugin;
        setPosition(OverlayPosition.TOP_LEFT);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        CoveTracker tracker = plugin.getTracker();
        if (tracker == null)
        {
            return null;
        }
        CoveState state = tracker.getState();
        Instant start = plugin.getStart();
        long millis = start == null ? 0 : Duration.between(start, Instant.now()).toMillis();

        panelComponent.getChildren().add(TitleComponent.builder().text("Vita Tempoross").build());
        panelComponent.getChildren().add(line("Runtime", DurationFormatUtils.formatDuration(millis, "HH:mm:ss")));
        panelComponent.getChildren().add(line("Status", plugin.getStatus()));
        panelComponent.getChildren().add(line("Where", state.getWhere().toString() + (state.getSide() == null ? "" : ", " + state.getSide())));
        if (state.getWhere() == Where.GAME)
        {
            panelComponent.getChildren().add(line("Energy", meter(state.getEnergy())));
            panelComponent.getChildren().add(line("Essence", meter(state.getEssence())));
            panelComponent.getChildren().add(line("Storm", meter(state.getStorm())));
            panelComponent.getChildren().add(line("Points", String.valueOf(state.getPoints())));
        }
        panelComponent.getChildren().add(line("Permits", state.getPermits() + " (+" + tracker.getPermitsGained() + ")"));
        panelComponent.getChildren().add(line("Games", tracker.getGamesWon() + " won, " + tracker.getGamesLost() + " lost"));
        return super.render(graphics);
    }

    private static String meter(int percent)
    {
        return percent < 0 ? "?" : percent + "%";
    }

    private static LineComponent line(String left, String right)
    {
        return LineComponent.builder().left(left).right(right == null ? "" : right).build();
    }
}
