package com.tonic.vitatithefarm.data;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import net.runelite.api.coords.WorldPoint;

/**
 * The next thing to do, as chosen by the planner, with a priority that decides whether it may cut a running action short.
 */
@Getter
@Builder(access = AccessLevel.PRIVATE)
public final class Action
{
    public static final int STOP = 100;

    public static final int UPKEEP = 50;

    public static final int WORK = 20;

    public static final int TRAVEL = 15;

    public static final int IDLE = 0;

    /**
     * The kinds of action.
     */
    public enum Type
    {
        STOP,
        WAIT,
        STEP,
        WALK_TO_FARM,
        BUY_REWARD,
        WEAR,
        DROP_SEEDS,
        TAKE_SEEDS,
        HUMIDIFY,
        ENTER,
        LEAVE,
        DRINK,
        RUN,
        REFILL,
        PLANT,
        WATER,
        HARVEST,
        CLEAR,
        DEPOSIT
    }

    private final Type type;
    private final int priority;
    private final WorldPoint plot;
    private final WorldPoint tile;
    private final int itemId;
    private final Reward reward;
    private final String reason;

    /**
     * @param type the kind
     * @param priority how urgent it is
     * @param reason what it does, shown as the status
     * @return the action
     */
    public static Action of(Type type, int priority, String reason)
    {
        return builder().type(type).priority(priority).reason(reason).build();
    }

    /**
     * @param type the kind
     * @param priority how urgent it is
     * @param plot the patch centre it acts on, in template coordinates
     * @param reason what it does
     * @return an action on a patch
     */
    public static Action onPlot(Type type, int priority, WorldPoint plot, String reason)
    {
        return builder().type(type).priority(priority).plot(plot).reason(reason).build();
    }

    /**
     * @param type the kind
     * @param priority how urgent it is
     * @param itemId the item it uses
     * @param reason what it does
     * @return an action with an item
     */
    public static Action withItem(Type type, int priority, int itemId, String reason)
    {
        return builder().type(type).priority(priority).itemId(itemId).reason(reason).build();
    }

    /**
     * @param tile where to step, in template coordinates
     * @param reason why
     * @return an action that steps to a tile
     */
    public static Action step(WorldPoint tile, String reason)
    {
        return builder().type(Type.STEP).priority(IDLE).tile(tile).reason(reason).build();
    }

    /**
     * @param reward the reward
     * @return an action that buys a reward
     */
    public static Action buy(Reward reward)
    {
        return builder().type(Type.BUY_REWARD).priority(TRAVEL).reward(reward).reason("Buying " + reward + " for " + reward.getCost() + " points").build();
    }

    /**
     * @param reason why it waits
     * @return an action that waits a tick
     */
    public static Action waiting(String reason)
    {
        return of(Type.WAIT, IDLE, reason);
    }

    /**
     * @param running the action being carried out
     * @return whether this action should cut it short: it is more urgent, and a watering, which takes a tick or two, is only ever cut short by a stop
     */
    public boolean preempts(Action running)
    {
        if (running.type == Type.WATER)
        {
            return type == Type.STOP;
        }
        return priority > running.priority;
    }

    @Override
    public String toString()
    {
        return reason;
    }
}
