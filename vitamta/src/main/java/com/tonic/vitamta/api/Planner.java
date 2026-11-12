package com.tonic.vitamta.api;

import com.tonic.vitamta.data.Action;
import com.tonic.vitamta.data.Reward;
import com.tonic.vitamta.data.RewardMode;
import com.tonic.vitamta.data.Room;
import com.tonic.vitamta.data.Setup;
import com.tonic.vitamta.rooms.AlchemistPlanner;
import com.tonic.vitamta.rooms.EnchantingPlanner;
import com.tonic.vitamta.rooms.GraveyardPlanner;
import com.tonic.vitamta.rooms.TelekineticPlanner;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.ObjectID;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Chooses the next action in the arena: get there, read the points, buy or stop at the reward target, pick a room by the room mode, then play that room.
 */
public final class Planner
{
    static final int RECOVER_HITPOINTS = 5;

    private final EnchantingPlanner enchanting;
    private final GraveyardPlanner graveyard;
    private final AlchemistPlanner alchemist = new AlchemistPlanner();
    private final TelekineticPlanner telekinetic = new TelekineticPlanner();
    private boolean coinsChecked;

    /**
     * Creates a planner.
     *
     * @param random the random source for batch sizes and eating thresholds
     */
    public Planner(Random random)
    {
        this.enchanting = new EnchantingPlanner(random);
        this.graveyard = new GraveyardPlanner(random);
    }

    /**
     * Starts a run afresh, so coins carried are banked before anything else.
     */
    public void reset()
    {
        coinsChecked = false;
    }

    /**
     * Chooses the next action.
     *
     * @param state the game state
     * @param settings the settings
     * @return the action
     */
    public Action next(ArenaState state, Settings settings)
    {
        if (!coinsChecked)
        {
            if (state.count(ItemID.COINS) == 0 || state.getRoom() == Room.ALCHEMIST)
            {
                coinsChecked = true;
            }
            else
            {
                return state.getRoom() == null ? Action.bankCoins() : leave(state, "Leaving to bank coins");
            }
        }
        if (!state.isInArena())
        {
            return Action.walkToArena();
        }
        if (state.isNeedsIntroduction())
        {
            return Action.talkToEntranceGuardian();
        }
        if (state.getRoom() == Room.GRAVEYARD && state.isBoneFalling() && state.getSafeTile() != null)
        {
            return Action.dodge(state.getSafeTile());
        }
        if (!state.pointsKnown())
        {
            return state.getRoom() == null ? Action.readPoints() : leave(state, "Leaving to read every room's points");
        }

        Reward target = settings.getTarget() == null ? Reward.NONE : settings.getTarget();
        RewardMode rewardMode = settings.getRewardMode();
        List<Reward> toBuy = target.items(state.getOwned()::contains);
        Map<Room, Integer> required = target.required(state.getOwned()::contains);
        boolean targetSet = target != Reward.NONE && rewardMode != RewardMode.STACK_POINTS;
        boolean reached = targetSet && toBuy.isEmpty() || targetSet && covers(state, required);

        if (targetSet && rewardMode.buys() && !toBuy.isEmpty() && affordable(state, toBuy.get(0)))
        {
            return state.getRoom() == null ? Action.buy(toBuy.get(0)) : leave(state, "Leaving to buy " + toBuy.get(0));
        }
        if (reached && (rewardMode == RewardMode.STOP_AT_TARGET || rewardMode == RewardMode.BUY_AND_STOP && toBuy.isEmpty()))
        {
            return Action.stop("Reward target reached: " + target);
        }

        List<Room> eligible = new ArrayList<>();
        for (Room room : Room.values())
        {
            if (eligible(state, settings, room))
            {
                eligible.add(room);
            }
        }
        if (eligible.isEmpty() && recovering(state, settings))
        {
            return Action.waitFor("Waiting for hitpoints to recover before the " + Room.GRAVEYARD);
        }
        if (eligible.isEmpty())
        {
            return Action.stop("No enabled room can be played with your level, runes and staves");
        }

        boolean active = targetSet && !reached;
        Room goal = choose(state, settings, eligible, active ? required : null);
        if (goal == null)
        {
            return Action.stop("The single room " + settings.getSingleRoom() + " cannot be played");
        }
        Setup setup = state.getSetups().get(goal);
        if (state.getRoom() != goal)
        {
            if (state.getRoom() != null)
            {
                return leave(state, "Leaving for the " + goal);
            }
            if (setup.getWeapon() != state.getWeapon() || setup.getShield() != state.getShield())
            {
                return Action.equip(setup, goal);
            }
            return Action.enterRoom(goal);
        }
        if (setup.getWeapon() != state.getWeapon() || setup.getShield() != state.getShield())
        {
            return Action.equip(setup, goal);
        }
        switch (goal)
        {
            case ENCHANTING:
                return enchanting.next(state, setup);
            case GRAVEYARD:
                return graveyard.next(state, setup, settings);
            case ALCHEMIST:
                return alchemist.next(state, setup);
            default:
                return telekinetic.next(state);
        }
    }

    private Action leave(ArenaState state, String reason)
    {
        if (state.getRoom() == Room.ENCHANTING && state.count(ItemID.MAGICTRAINING_ENCHAN_SHAPEORB) > 0)
        {
            return Action.deposit(ObjectID.MAGICTRAINING_ENCHA_HOLE, ItemID.MAGICTRAINING_ENCHAN_SHAPEORB, "Depositing orbs before leaving");
        }
        if (state.getRoom() == Room.GRAVEYARD && state.getNearestChute() != null && (state.count(ItemID.BANANA) > 0 || state.count(ItemID.PEACH) > 0))
        {
            return Action.deposit(state.getNearestChute().getObjectId(), state.count(ItemID.PEACH) > 0 ? ItemID.PEACH : ItemID.BANANA, "Depositing fruit before leaving");
        }
        return Action.leaveRoom(reason);
    }

    static boolean eligible(ArenaState state, Settings settings, Room room)
    {
        return playable(state, settings, room) && !(room == Room.GRAVEYARD && recovering(state, settings));
    }

    private static boolean playable(ArenaState state, Settings settings, Room room)
    {
        return settings.getEnabled().contains(room) && state.getSetups().get(room) != null && !(settings.isSkipCappedRooms() && state.points(room) >= room.getCap());
    }

    private static boolean recovering(ArenaState state, Settings settings)
    {
        return playable(state, settings, Room.GRAVEYARD) && state.getRoom() != Room.GRAVEYARD && state.getHitpoints() <= settings.getLeaveBelowHitpoints() + RECOVER_HITPOINTS;
    }

    static Room choose(ArenaState state, Settings settings, List<Room> eligible, Map<Room, Integer> required)
    {
        Room current = state.getRoom();
        Map<Room, Integer> shortfall = shortfall(state, eligible, required);
        boolean stay = current != null && shortfall.containsKey(current);
        switch (settings.getRoomMode())
        {
            case SINGLE_ROOM:
                return eligible.contains(settings.getSingleRoom()) ? settings.getSingleRoom() : null;
            case REWARD_TARGET:
                if (stay)
                {
                    return current;
                }
                return shortfall.isEmpty() ? lowest(state, settings, eligible) : mostBehind(shortfall);
            case LOWEST_FIRST:
            default:
                return stay ? current : lowest(state, settings, eligible);
        }
    }

    private static Map<Room, Integer> shortfall(ArenaState state, List<Room> eligible, Map<Room, Integer> required)
    {
        Map<Room, Integer> shortfall = new EnumMap<>(Room.class);
        if (required == null)
        {
            return shortfall;
        }
        for (Room room : eligible)
        {
            int missing = required.get(room) - state.points(room);
            if (missing > 0)
            {
                shortfall.put(room, room.scaled(missing));
            }
        }
        return shortfall;
    }

    private static Room mostBehind(Map<Room, Integer> shortfall)
    {
        Room most = null;
        for (Map.Entry<Room, Integer> entry : shortfall.entrySet())
        {
            if (most == null || entry.getValue() > shortfall.get(most))
            {
                most = entry.getKey();
            }
        }
        return most;
    }

    private static Room lowest(ArenaState state, Settings settings, List<Room> eligible)
    {
        Room current = state.getRoom();
        Room lowest = null;
        for (Room room : eligible)
        {
            if (lowest == null || room.scaled(state.points(room)) < lowest.scaled(state.points(lowest)))
            {
                lowest = room;
            }
        }
        if (current != null && eligible.contains(current) && current.scaled(state.points(current)) - lowest.scaled(state.points(lowest)) < settings.getSwitchMargin())
        {
            return current;
        }
        return lowest;
    }

    private static boolean covers(ArenaState state, Map<Room, Integer> required)
    {
        for (Room room : Room.values())
        {
            if (state.points(room) < required.get(room))
            {
                return false;
            }
        }
        return true;
    }

    private static boolean affordable(ArenaState state, Reward reward)
    {
        for (Room room : Room.values())
        {
            if (state.points(room) < reward.cost(room))
            {
                return false;
            }
        }
        return true;
    }
}
