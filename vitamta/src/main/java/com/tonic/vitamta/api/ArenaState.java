package com.tonic.vitamta.api;

import com.tonic.vitamta.data.AlchemyItem;
import com.tonic.vitamta.data.EnchantShape;
import com.tonic.vitamta.data.Reward;
import com.tonic.vitamta.data.Room;
import com.tonic.vitamta.data.Setup;
import com.tonic.vitamta.rooms.AlchemistLayout;
import com.tonic.vitamta.rooms.TelekineticSolver;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.coords.Direction;
import net.runelite.api.coords.WorldPoint;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Everything the planner needs to know about one game tick, read from the game by the tracker so planning holds no client references.
 */
@Getter
@Setter
public final class ArenaState
{
    /**
     * An object the player can use, with where it stands.
     */
    @Getter
    public static final class Spot
    {
        private final WorldPoint tile;
        private final int objectId;
        private final int index;

        /**
         * @param tile where it stands
         * @param objectId its object id
         * @param index its place in its group, such as the cupboard number
         */
        public Spot(WorldPoint tile, int objectId, int index)
        {
            this.tile = tile;
            this.objectId = objectId;
            this.index = index;
        }
    }

    private WorldPoint player;
    private boolean inArena;
    private Room room;
    private boolean needsIntroduction;
    private final Map<Room, Integer> points = new EnumMap<>(Room.class);
    private final Map<Room, Setup> setups = new EnumMap<>(Room.class);
    private final Set<Reward> owned = new HashSet<>();
    private int weapon = Castability.NONE;
    private int shield = Castability.NONE;
    private final Map<Integer, Integer> inventory = new HashMap<>();
    private int freeSlots;
    private int hitpoints;
    private int maxHitpoints;
    private boolean animating;
    private boolean moving;

    private EnchantShape bonusShape;
    private final Map<EnchantShape, Spot> nearestPiles = new EnumMap<>(EnchantShape.class);
    private final List<WorldPoint> dragonstones = new ArrayList<>();

    private Spot nearestBonePile;
    private Spot nearestChute;
    private int fruitFromBones;
    private int bonesHeld;
    private WorldPoint safeTile;
    private boolean boneFalling;

    private AlchemyItem bestItem;
    private AlchemistLayout layout;
    private final List<Spot> cupboards = new ArrayList<>();
    private Spot coinCollector;

    private TelekineticSolver maze;
    private boolean guardianPresent;
    private boolean guardianMoving;
    private boolean guardianComplete;
    private boolean telegrabInFlight;
    private final List<Direction> moves = new ArrayList<>();

    /**
     * @param itemId an item
     * @return how many are carried
     */
    public int count(int itemId)
    {
        return inventory.getOrDefault(itemId, 0);
    }

    /**
     * @param room a room
     * @return its points, or -1 when not read yet
     */
    public int points(Room room)
    {
        return points.getOrDefault(room, -1);
    }

    /**
     * @return whether every room's points are known
     */
    public boolean pointsKnown()
    {
        for (Room room : Room.values())
        {
            if (points(room) < 0)
            {
                return false;
            }
        }
        return true;
    }
}
