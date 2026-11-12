package com.tonic.vitamta.data;

import com.tonic.data.magic.Spell;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import net.runelite.api.coords.WorldPoint;

/**
 * The next thing to do in the arena, as chosen by the planner.
 */
@Getter
@Builder(access = AccessLevel.PRIVATE)
public final class Action
{
    /**
     * The kinds of action.
     */
    public enum Type
    {
        WALK_TO_ARENA,
        BANK_COINS,
        TALK_TO_ENTRANCE_GUARDIAN,
        READ_POINTS,
        EQUIP,
        ENTER_ROOM,
        LEAVE_ROOM,
        BUY_REWARD,
        TAKE_SHAPES,
        TAKE_DRAGONSTONE,
        CAST_ON_ITEM,
        DEPOSIT,
        GRAB_BONES,
        CAST,
        EAT,
        DODGE,
        SEARCH_CUPBOARD,
        TAKE_ITEMS,
        DROP,
        MOVE,
        TELEGRAB,
        NEW_MAZE,
        WAIT,
        STOP
    }

    private final Type type;
    private final Room room;
    private final WorldPoint tile;
    private final int itemId;
    private final int objectId;
    private final int count;
    private final Spell spell;
    private final Setup setup;
    private final Reward reward;
    private final String reason;

    /**
     * @return an action that walks to the arena
     */
    public static Action walkToArena()
    {
        return builder().type(Type.WALK_TO_ARENA).reason("Walking to the Mage Training Arena").build();
    }

    /**
     * @return an action that banks the coins carried, which cannot be taken into the arena
     */
    public static Action bankCoins()
    {
        return builder().type(Type.BANK_COINS).reason("Banking coins at the Emir's Arena").build();
    }

    /**
     * @return an action that introduces the player to the Entrance Guardian
     */
    public static Action talkToEntranceGuardian()
    {
        return builder().type(Type.TALK_TO_ENTRANCE_GUARDIAN).reason("Talking to the Entrance Guardian").build();
    }

    /**
     * @return an action that goes to the lobby to read every room's points
     */
    public static Action readPoints()
    {
        return builder().type(Type.READ_POINTS).reason("Reading the pizazz points in the lobby").build();
    }

    /**
     * Creates an action that wields a weapon and shield.
     *
     * @param setup the weapon and shield
     * @param room the room it is for
     * @return the action
     */
    public static Action equip(Setup setup, Room room)
    {
        return builder().type(Type.EQUIP).setup(setup).room(room).reason("Equipping for " + room).build();
    }

    /**
     * Creates an action that enters a room.
     *
     * @param room the room
     * @return the action
     */
    public static Action enterRoom(Room room)
    {
        return builder().type(Type.ENTER_ROOM).room(room).objectId(room.getDoor()).reason("Entering the " + room).build();
    }

    /**
     * Creates an action that leaves the current room.
     *
     * @param reason why it leaves
     * @return the action
     */
    public static Action leaveRoom(String reason)
    {
        return builder().type(Type.LEAVE_ROOM).reason(reason).build();
    }

    /**
     * Creates an action that buys a reward.
     *
     * @param reward the item to buy
     * @return the action
     */
    public static Action buy(Reward reward)
    {
        return builder().type(Type.BUY_REWARD).reward(reward).reason("Buying " + reward).build();
    }

    /**
     * Creates an action that takes shapes from a pile.
     *
     * @param pile the pile's tile
     * @param objectId the pile's object id
     * @param shape the shape's item id
     * @return the action
     */
    public static Action takeShapes(WorldPoint pile, int objectId, int shape)
    {
        return builder().type(Type.TAKE_SHAPES).tile(pile).objectId(objectId).itemId(shape).reason("Taking shapes").build();
    }

    /**
     * Creates an action that picks up a dragonstone.
     *
     * @param tile where it lies
     * @return the action
     */
    public static Action takeDragonstone(WorldPoint tile)
    {
        return builder().type(Type.TAKE_DRAGONSTONE).tile(tile).reason("Picking up a dragonstone").build();
    }

    /**
     * Creates an action that casts a spell on an inventory item.
     *
     * @param spell the spell
     * @param itemId the item
     * @param reason what it is for
     * @return the action
     */
    public static Action castOnItem(Spell spell, int itemId, String reason)
    {
        return builder().type(Type.CAST_ON_ITEM).spell(spell).itemId(itemId).reason(reason).build();
    }

    /**
     * Creates an action that casts a spell with no target.
     *
     * @param spell the spell
     * @param reason what it is for
     * @return the action
     */
    public static Action cast(Spell spell, String reason)
    {
        return builder().type(Type.CAST).spell(spell).reason(reason).build();
    }

    /**
     * Creates an action that uses a deposit object.
     *
     * @param objectId the object
     * @param itemId the item that leaves the inventory when it is done
     * @param reason what is deposited
     * @return the action
     */
    public static Action deposit(int objectId, int itemId, String reason)
    {
        return builder().type(Type.DEPOSIT).objectId(objectId).itemId(itemId).reason(reason).build();
    }

    /**
     * Creates an action that grabs bones from a pile.
     *
     * @param pile the pile's tile
     * @param objectId the pile's object id
     * @param count how many bones to grab
     * @return the action
     */
    public static Action grabBones(WorldPoint pile, int objectId, int count)
    {
        return builder().type(Type.GRAB_BONES).tile(pile).objectId(objectId).count(count).reason("Grabbing bones").build();
    }

    /**
     * Creates an action that eats food.
     *
     * @param itemId the food
     * @param count how many to eat
     * @return the action
     */
    public static Action eat(int itemId, int count)
    {
        return builder().type(Type.EAT).itemId(itemId).count(count).reason("Eating").build();
    }

    /**
     * Creates an action that steps out of the way of a falling bone.
     *
     * @param tile the tile to step to
     * @return the action
     */
    public static Action dodge(WorldPoint tile)
    {
        return builder().type(Type.DODGE).tile(tile).reason("Dodging a falling bone").build();
    }

    /**
     * Creates an action that searches a cupboard.
     *
     * @param tile the cupboard's tile
     * @param objectId the cupboard's object id
     * @return the action
     */
    public static Action searchCupboard(WorldPoint tile, int objectId)
    {
        return builder().type(Type.SEARCH_CUPBOARD).tile(tile).objectId(objectId).reason("Searching a cupboard").build();
    }

    /**
     * Creates an action that takes five items from a cupboard.
     *
     * @param tile the cupboard's tile
     * @param objectId the cupboard's object id
     * @param itemId the item it holds
     * @return the action
     */
    public static Action takeItems(WorldPoint tile, int objectId, int itemId)
    {
        return builder().type(Type.TAKE_ITEMS).tile(tile).objectId(objectId).itemId(itemId).reason("Taking items to alchemise").build();
    }

    /**
     * Creates an action that drops every copy of an item.
     *
     * @param itemId the item
     * @return the action
     */
    public static Action drop(int itemId)
    {
        return builder().type(Type.DROP).itemId(itemId).reason("Dropping items to make room").build();
    }

    /**
     * Creates an action that walks to a tile inside a room.
     *
     * @param tile the tile
     * @param reason what for
     * @return the action
     */
    public static Action move(WorldPoint tile, String reason)
    {
        return builder().type(Type.MOVE).tile(tile).reason(reason).build();
    }

    /**
     * @return an action that casts Telekinetic Grab on the maze guardian
     */
    public static Action telegrab()
    {
        return builder().type(Type.TELEGRAB).reason("Moving the maze guardian").build();
    }

    /**
     * @return an action that asks the maze guardian for a new maze
     */
    public static Action newMaze()
    {
        return builder().type(Type.NEW_MAZE).reason("Starting a new maze").build();
    }

    /**
     * Creates an action that waits a moment.
     *
     * @param reason why it waits
     * @return the action
     */
    public static Action waitFor(String reason)
    {
        return builder().type(Type.WAIT).reason(reason).build();
    }

    /**
     * Creates an action that switches Run off.
     *
     * @param reason why it stops
     * @return the action
     */
    public static Action stop(String reason)
    {
        return builder().type(Type.STOP).reason(reason).build();
    }

    @Override
    public String toString()
    {
        return reason;
    }
}
