package com.tonic.vitatempoross.data;

import com.tonic.vitatempoross.api.Thing;
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
    /**
     * Priority of a leave after the game ends.
     */
    public static final int LEAVE = 100;

    /**
     * Priority of anything done about a wave.
     */
    public static final int WAVE = 90;

    /**
     * Priority of getting out of a fire.
     */
    public static final int FIRE = 80;

    /**
     * Priority of getting out from under a cloud.
     */
    public static final int CLOUD = 70;

    /**
     * Priority of untethering after a wave.
     */
    public static final int UNTETHER = 60;

    /**
     * Priority of loading everything before the storm wins.
     */
    public static final int EMERGENCY = 30;

    /**
     * Priority of a double fishing spot or the spirit pool.
     */
    public static final int BONUS = 25;

    /**
     * Priority of fishing, cooking and loading.
     */
    public static final int WORK = 20;

    /**
     * Priority of walking to where the work is, below the work itself so it stops once a target comes into view.
     */
    public static final int TRAVEL = 15;

    /**
     * Priority of fetching supplies, repairs and fires near the stations.
     */
    public static final int UPKEEP = 10;

    /**
     * Priority of anything outside the cove and of waiting.
     */
    public static final int IDLE = 0;

    /**
     * The kinds of action.
     */
    public enum Type
    {
        WALK_TO_DOCK,
        HOP,
        TAKE_NET,
        BIG_SEARCH,
        BANK,
        CLIMB_LADDER,
        WAIT,
        LEAVE,
        TETHER,
        UNTETHER,
        REPAIR,
        DOUSE,
        MOVE,
        TAKE_SUPPLY,
        FILL_BUCKETS,
        FISH,
        COOK,
        LOAD,
        HARPOON_POOL
    }

    private final Type type;
    private final int priority;
    private final Thing target;
    private final WorldPoint tile;
    private final int itemId;
    private final int count;
    private final boolean special;
    private final String reason;

    /**
     * @param type the kind
     * @param priority how urgent it is
     * @param target what it acts on, or null
     * @param reason what it does, shown as the status
     * @return the action
     */
    public static Action of(Type type, int priority, Thing target, String reason)
    {
        return builder().type(type).priority(priority).target(target).reason(reason).build();
    }

    /**
     * @param reason why it waits
     * @param priority how urgent the wait is
     * @return an action that waits a tick
     */
    public static Action waiting(String reason, int priority)
    {
        return of(Type.WAIT, priority, null, reason);
    }

    /**
     * @param world the world to hop to
     * @return an action that hops worlds
     */
    public static Action hop(int world)
    {
        return builder().type(Type.HOP).priority(IDLE).count(world).reason("Hopping to world " + world).build();
    }

    /**
     * @param reason what is banked
     * @return an action that deposits everything but the kit at the bank chest
     */
    public static Action bank(String reason)
    {
        return builder().type(Type.BANK).priority(IDLE).reason(reason).build();
    }

    /**
     * @param tile where to step
     * @param priority how urgent it is
     * @param reason why
     * @return an action that steps to a tile
     */
    public static Action move(WorldPoint tile, int priority, String reason)
    {
        return builder().type(Type.MOVE).priority(priority).tile(tile).reason(reason).build();
    }

    /**
     * @param crate the supply crate
     * @param itemId the item it gives
     * @param count how many to hold afterwards
     * @param reason what is taken
     * @return an action that takes from a supply crate
     */
    public static Action takeSupply(Thing crate, int itemId, int count, String reason)
    {
        return builder().type(Type.TAKE_SUPPLY).priority(UPKEEP).target(crate).itemId(itemId).count(count).reason(reason).build();
    }

    /**
     * @param spot the fishing spot
     * @param priority how urgent it is
     * @param count the fish to hold before stopping
     * @param special whether to use the harpoon's special attack first
     * @param reason what is fished
     * @return an action that fishes
     */
    public static Action fish(Thing spot, int priority, int count, boolean special, String reason)
    {
        return builder().type(Type.FISH).priority(priority).target(spot).count(count).special(special).reason(reason).build();
    }

    @Override
    public String toString()
    {
        return reason;
    }
}
