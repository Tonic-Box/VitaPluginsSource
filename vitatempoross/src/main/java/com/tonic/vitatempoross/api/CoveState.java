package com.tonic.vitatempoross.api;

import com.tonic.vitatempoross.data.Harpoon;
import com.tonic.vitatempoross.data.Side;
import com.tonic.vitatempoross.data.Where;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.coords.WorldPoint;

import java.util.ArrayList;
import java.util.List;

/**
 * Everything the planner needs to know about one game tick, read by the tracker so planning holds no client references.
 */
@Getter
@Setter
public final class CoveState
{
    private Where where = Where.ELSEWHERE;
    private int world;
    private WorldPoint player;
    private boolean animating;
    private boolean moving;

    private int energy = -1;
    private int essence = -1;
    private int storm = -1;
    private int points;
    private int permits;
    private boolean tethered;
    private boolean waveActive;
    private boolean submerged;

    private int raw;
    private int cooked;
    private int crystal;
    private int emptyBuckets;
    private int fullBuckets;
    private int freeSlots;
    private boolean rope;
    private boolean hammer;
    private boolean net;
    private boolean spiritOutfit;
    private boolean bankOpen;
    private final List<Integer> extras = new ArrayList<>();

    private Harpoon harpoon;
    private boolean harpoonWielded;
    private int specEnergy;
    private int fishingLevel;
    private int fishingBoosted;

    private Side side;
    private final List<Thing> spots = new ArrayList<>();
    private Thing pool;
    private final List<Thing> crates = new ArrayList<>();
    private Thing shrine;
    private Thing pump;
    private Thing bucketCrate;
    private Thing ropeCrate;
    private Thing hammerCrate;
    private final List<Thing> tethers = new ArrayList<>();
    private final List<Thing> fires = new ArrayList<>();
    private final List<Shadow> shadows = new ArrayList<>();
    private Thing leave;
    private WorldPoint ship;
    private WorldPoint island;
    private final List<WorldPoint> walkable = new ArrayList<>();

    private Thing ladder;
    private Thing bankChest;
    private Thing rewardPool;
    private Thing angler;

    /**
     * @return the harpoonfish held, raw, cooked and crystallised
     */
    public int fish()
    {
        return raw + cooked + crystal;
    }

    /**
     * @return the empty and full buckets held
     */
    public int buckets()
    {
        return emptyBuckets + fullBuckets;
    }
}
