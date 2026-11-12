package com.tonic.vitamta.api;

import com.tonic.vitamta.data.Reward;
import com.tonic.vitamta.data.RewardMode;
import com.tonic.vitamta.data.Room;
import com.tonic.vitamta.data.RoomMode;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.Set;

/**
 * The settings the planner works from, copied from the config each tick.
 */
@Value
@Builder
public class Settings
{
    @Singular("enabled")
    Set<Room> enabled;
    RoomMode roomMode;
    Room singleRoom;
    int switchMargin;
    RewardMode rewardMode;
    Reward target;
    boolean skipCappedRooms;
    int eatMinPercent;
    int eatMaxPercent;
    int leaveBelowHitpoints;
}
