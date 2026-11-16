package com.tonic.vitatempoross.api;

import com.tonic.vitatempoross.data.Action;
import com.tonic.vitatempoross.data.Strategy;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;

import java.util.List;

/**
 * Chooses the next action from a CoveState: travel, world, rewards and banking outside the cove, then hazards, upkeep and the fishing plan inside it.
 */
public final class Planner
{
    /**
     * The mass Tempoross world games are played on.
     */
    public static final int WORLD = 422;

    private static final int RESERVE_SLOTS = 4;
    private static final int RAW_SLOTS = 20;
    private static final int LOOT_SLOTS = 3;
    private static final int LOW_ENERGY_FISH = 4;
    private static final int POOL_FULL = 97;
    private static final int STRIKE_SOON = 3;
    private static final int SHADOW_AVOID = 8;
    private static final int STATION_FIRE = 4;
    private static final int FIRE_REACH = 10;
    private static final int REPAIR_REACH = 12;
    private static final int COOK_FIRST = 3;
    private static final int ARRIVED = 3;

    private boolean collecting;
    private boolean bankLoot;

    /**
     * Forgets the reward run in progress.
     */
    public void reset()
    {
        collecting = false;
        bankLoot = false;
    }

    /**
     * Chooses the next action.
     *
     * @param state the tick's state
     * @param settings the settings
     * @return the action
     */
    public Action next(CoveState state, Settings settings)
    {
        switch (state.getWhere())
        {
            case GAME_OVER:
                return state.getLeave() == null ? Action.waiting("Waiting to leave", Action.LEAVE) : Action.of(Action.Type.LEAVE, Action.LEAVE, state.getLeave(), "Leaving the cove");
            case GAME:
                return inGame(state, settings);
            case LOBBY_BOAT:
                return Action.waiting("Waiting for the boat to leave", Action.IDLE);
            case UNKAH:
                return atUnkah(state, settings);
            default:
                return Action.of(Action.Type.WALK_TO_DOCK, Action.IDLE, null, "Walking to the Tempoross dock");
        }
    }

    private Action atUnkah(CoveState state, Settings settings)
    {
        if (state.getWorld() != WORLD)
        {
            return Action.hop(WORLD);
        }
        if (settings.getCollectAtPermits() > 0 && state.getPermits() >= settings.getCollectAtPermits())
        {
            collecting = true;
        }
        if (collecting && state.getPermits() <= 0)
        {
            collecting = false;
            bankLoot = true;
        }
        if (collecting)
        {
            if (!state.isNet())
            {
                return state.getAngler() == null ? Action.waiting("Looking for the Spirit Angler", Action.IDLE) : Action.of(Action.Type.TAKE_NET, Action.IDLE, state.getAngler(), "Taking a fishing net");
            }
            if (state.getFreeSlots() < LOOT_SLOTS && !state.getExtras().isEmpty())
            {
                return Action.bank("Banking reward loot");
            }
            return state.getRewardPool() == null ? Action.waiting("Looking for the reward pool", Action.IDLE) : Action.of(Action.Type.BIG_SEARCH, Action.IDLE, state.getRewardPool(), "Collecting rewards, " + state.getPermits() + " permits left");
        }
        if (state.getExtras().isEmpty())
        {
            bankLoot = false;
        }
        if (!state.getExtras().isEmpty() && (bankLoot || state.getFreeSlots() < slotsNeeded(state, settings)))
        {
            return Action.bank(bankLoot ? "Banking reward loot" : "Banking items to make room for fish");
        }
        if (state.getLadder() == null)
        {
            return Action.of(Action.Type.WALK_TO_DOCK, Action.IDLE, null, "Walking to the boat");
        }
        return Action.of(Action.Type.CLIMB_LADDER, Action.IDLE, state.getLadder(), "Boarding the boat");
    }

    private Action inGame(CoveState state, Settings settings)
    {
        WorldPoint player = state.getPlayer();
        if (state.getLeave() != null)
        {
            return Action.of(Action.Type.LEAVE, Action.LEAVE, state.getLeave(), "Leaving the cove");
        }
        if (state.isWaveActive())
        {
            Action wave = wave(state, player);
            if (wave != null)
            {
                return wave;
            }
        }
        Thing fire = fireAt(state, player);
        if (fire != null)
        {
            if (state.getFullBuckets() > 0)
            {
                return Action.of(Action.Type.DOUSE, Action.FIRE, fire, "Putting out the fire underfoot");
            }
            WorldPoint safe = safeTile(state);
            if (safe != null)
            {
                return Action.move(safe, Action.FIRE, "Stepping out of the fire");
            }
        }
        if (threatened(state, player, STRIKE_SOON))
        {
            WorldPoint safe = safeTile(state);
            if (safe != null)
            {
                return Action.move(safe, Action.CLOUD, "Stepping out from under a storm cloud");
            }
        }
        if (state.isTethered() && !state.isWaveActive())
        {
            Thing point = nearestMatching(state.getTethers(), player, false);
            if (point != null && point.distanceTo(player) <= 1)
            {
                return Action.of(Action.Type.UNTETHER, Action.UNTETHER, point, "Untethering after the wave");
            }
        }
        if (state.getSide() == null)
        {
            return Action.waiting("Finding our ship", Action.IDLE);
        }
        if (state.getStorm() < settings.getEmergencyStorm())
        {
            Action upkeep = upkeep(state, settings, player);
            if (upkeep != null)
            {
                return upkeep;
            }
        }
        return work(state, settings, player);
    }

    private Action wave(CoveState state, WorldPoint player)
    {
        if (state.isTethered())
        {
            return Action.waiting("Riding out the wave", Action.WAVE);
        }
        if (!state.isRope() && !state.isSpiritOutfit())
        {
            return null;
        }
        Thing intact = nearestMatching(state.getTethers(), player, false);
        if (intact != null)
        {
            return Action.of(Action.Type.TETHER, Action.WAVE, intact, "Tethering for the wave");
        }
        Thing broken = nearestMatching(state.getTethers(), player, true);
        if (broken != null && state.isHammer())
        {
            return Action.of(Action.Type.REPAIR, Action.WAVE, broken, "Repairing a tether point before the wave");
        }
        return null;
    }

    private Action upkeep(CoveState state, Settings settings, WorldPoint player)
    {
        if (!state.isRope() && !state.isSpiritOutfit() && state.getRopeCrate() != null)
        {
            return Action.takeSupply(state.getRopeCrate(), ItemID.ROPE, 1, "Taking a rope");
        }
        if (settings.isHammer() && !state.isHammer() && state.getHammerCrate() != null)
        {
            return Action.takeSupply(state.getHammerCrate(), ItemID.HAMMER, 1, "Taking a hammer");
        }
        if (state.buckets() < settings.getBuckets() && state.getBucketCrate() != null)
        {
            return Action.takeSupply(state.getBucketCrate(), ItemID.BUCKET_EMPTY, settings.getBuckets(), "Taking buckets");
        }
        if (state.getEmptyBuckets() > 0 && state.getPump() != null)
        {
            return Action.of(Action.Type.FILL_BUCKETS, Action.UPKEEP, state.getPump(), "Filling buckets");
        }
        if (state.isHammer())
        {
            Thing broken = nearestMatching(state.getTethers(), player, true);
            if (broken != null && broken.distanceTo(player) <= REPAIR_REACH)
            {
                return Action.of(Action.Type.REPAIR, Action.UPKEEP, broken, "Repairing a tether point");
            }
        }
        if (state.getFullBuckets() > 0)
        {
            Thing fire = stationFire(state, player);
            if (fire != null)
            {
                return Action.of(Action.Type.DOUSE, Action.UPKEEP, fire, "Putting out a fire");
            }
        }
        return null;
    }

    private Action work(CoveState state, Settings settings, WorldPoint player)
    {
        boolean cook = settings.getStrategy() == Strategy.COOK;
        int fish = state.fish();
        if (state.getPool() != null && (state.getEnergy() < 0 || state.getEnergy() < POOL_FULL))
        {
            return Action.of(Action.Type.HARPOON_POOL, Action.BONUS, state.getPool(), "Harpooning the spirit pool");
        }
        Thing crate = crate(state, player);
        if (fish > 0 && state.getStorm() >= settings.getEmergencyStorm())
        {
            Action load = load(state, crate, Action.EMERGENCY, "Loading everything, the storm is at " + state.getStorm() + "%");
            if (load != null)
            {
                return load;
            }
        }
        if (state.getEnergy() >= 0 && state.getEnergy() <= settings.getLoadBelowEnergy() && fish >= Math.min(LOW_ENERGY_FISH, batch(state, settings)))
        {
            if (cook && state.getRaw() > 0 && state.getRaw() <= COOK_FIRST && state.getShrine() != null)
            {
                return Action.of(Action.Type.COOK, Action.WORK, state.getShrine(), "Cooking the last few fish before loading");
            }
            Action load = load(state, crate, Action.WORK, "Loading, energy is at " + state.getEnergy() + "%");
            if (load != null)
            {
                return load;
            }
        }
        if (state.getFreeSlots() <= 0)
        {
            if (cook && state.getRaw() > 0 && state.getShrine() != null)
            {
                return Action.of(Action.Type.COOK, Action.WORK, state.getShrine(), "Cooking a full inventory");
            }
            Action load = load(state, crate, Action.WORK, "Loading a full inventory");
            if (load != null)
            {
                return load;
            }
        }
        Thing special = doubleSpot(state, player);
        boolean spec = useSpecial(state, settings);
        if (special != null && state.getFreeSlots() > 0)
        {
            return Action.fish(special, Action.BONUS, fish + state.getFreeSlots(), spec, "Fishing the double spot");
        }
        int target = cook ? batch(state, settings) : fish + state.getFreeSlots();
        if (fish < target && state.getFreeSlots() > 0)
        {
            Thing spot = spot(state, player);
            if (spot != null)
            {
                return Action.fish(spot, Action.WORK, target, spec, "Fishing " + fish + " / " + target);
            }
            Action walk = walk(state.getIsland(), player, "Walking to the fishing spots");
            if (walk != null)
            {
                return walk;
            }
        }
        if (cook && state.getRaw() > 0 && state.getShrine() != null)
        {
            return Action.of(Action.Type.COOK, Action.WORK, state.getShrine(), "Cooking " + state.getRaw() + " fish");
        }
        if (fish > 0)
        {
            Action load = load(state, crate, Action.WORK, "Loading " + fish + " fish");
            if (load != null)
            {
                return load;
            }
        }
        return Action.waiting("Waiting for a fishing spot", Action.IDLE);
    }

    private static Action load(CoveState state, Thing crate, int priority, String reason)
    {
        if (crate != null)
        {
            return Action.of(Action.Type.LOAD, priority, crate, reason);
        }
        return walk(state.getShip(), state.getPlayer(), "Walking to the ammunition crates");
    }

    private static Action walk(WorldPoint to, WorldPoint player, String reason)
    {
        if (to == null || player == null || to.distanceTo(player) <= ARRIVED)
        {
            return null;
        }
        return Action.move(to, Action.TRAVEL, reason);
    }

    private static boolean useSpecial(CoveState state, Settings settings)
    {
        return settings.isUseSpecial() && state.getHarpoon() != null && state.isHarpoonWielded() && state.getHarpoon().hasSpecial() && state.getSpecEnergy() >= 100 && state.getHarpoon().specialHelps(state.getFishingBoosted());
    }

    private static int batch(CoveState state, Settings settings)
    {
        if (settings.getFishPerLoad() > 0)
        {
            return settings.getFishPerLoad();
        }
        return state.getFishingLevel() >= 85 ? 9 : 7;
    }

    private static int slotsNeeded(CoveState state, Settings settings)
    {
        int fish = settings.getStrategy() == Strategy.COOK ? batch(state, settings) + RESERVE_SLOTS : RAW_SLOTS;
        int supplies = Math.max(0, settings.getBuckets() - state.buckets());
        if (!state.isRope() && !state.isSpiritOutfit())
        {
            supplies++;
        }
        if (settings.isHammer() && !state.isHammer())
        {
            supplies++;
        }
        return fish + supplies;
    }

    private static Thing fireAt(CoveState state, WorldPoint player)
    {
        if (player == null)
        {
            return null;
        }
        for (Thing fire : state.getFires())
        {
            if (fire.covers(player))
            {
                return fire;
            }
        }
        return null;
    }

    private static Thing stationFire(CoveState state, WorldPoint player)
    {
        Thing best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (Thing fire : state.getFires())
        {
            if (!nearStation(state, fire))
            {
                continue;
            }
            int distance = fire.distanceTo(player);
            if (distance <= FIRE_REACH && distance < bestDistance)
            {
                best = fire;
                bestDistance = distance;
            }
        }
        return best;
    }

    private static boolean nearStation(CoveState state, Thing fire)
    {
        if (state.getShrine() != null && fire.getArea().distanceTo(state.getShrine().getArea()) <= STATION_FIRE)
        {
            return true;
        }
        for (Thing crate : state.getCrates())
        {
            if (fire.getArea().distanceTo(crate.getArea()) <= STATION_FIRE)
            {
                return true;
            }
        }
        for (Thing spot : state.getSpots())
        {
            if (fire.getArea().distanceTo(spot.getArea()) <= STATION_FIRE)
            {
                return true;
            }
        }
        return false;
    }

    private static boolean threatened(CoveState state, WorldPoint point, int within)
    {
        if (point == null)
        {
            return false;
        }
        for (Shadow shadow : state.getShadows())
        {
            if (shadow.threatens(point, within))
            {
                return true;
            }
        }
        return false;
    }

    private static boolean inFire(CoveState state, WorldPoint point)
    {
        for (Thing fire : state.getFires())
        {
            if (fire.covers(point))
            {
                return true;
            }
        }
        return false;
    }

    private static WorldPoint safeTile(CoveState state)
    {
        WorldPoint player = state.getPlayer();
        WorldPoint best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (WorldPoint tile : state.getWalkable())
        {
            if (tile.equals(player) || inFire(state, tile) || threatened(state, tile, Integer.MAX_VALUE))
            {
                continue;
            }
            int distance = player == null ? 0 : tile.distanceTo(player);
            if (distance < bestDistance)
            {
                best = tile;
                bestDistance = distance;
            }
        }
        return best;
    }

    private static Thing crate(CoveState state, WorldPoint player)
    {
        Thing best = null;
        int bestScore = Integer.MAX_VALUE;
        for (Thing crate : state.getCrates())
        {
            int score = crate.distanceTo(player);
            if (crate.isFlagged() || threatenedArea(state, crate))
            {
                score += 1000;
            }
            if (score < bestScore)
            {
                best = crate;
                bestScore = score;
            }
        }
        return best;
    }

    private static boolean threatenedArea(CoveState state, Thing thing)
    {
        for (Shadow shadow : state.getShadows())
        {
            if (shadow.getTicksLeft() <= SHADOW_AVOID && shadow.getArea().distanceTo(thing.getArea()) <= 1)
            {
                return true;
            }
        }
        return false;
    }

    private static Thing doubleSpot(CoveState state, WorldPoint player)
    {
        Thing best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (Thing spot : state.getSpots())
        {
            if (!spot.isFlagged() || !usableSpot(state, spot))
            {
                continue;
            }
            int distance = spot.distanceTo(player);
            if (distance < bestDistance)
            {
                best = spot;
                bestDistance = distance;
            }
        }
        return best;
    }

    private static Thing spot(CoveState state, WorldPoint player)
    {
        Thing best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (Thing spot : state.getSpots())
        {
            if (!usableSpot(state, spot))
            {
                continue;
            }
            int distance = spot.distanceTo(player);
            if (distance < bestDistance)
            {
                best = spot;
                bestDistance = distance;
            }
        }
        return best;
    }

    private static boolean usableSpot(CoveState state, Thing spot)
    {
        if (threatenedArea(state, spot))
        {
            return false;
        }
        if (state.getFullBuckets() > 0)
        {
            return true;
        }
        for (Thing fire : state.getFires())
        {
            if (fire.getArea().distanceTo(spot.getArea()) <= 1)
            {
                return false;
            }
        }
        return true;
    }

    private static Thing nearestMatching(List<Thing> things, WorldPoint player, boolean flagged)
    {
        Thing best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (Thing thing : things)
        {
            if (thing.isFlagged() != flagged)
            {
                continue;
            }
            int distance = player == null ? 0 : thing.distanceTo(player);
            if (distance < bestDistance)
            {
                best = thing;
                bestDistance = distance;
            }
        }
        return best;
    }
}
