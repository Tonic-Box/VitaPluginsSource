package com.tonic.vitaarceuuslibrary;

import com.tonic.vitaarceuuslibrary.api.LibraryTracker;
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
 * Shows runtime, status, the open request, how much of the layout is known and books delivered.
 */
class ArceuusLibraryOverlay extends OverlayPanel
{
    private final ArceuusLibraryPlugin plugin;

    @Inject
    ArceuusLibraryOverlay(ArceuusLibraryPlugin plugin)
    {
        this.plugin = plugin;
        setPosition(OverlayPosition.TOP_LEFT);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        LibraryTracker tracker = plugin.getTracker();
        Instant start = plugin.getStart();
        long millis = start == null ? 0 : Duration.between(start, Instant.now()).toMillis();
        String request = tracker.getRequest() == null ? (tracker.isRequestUnknown() ? "Open, unknown" : "None") : tracker.getRequest().getShortName() + " for " + tracker.getRequester();

        panelComponent.getChildren().add(TitleComponent.builder().text("Vita Arceuus Library").build());
        panelComponent.getChildren().add(line("Runtime", DurationFormatUtils.formatDuration(millis, "HH:mm:ss")));
        panelComponent.getChildren().add(line("Status", plugin.getStatus()));
        panelComponent.getChildren().add(line("Request", request));
        panelComponent.getChildren().add(line("Layout", tracker.getLibrary().getState().toString()));
        panelComponent.getChildren().add(line("Spare books", String.valueOf(Math.max(0, tracker.getHeld().size() - (tracker.getRequest() != null && tracker.getHeld().contains(tracker.getRequest()) ? 1 : 0)))));
        panelComponent.getChildren().add(line("Delivered", String.valueOf(tracker.getDelivered())));
        return super.render(graphics);
    }

    private static LineComponent line(String left, String right)
    {
        return LineComponent.builder().left(left).right(right == null ? "" : right).build();
    }
}
