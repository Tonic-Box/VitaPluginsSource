package com.tonic.vitamta;

import com.tonic.vitamta.api.ArenaState;
import com.tonic.vitamta.data.Reward;
import com.tonic.vitamta.data.Room;
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
import java.util.Map;

/**
 * Shows runtime, status, the room, every room's points against the reward target and what the room is doing.
 */
class MtaOverlay extends OverlayPanel
{
    private final MtaPlugin plugin;

    @Inject
    MtaOverlay(MtaPlugin plugin)
    {
        this.plugin = plugin;
        setPosition(OverlayPosition.TOP_LEFT);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (plugin.getTracker() == null)
        {
            return null;
        }
        ArenaState state = plugin.getTracker().getState();
        Instant start = plugin.getStart();
        long millis = start == null ? 0 : Duration.between(start, Instant.now()).toMillis();
        Reward target = plugin.getConfig().rewardTarget();
        Map<Room, Integer> required = target.required(state.getOwned()::contains);

        panelComponent.getChildren().add(TitleComponent.builder().text("Vita MTA").build());
        panelComponent.getChildren().add(line("Runtime", DurationFormatUtils.formatDuration(millis, "HH:mm:ss")));
        panelComponent.getChildren().add(line("Status", plugin.getStatus()));
        panelComponent.getChildren().add(line("Room", state.getRoom() == null ? (state.isInArena() ? "Lobby" : "Outside") : state.getRoom().toString()));
        panelComponent.getChildren().add(line("Target", target.toString()));
        for (Room room : Room.values())
        {
            int points = state.points(room);
            String value = points < 0 ? "?" : String.valueOf(points);
            if (target != Reward.NONE)
            {
                value += " / " + required.get(room);
            }
            panelComponent.getChildren().add(line(room.toString(), value + (state.getSetups().containsKey(room) ? "" : " (can't cast)")));
        }
        String detail = detail(state);
        if (detail != null)
        {
            panelComponent.getChildren().add(line("Detail", detail));
        }
        return super.render(graphics);
    }

    private static String detail(ArenaState state)
    {
        if (state.getRoom() == null)
        {
            return null;
        }
        switch (state.getRoom())
        {
            case ENCHANTING:
                return "Bonus " + (state.getBonusShape() == null ? "?" : state.getBonusShape().toString());
            case ALCHEMIST:
                return "Best " + (state.getBestItem() == null ? "?" : state.getBestItem().toString());
            case TELEKINETIC:
                return state.getMoves().size() + " pulls left";
            default:
                return state.getFruitFromBones() + " fruit in bones";
        }
    }

    private static LineComponent line(String left, String right)
    {
        return LineComponent.builder().left(left).right(right == null ? "" : right).build();
    }
}
