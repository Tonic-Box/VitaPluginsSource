package com.tonic.vitamta.rooms;

import com.tonic.vitamta.api.ArenaState;
import com.tonic.vitamta.data.Action;
import com.tonic.vitamta.data.EnchantShape;
import com.tonic.vitamta.data.Setup;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.ObjectID;

import java.util.Random;

/**
 * Plays the Enchanting Chamber: dragonstones first, then batches of the current bonus shape, depositing orbs when space runs low.
 */
public final class EnchantingPlanner
{
    static final int DRAGONSTONE_RANGE = 15;
    static final int MIN_BATCH = 2;
    static final int MAX_BATCH = 4;

    private final Random random;
    private int batch;

    /**
     * Creates the planner.
     *
     * @param random the random source for batch sizes
     */
    public EnchantingPlanner(Random random)
    {
        this.random = random;
        this.batch = roll();
    }

    /**
     * Chooses the next action in the room.
     *
     * @param state the game state
     * @param setup the enchant spell and gear
     * @return the action
     */
    public Action next(ArenaState state, Setup setup)
    {
        if (state.count(ItemID.MAGICTRAINING_DRAGONSTONE) > 0)
        {
            return Action.castOnItem(setup.getSpell(), ItemID.MAGICTRAINING_DRAGONSTONE, "Enchanting a dragonstone");
        }
        WorldPoint dragonstone = nearestDragonstone(state);
        if (state.getFreeSlots() > 0 && dragonstone != null)
        {
            return Action.takeDragonstone(dragonstone);
        }
        EnchantShape bonus = state.getBonusShape();
        if (bonus != null && state.count(bonus.getItem()) >= batch)
        {
            return Action.castOnItem(setup.getSpell(), bonus.getItem(), "Enchanting the bonus " + bonus);
        }
        if (state.getFreeSlots() < batch)
        {
            EnchantShape held = held(state, bonus);
            if (held != null)
            {
                return Action.castOnItem(setup.getSpell(), held.getItem(), "Enchanting a " + held);
            }
            if (state.count(ItemID.MAGICTRAINING_ENCHAN_SHAPEORB) > 0)
            {
                return Action.deposit(ObjectID.MAGICTRAINING_ENCHA_HOLE, ItemID.MAGICTRAINING_ENCHAN_SHAPEORB, "Depositing orbs");
            }
            return Action.stop("No inventory space for shapes");
        }
        if (bonus == null)
        {
            return Action.waitFor("Waiting for the bonus shape");
        }
        ArenaState.Spot pile = state.getNearestPiles().get(bonus);
        if (pile == null)
        {
            return Action.waitFor("No " + bonus + " pile in view");
        }
        if (state.count(bonus.getItem()) == 0)
        {
            batch = roll();
        }
        return Action.takeShapes(pile.getTile(), pile.getObjectId(), bonus.getItem());
    }

    private static EnchantShape held(ArenaState state, EnchantShape bonus)
    {
        if (bonus != null && state.count(bonus.getItem()) > 0)
        {
            return bonus;
        }
        for (EnchantShape shape : EnchantShape.values())
        {
            if (state.count(shape.getItem()) > 0)
            {
                return shape;
            }
        }
        return null;
    }

    private static WorldPoint nearestDragonstone(ArenaState state)
    {
        WorldPoint best = null;
        int bestDistance = DRAGONSTONE_RANGE + 1;
        for (WorldPoint tile : state.getDragonstones())
        {
            int distance = state.getPlayer().distanceTo(tile);
            if (distance < bestDistance)
            {
                best = tile;
                bestDistance = distance;
            }
        }
        return best;
    }

    private int roll()
    {
        return MIN_BATCH + random.nextInt(MAX_BATCH - MIN_BATCH + 1);
    }
}
