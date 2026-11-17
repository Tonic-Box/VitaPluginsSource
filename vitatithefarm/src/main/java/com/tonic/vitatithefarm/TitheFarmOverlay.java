package com.tonic.vitatithefarm;

import com.tonic.vitatithefarm.api.FarmState;
import com.tonic.vitatithefarm.api.FarmTracker;
import com.tonic.vitatithefarm.api.Planner;
import com.tonic.vitatithefarm.api.Water;
import com.tonic.vitatithefarm.data.Condition;
import com.tonic.vitatithefarm.data.RefillMethod;
import com.tonic.vitatithefarm.data.Where;
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
 * Shows runtime, status, the seed, the plants per run, water, points, the sack and the patches while the plugin runs.
 */
class TitheFarmOverlay extends OverlayPanel
{
    private final TitheFarmPlugin plugin;

    @Inject
    TitheFarmOverlay(TitheFarmPlugin plugin)
    {
        this.plugin = plugin;
        setPosition(OverlayPosition.TOP_LEFT);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!plugin.getConfig().run())
        {
            return null;
        }

        FarmTracker tracker = plugin.getTracker();
        if (tracker == null)
        {
            return null;
        }
        FarmState state = tracker.getState();
        Planner planner = plugin.getPlanner();
        Instant start = plugin.getStart();
        long millis = start == null ? 0 : Duration.between(start, Instant.now()).toMillis();
        Water water = state.getWater();
        RefillMethod refill = state.isHumidify() ? RefillMethod.HUMIDIFY : RefillMethod.BARREL;

        panelComponent.getChildren().add(TitleComponent.builder().text("Vita Tithe Farm").build());
        panelComponent.getChildren().add(line("Runtime", DurationFormatUtils.formatDuration(millis, "HH:mm:ss")));
        panelComponent.getChildren().add(line("Status", plugin.getStatus()));
        panelComponent.getChildren().add(line("Where", state.getWhere().toString()));
        panelComponent.getChildren().add(line("Seed", plugin.getSeed() == null ? "" : plugin.getSeed().toString()));
        String run = planner.getPhase() + (planner.getRunSize() == 0 ? "" : ", patch " + Math.min(planner.getIndex() + 1, planner.getRunSize()) + " of " + planner.getRunSize());
        if (planner.getCapReason() != null)
        {
            run += " (" + planner.getCapReason() + ")";
        }
        panelComponent.getChildren().add(line("Run", run));
        panelComponent.getChildren().add(line("Water", water.getCharges() + " / " + water.getCapacity() + ", refill by " + refill));
        panelComponent.getChildren().add(line("Points", state.getPoints() + " (+" + tracker.getPointsGained() + ")"));
        panelComponent.getChildren().add(line("Sack", state.getScore() % 100 + " / 100, " + tracker.getSacks() + " filled"));
        if (state.getWhere() == Where.FARM)
        {
            panelComponent.getChildren().add(line("Patches", state.count(Condition.DRY) + " dry, " + state.count(Condition.WET) + " wet, " + state.count(Condition.GROWN) + " grown, " + state.count(Condition.DEAD) + " dead"));
        }
        if (plugin.getStopReason() != null)
        {
            panelComponent.getChildren().add(line("Stopping", plugin.getStopReason()));
        }
        return super.render(graphics);
    }

    private static LineComponent line(String left, String right)
    {
        return LineComponent.builder().left(left).right(right == null ? "" : right).build();
    }
}
