package com.tonic.vitamta.api;

import com.tonic.data.magic.RuneRequirement;
import com.tonic.data.magic.Spell;
import com.tonic.vitamta.data.Setup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Picks the weapon and shield, from those worn and carried, that can cast a spell for the fewest runes used up per cast.
 */
public final class Castability
{
    /**
     * No item in the slot.
     */
    public static final int NONE = -1;

    /**
     * Counts the runes available for a spell as if the given weapon and shield were worn, staves, tomes and the rune pouch included.
     */
    @FunctionalInterface
    public interface RuneCounter
    {
        /**
         * @param runeId the rune's item id
         * @param weapon the weapon worn, or NONE
         * @param shield the shield worn, or NONE
         * @return the runes available
         */
        int count(int runeId, int weapon, int shield);
    }

    private Castability()
    {
    }

    /**
     * Finds the best way to cast a spell.
     *
     * @param spell the spell
     * @param weapon the weapon worn, or NONE
     * @param shield the shield worn, or NONE
     * @param weapons weapons carried that could be wielded instead
     * @param shields shields carried that could be worn instead
     * @param twoHanded the weapons that leave no room for a shield
     * @param runes the rune counter
     * @return the cheapest castable setup, preferring fewer swaps on a tie, or null if no setup can cast the spell
     */
    public static Setup best(Spell spell, int weapon, int shield, Collection<Integer> weapons, Collection<Integer> shields, Set<Integer> twoHanded, RuneCounter runes)
    {
        List<Integer> weaponOptions = options(weapon, weapons);
        List<Integer> shieldOptions = options(shield, shields);
        Setup best = null;
        int bestSwaps = Integer.MAX_VALUE;
        for (int candidateWeapon : weaponOptions)
        {
            List<Integer> wearable = twoHanded.contains(candidateWeapon) ? Collections.singletonList(NONE) : shieldOptions;
            for (int wornShield : wearable)
            {
                int cost = cost(spell, candidateWeapon, wornShield, runes);
                if (cost < 0)
                {
                    continue;
                }
                int swaps = (candidateWeapon == weapon ? 0 : 1) + (wornShield == shield ? 0 : 1);
                if (best == null || cost < best.getRunesPerCast() || (cost == best.getRunesPerCast() && swaps < bestSwaps))
                {
                    best = new Setup(spell, candidateWeapon, wornShield, cost);
                    bestSwaps = swaps;
                }
            }
        }
        return best;
    }

    /**
     * Counts the runes a cast uses up with a weapon and shield.
     *
     * @param spell the spell
     * @param weapon the weapon, or NONE
     * @param shield the shield, or NONE
     * @param runes the rune counter
     * @return the runes used up per cast, or -1 when the spell cannot be cast
     */
    public static int cost(Spell spell, int weapon, int shield, RuneCounter runes)
    {
        int cost = 0;
        for (RuneRequirement requirement : spell.getRequirements())
        {
            int runeId = requirement.getRune().getRuneId();
            int available = runes.count(runeId, weapon, shield);
            if (available < requirement.getQuantity())
            {
                return -1;
            }
            if (available <= runes.count(runeId, NONE, NONE))
            {
                cost += requirement.getQuantity();
            }
        }
        return cost;
    }

    private static List<Integer> options(int worn, Collection<Integer> carried)
    {
        Set<Integer> options = new LinkedHashSet<>();
        options.add(worn);
        options.addAll(carried);
        return new ArrayList<>(options);
    }
}
