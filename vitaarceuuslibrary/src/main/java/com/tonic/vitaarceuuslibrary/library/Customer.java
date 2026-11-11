package com.tonic.vitaarceuuslibrary.library;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.NpcID;

/**
 * The Arceuus Library customers who ask for books, with where they usually stand on the ground floor.
 */
@Getter
@RequiredArgsConstructor
public enum Customer
{
    VILLIA(NpcID.ARCEUUS_LIBRARY_CUSTOMER_2, "Villia", new WorldPoint(1627, 3815, 0)),
    PROFESSOR_GRACKLEBONE(NpcID.ARCEUUS_LIBRARY_CUSTOMER_3, "Professor Gracklebone", new WorldPoint(1627, 3801, 0)),
    SAM(NpcID.ARCEUUS_LIBRARY_CUSTOMER_4, "Sam", new WorldPoint(1640, 3802, 0));

    private final int npcId;
    private final String displayName;
    private final WorldPoint home;

    /**
     * Gets the customer with an NPC id.
     *
     * @param npcId the NPC id
     * @return the customer, or null if the NPC is not a library customer
     */
    public static Customer byNpcId(int npcId)
    {
        for (Customer customer : values())
        {
            if (customer.npcId == npcId)
            {
                return customer;
            }
        }
        return null;
    }

    @Override
    public String toString()
    {
        return displayName;
    }
}
