package com.tonic.vitamta.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.ObjectID;

/**
 * The four training rooms: the lobby door into each, where its points show and its pizazz cap.
 */
@Getter
@RequiredArgsConstructor
public enum Room
{
    TELEKINETIC("Telekinetic Theatre", ObjectID.MAGICTRAINING_TELEDOOR, InterfaceID.MagictrainingTele.MAGICTRAIN_TELE_PTS, InterfaceID.MagictrainingMain.MAGICTRAIN_TELE_POINTS, 8000, 2825),
    ALCHEMIST("Alchemists' Playground", ObjectID.MAGICTRAINING_ALCHEMDOOR, InterfaceID.MagictrainingAlchem.MAGICTRAIN_ALCHEM_PTS, InterfaceID.MagictrainingMain.MAGICTRAIN_ALCH_POINTS, 16000, 3275),
    ENCHANTING("Enchanting Chamber", ObjectID.MAGICTRAINING_ENCHANTDOOR, InterfaceID.MagictrainingEncha.MAGICTRAIN_ENCHA_PTS, InterfaceID.MagictrainingMain.MAGICTRAIN_ENCH_POINTS, 32000, 29000),
    GRAVEYARD("Creature Graveyard", ObjectID.MAGICTRAINING_GRAVEDOOR, InterfaceID.MagictrainingGrave.MAGICTRAIN_GRAVE_PTS, InterfaceID.MagictrainingMain.MAGICTRAIN_GRAVE_POINTS, 8000, 2825);

    private final String displayName;
    private final int door;
    private final int roomPointsWidget;
    private final int lobbyPointsWidget;
    private final int cap;
    private final int allItemsCost;

    /**
     * Scales points to Telekinetic Theatre units, so rooms whose shop prices differ can be compared.
     *
     * @param points the room's points
     * @return the points in Telekinetic Theatre units
     */
    public int scaled(int points)
    {
        return (int) ((long) points * TELEKINETIC.allItemsCost / allItemsCost);
    }

    @Override
    public String toString()
    {
        return displayName;
    }
}
