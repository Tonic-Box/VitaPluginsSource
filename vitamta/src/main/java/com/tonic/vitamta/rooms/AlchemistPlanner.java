package com.tonic.vitamta.rooms;

import com.tonic.vitamta.api.ArenaState;
import com.tonic.vitamta.data.Action;
import com.tonic.vitamta.data.AlchemyItem;
import com.tonic.vitamta.data.Setup;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;

/**
 * Plays the Alchemists' Playground: find the item worth 30 coins, take five at a time, alchemise them, and keep leftovers in case their type is worth most after the next shuffle.
 */
public final class AlchemistPlanner
{
    static final int DEPOSIT_COINS_AT = 2000;

    /**
     * Chooses the next action in the room.
     *
     * @param state the game state
     * @param setup the alchemy spell and gear
     * @return the action
     */
    public Action next(ArenaState state, Setup setup)
    {
        if (state.count(ItemID.MAGICTRAINING_COINS) >= DEPOSIT_COINS_AT && state.getCoinCollector() != null)
        {
            return Action.deposit(state.getCoinCollector().getObjectId(), ItemID.MAGICTRAINING_COINS, "Depositing coins");
        }
        AlchemyItem best = state.getBestItem();
        if (best == null)
        {
            return Action.waitFor("Waiting for the item values");
        }
        if (state.count(best.getItemId()) > 0)
        {
            return Action.castOnItem(setup.getSpell(), best.getItemId(), "Alchemising " + best);
        }
        if (state.getCupboards().isEmpty())
        {
            return Action.waitFor("No cupboards in view");
        }
        AlchemistLayout layout = state.getLayout();
        ArenaState.Spot cupboard = layout == null || !layout.isKnown() ? null : spot(state, layout.find(best));
        if (cupboard == null)
        {
            ArenaState.Spot nearest = nearest(state);
            return Action.searchCupboard(nearest.getTile(), nearest.getObjectId());
        }
        if (state.getFreeSlots() == 0)
        {
            AlchemyItem spare = mostHeld(state, best);
            if (spare == null)
            {
                return Action.stop("No inventory space for items");
            }
            return Action.drop(spare.getItemId());
        }
        return Action.takeItems(cupboard.getTile(), cupboard.getObjectId(), best.getItemId());
    }

    private static ArenaState.Spot spot(ArenaState state, int index)
    {
        for (ArenaState.Spot spot : state.getCupboards())
        {
            if (spot.getIndex() == index)
            {
                return spot;
            }
        }
        return null;
    }

    private static ArenaState.Spot nearest(ArenaState state)
    {
        ArenaState.Spot best = null;
        int bestDistance = Integer.MAX_VALUE;
        WorldPoint player = state.getPlayer();
        for (ArenaState.Spot spot : state.getCupboards())
        {
            int distance = player.distanceTo(spot.getTile());
            if (distance < bestDistance)
            {
                best = spot;
                bestDistance = distance;
            }
        }
        return best;
    }

    private static AlchemyItem mostHeld(ArenaState state, AlchemyItem best)
    {
        AlchemyItem most = null;
        for (AlchemyItem item : AlchemyItem.values())
        {
            if (item == AlchemyItem.EMPTY || item == best || state.count(item.getItemId()) == 0)
            {
                continue;
            }
            if (most == null || state.count(item.getItemId()) > state.count(most.getItemId()))
            {
                most = item;
            }
        }
        return most;
    }
}
