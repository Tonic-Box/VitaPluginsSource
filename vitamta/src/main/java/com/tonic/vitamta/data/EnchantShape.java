package com.tonic.vitamta.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.ObjectID;

/**
 * An Enchanting Chamber shape: the pile it comes from, its item, and the widget shown while it is the bonus shape.
 */
@Getter
@RequiredArgsConstructor
public enum EnchantShape
{
    CUBE("Cube", ObjectID.MAGICTRAINING_ENCHAN_SHAPEPILE1, ItemID.MAGICTRAINING_ENCHAN_CUBE, InterfaceID.MagictrainingEncha.MAGICTRAIN_CUBE),
    CYLINDER("Cylinder", ObjectID.MAGICTRAINING_ENCHAN_SHAPEPILE2, ItemID.MAGICTRAINING_ENCHAN_CYLINDER, InterfaceID.MagictrainingEncha.MAGICTRAIN_CYLINDER),
    ICOSAHEDRON("Icosahedron", ObjectID.MAGICTRAINING_ENCHAN_SHAPEPILE3, ItemID.MAGICTRAINING_ENCHAN_ICOSAHENDRON, InterfaceID.MagictrainingEncha.MAGICTRAIN_ICOSAHEDRON),
    PENTAMID("Pentamid", ObjectID.MAGICTRAINING_ENCHAN_SHAPEPILE4, ItemID.MAGICTRAINING_ENCHAN_PENTAMID, InterfaceID.MagictrainingEncha.MAGICTRAIN_PENTAMID);

    private final String displayName;
    private final int pile;
    private final int item;
    private final int bonusWidget;

    @Override
    public String toString()
    {
        return displayName;
    }
}
