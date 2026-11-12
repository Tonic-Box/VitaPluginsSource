package com.tonic.vitamta.data;

import com.tonic.data.magic.Spell;
import lombok.Value;

/**
 * A spell with the weapon and shield to cast it with, and how many runes each cast uses up.
 */
@Value
public class Setup
{
    Spell spell;
    int weapon;
    int shield;
    int runesPerCast;
}
