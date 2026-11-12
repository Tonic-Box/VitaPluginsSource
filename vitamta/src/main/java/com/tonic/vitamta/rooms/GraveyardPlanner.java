package com.tonic.vitamta.rooms;

import com.tonic.data.magic.spellbooks.Standard;
import com.tonic.vitamta.api.ArenaState;
import com.tonic.vitamta.api.Settings;
import com.tonic.vitamta.data.Action;
import com.tonic.vitamta.data.Setup;
import net.runelite.api.gameval.ItemID;

import java.util.Random;

/**
 * Plays the Creature Graveyard: grab bones for a full deposit, turn them into fruit, deposit it, and eat when hitpoints run low.
 */
public final class GraveyardPlanner
{
    static final int BANANA_HEAL = 2;
    static final int PEACH_HEAL = 8;
    static final int MAX_EAT = 6;

    private final Random random;
    private int eatBelowPercent = -1;

    /**
     * Creates the planner.
     *
     * @param random the random source for eating thresholds
     */
    public GraveyardPlanner(Random random)
    {
        this.random = random;
    }

    /**
     * Chooses the next action in the room.
     *
     * @param state the game state
     * @param setup the fruit spell and gear
     * @param settings the eating settings
     * @return the action
     */
    public Action next(ArenaState state, Setup setup, Settings settings)
    {
        if (eatBelowPercent < 0)
        {
            eatBelowPercent = roll(settings);
        }
        int bananas = state.count(ItemID.BANANA);
        int peaches = state.count(ItemID.PEACH);
        int hitpoints = state.getHitpoints();
        int max = Math.max(1, state.getMaxHitpoints());
        if (hitpoints * 100 / max < eatBelowPercent && bananas + peaches > 0)
        {
            eatBelowPercent = roll(settings);
            int food = peaches > 0 ? ItemID.PEACH : ItemID.BANANA;
            int heal = food == ItemID.PEACH ? PEACH_HEAL : BANANA_HEAL;
            int wanted = (max - hitpoints + heal - 1) / heal;
            return Action.eat(food, Math.max(1, Math.min(Math.min(wanted, MAX_EAT), state.count(food))));
        }
        if (hitpoints <= settings.getLeaveBelowHitpoints() && bananas + peaches == 0)
        {
            if (state.getBonesHeld() > 0)
            {
                return Action.cast(setup.getSpell(), "Making fruit to eat");
            }
            return Action.leaveRoom("Hitpoints too low to stay in the graveyard");
        }
        if (bananas + peaches > 0)
        {
            if (state.getNearestChute() == null)
            {
                return Action.waitFor("No food chute in view");
            }
            return Action.deposit(state.getNearestChute().getObjectId(), peaches > 0 ? ItemID.PEACH : ItemID.BANANA, "Depositing fruit");
        }
        boolean usingPeaches = setup.getSpell() == Standard.BONES_TO_PEACHES;
        if (GraveyardPlan.grabMore(state.getFruitFromBones(), state.getBonesHeld(), state.getFreeSlots(), usingPeaches))
        {
            if (state.getNearestBonePile() == null)
            {
                return Action.waitFor("No bone pile in view");
            }
            return Action.grabBones(state.getNearestBonePile().getTile(), state.getNearestBonePile().getObjectId(), 1);
        }
        if (state.getBonesHeld() > 0)
        {
            return Action.cast(setup.getSpell(), usingPeaches ? "Casting Bones to Peaches" : "Casting Bones to Bananas");
        }
        return Action.stop("No inventory space for bones");
    }

    private int roll(Settings settings)
    {
        int low = Math.min(settings.getEatMinPercent(), settings.getEatMaxPercent());
        int high = Math.max(settings.getEatMinPercent(), settings.getEatMaxPercent());
        return low + random.nextInt(high - low + 1);
    }
}
