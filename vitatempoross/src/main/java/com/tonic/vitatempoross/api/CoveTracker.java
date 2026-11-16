package com.tonic.vitatempoross.api;

import com.tonic.api.entities.NpcAPI;
import com.tonic.api.entities.TileObjectAPI;
import com.tonic.api.game.CombatAPI;
import com.tonic.api.game.MovementAPI;
import com.tonic.api.game.SceneAPI;
import com.tonic.api.widgets.BankAPI;
import com.tonic.api.widgets.EquipmentAPI;
import com.tonic.api.widgets.InventoryAPI;
import com.tonic.data.wrappers.ItemEx;
import com.tonic.data.wrappers.NpcEx;
import com.tonic.data.wrappers.TileObjectEx;
import com.tonic.vitatempoross.data.Harpoon;
import com.tonic.vitatempoross.data.Side;
import com.tonic.vitatempoross.data.Where;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reads the cove, the Unkah dock and the player each tick into a CoveState, and keeps what lasts between ticks: the ship the player is on, the incoming wave, the clouds' timers and the game counts.
 */
public final class CoveTracker
{
    /**
     * The tile the ferry from Al Kharid lands on, next to the boat.
     */
    public static final WorldPoint DOCK = new WorldPoint(3148, 2842, 0);

    private static final int GAME_REGION = 12076;
    private static final int[] UNKAH_REGIONS = {12332, 12588};
    private static final int SHADOW_TICKS = 16;
    private static final int SHORT_SHADOW_TICKS = 2;
    private static final int ESCAPE_RADIUS = 4;
    private static final int ESCAPE_CHECK_TICKS = 3;
    private static final int CANNON_REACH = 3;
    private static final int[] ALL_HOSTS = {NpcID.TEMPOROSS_INSTANCE_HOST_W, NpcID.TEMPOROSS_INSTANCE_HOST_N, NpcID.TEMPOROSS_INSTANCE_HOST_E, NpcID.TEMPOROSS_INSTANCE_HOST_S};
    private static final int[] VICTORY_HOSTS = {NpcID.TEMPOROSS_INSTANCE_HOST_W_VICTORY, NpcID.TEMPOROSS_INSTANCE_HOST_N_VICTORY, NpcID.TEMPOROSS_INSTANCE_HOST_E_VICTORY, NpcID.TEMPOROSS_INSTANCE_HOST_S_VICTORY};
    private static final int[] DISABLED_CANNONS = {ObjectID.TEMPOROSS_CANNON_1_DISABLED, ObjectID.TEMPOROSS_CANNON_2_DISABLED, ObjectID.TEMPOROSS_CANNON_3_DISABLED, ObjectID.TEMPOROSS_CANNON_4_DISABLED};
    private static final int[] SPOTS = {NpcID.TEMPOROSS_HARPOONFISH_FISHINGSPOT_NORTH, NpcID.TEMPOROSS_HARPOONFISH_FISHINGSPOT_SOUTH, NpcID.TEMPOROSS_HARPOONFISH_FISHINGSPOT_SPECIAL};
    private static final int[] CRATES = {NpcID.TEMPOROSS_NPC_CRATE_AMMUNITION_1, NpcID.TEMPOROSS_NPC_CRATE_AMMUNITION_2, NpcID.TEMPOROSS_NPC_CRATE_AMMUNITION_3, NpcID.TEMPOROSS_NPC_CRATE_AMMUNITION_4};
    private static final int[] MASTS = {ObjectID.TEMPOROSS_MAST_BOTTOM_WEST, ObjectID.TEMPOROSS_MAST_BOTTOM_EAST, ObjectID.TEMPOROSS_MAST_BOTTOM_WEST_BROKEN, ObjectID.TEMPOROSS_MAST_BOTTOM_EAST_BROKEN};
    private static final int[] TOTEMS = {ObjectID.TEMPOROSS_TOTEM_NORTH, ObjectID.TEMPOROSS_TOTEM_SOUTH, ObjectID.TEMPOROSS_TOTEM_NORTH_BROKEN, ObjectID.TEMPOROSS_TOTEM_SOUTH_BROKEN};
    private static final int[] TETHERS = {ObjectID.TEMPOROSS_MAST_BOTTOM_WEST, ObjectID.TEMPOROSS_MAST_BOTTOM_EAST, ObjectID.TEMPOROSS_TOTEM_NORTH, ObjectID.TEMPOROSS_TOTEM_SOUTH};
    private static final int[] BROKEN_TETHERS = {ObjectID.TEMPOROSS_MAST_BOTTOM_WEST_BROKEN, ObjectID.TEMPOROSS_MAST_BOTTOM_EAST_BROKEN, ObjectID.TEMPOROSS_TOTEM_NORTH_BROKEN, ObjectID.TEMPOROSS_TOTEM_SOUTH_BROKEN};
    private static final int[] PUMPS = {ObjectID.TEMPOROSS_WATER_PUMP, ObjectID.TEMPOROSS_WATER_PUMP_DOCK};
    private static final int[] REWARD_POOLS = {ObjectID.TEMPOROSS_REWARDPOOL, ObjectID.TEMPOROSS_REWARDPOOL_0, ObjectID.TEMPOROSS_REWARDPOOL_1, ObjectID.TEMPOROSS_REWARDPOOL_2, ObjectID.TEMPOROSS_REWARDPOOL_3, ObjectID.TEMPOROSS_REWARDPOOL_4, ObjectID.TEMPOROSS_REWARDPOOL_5, ObjectID.TEMPOROSS_REWARDPOOL_6, ObjectID.TEMPOROSS_REWARDPOOL_7, ObjectID.TEMPOROSS_REWARDPOOL_8};
    private static final int[] SPIRIT_OUTFIT = {ItemID.SPIRIT_ANGLER_HAT, ItemID.SPIRIT_ANGLER_TOP, ItemID.SPIRIT_ANGLER_LEGS, ItemID.SPIRIT_ANGLER_BOOTS};
    private static final int[] HAMMERS = {ItemID.HAMMER, ItemID.IMCANDO_HAMMER, ItemID.IMCANDO_HAMMER_OFFHAND};
    private static final int[] SUPPLY_KIT = {ItemID.ROPE, ItemID.HAMMER, ItemID.IMCANDO_HAMMER, ItemID.IMCANDO_HAMMER_OFFHAND, ItemID.BUCKET_EMPTY, ItemID.BUCKET_WATER, ItemID.SPIRIT_FLAKES, ItemID.COINS};

    private final Client client;
    private final WaveWatch wave = new WaveWatch();
    private final Map<String, SeenShadow> shadows = new HashMap<>();
    private final Set<Integer> kit = new HashSet<>();

    @Getter
    private CoveState state = new CoveState();
    @Getter
    private int gamesWon;
    @Getter
    private int gamesLost;
    @Getter
    private int permitsGained;
    private Side side;
    private WorldPoint landing;
    private Where lastWhere;
    private int lastPermits = -1;

    /**
     * Creates a tracker.
     *
     * @param client the client
     */
    public CoveTracker(Client client)
    {
        this.client = client;
        keepNet(false);
    }

    /**
     * Sets whether the fishing net is the player's own and stays out of the bank.
     *
     * @param ownNet whether the player brought a net
     */
    public void keepNet(boolean ownNet)
    {
        kit.clear();
        for (int id : SUPPLY_KIT)
        {
            kit.add(id);
        }
        for (int id : Harpoon.allIds())
        {
            kit.add(id);
        }
        if (ownNet)
        {
            kit.add(ItemID.NET);
        }
    }

    /**
     * Reads a game message.
     *
     * @param message the message text
     */
    public void gameMessage(String message)
    {
        wave.message(message, client.getTickCount());
    }

    /**
     * Notes a cloud shadow appearing.
     *
     * @param object the object that spawned
     */
    public void objectSpawned(TileObject object)
    {
        int id = object.getId();
        if (id != ObjectID.TEMPOROSS_LIGHTNING_SHADOW && id != ObjectID.TEMPOROSS_LIGHTNING_SHADOW_SHORT)
        {
            return;
        }
        WorldArea area = TileObjectEx.of(object).getWorldArea();
        shadows.put(key(object), new SeenShadow(id, area, client.getTickCount()));
    }

    /**
     * Notes a cloud shadow going away.
     *
     * @param object the object that despawned
     */
    public void objectDespawned(TileObject object)
    {
        shadows.remove(key(object));
    }

    /**
     * Forgets everything kept between ticks except the game counts.
     */
    public void clear()
    {
        wave.clear();
        shadows.clear();
        side = null;
        landing = null;
        lastWhere = null;
        lastPermits = -1;
        state = new CoveState();
    }

    /**
     * Reads the tick's state. Runs on the client thread.
     *
     * @return the state
     */
    public CoveState update()
    {
        CoveState next = new CoveState();
        Player local = client.getLocalPlayer();
        if (local == null)
        {
            return state;
        }
        int tick = client.getTickCount();
        WorldPoint player = local.getWorldLocation();
        int region = WorldPoint.fromLocalInstance(client, local.getLocalLocation()).getRegionID();
        next.setWorld(client.getWorld());
        next.setPlayer(player);
        next.setAnimating(local.getAnimation() != -1);
        next.setMoving(MovementAPI.isMoving());
        next.setWhere(where(region, player));
        next.setPermits(client.getVarbitValue(VarbitID.TEMPOROSS_REWARDPERMITS));
        next.setPoints(client.getVarbitValue(VarbitID.TEMPOROSS_CURRENT_POINTS));
        next.setTethered(client.getVarbitValue(VarbitID.TEMPOROSS_TETHERED) > 0);
        next.setFishingLevel(client.getRealSkillLevel(Skill.FISHING));
        next.setFishingBoosted(client.getBoostedSkillLevel(Skill.FISHING));
        next.setSpecEnergy(CombatAPI.getSpecEnergy());
        next.setBankOpen(BankAPI.isOpen());
        readItems(next);
        if (next.getWhere() == Where.GAME || next.getWhere() == Where.GAME_OVER)
        {
            readCove(next, player, tick);
        }
        else
        {
            side = null;
            landing = null;
            wave.clear();
            shadows.clear();
            readUnkah(next);
        }
        count(next);
        state = next;
        return next;
    }

    private Where where(int region, WorldPoint player)
    {
        if (region == GAME_REGION)
        {
            return NpcAPI.search().withIds(VICTORY_HOSTS).first() != null ? Where.GAME_OVER : Where.GAME;
        }
        boolean unkah = false;
        for (int id : UNKAH_REGIONS)
        {
            unkah |= region == id;
        }
        Widget lobby = client.getWidget(InterfaceID.TemporossLobbyHud.UNIVERSE);
        if (lobby != null && !lobby.isHidden())
        {
            return unkah && SceneAPI.isReachable(player, DOCK) ? Where.UNKAH : Where.LOBBY_BOAT;
        }
        return unkah ? Where.UNKAH : Where.ELSEWHERE;
    }

    private void count(CoveState next)
    {
        Where where = next.getWhere();
        boolean inCove = where == Where.GAME || where == Where.GAME_OVER;
        boolean wasInCove = lastWhere == Where.GAME || lastWhere == Where.GAME_OVER;
        if (wasInCove && !inCove)
        {
            if (lastWhere == Where.GAME_OVER)
            {
                gamesWon++;
            }
            else
            {
                gamesLost++;
            }
        }
        if (lastPermits >= 0 && next.getPermits() > lastPermits)
        {
            permitsGained += next.getPermits() - lastPermits;
        }
        lastPermits = next.getPermits();
        lastWhere = where;
    }

    private void readItems(CoveState next)
    {
        next.setRaw(InventoryAPI.count(ItemID.TEMPOROSS_RAW_HARPOONFISH));
        next.setCooked(InventoryAPI.count(ItemID.TEMPOROSS_HARPOONFISH));
        next.setCrystal(InventoryAPI.count(ItemID.TEMPOROSS_CRYSTALLISED_HARPOONFISH));
        next.setEmptyBuckets(InventoryAPI.count(ItemID.BUCKET_EMPTY));
        next.setFullBuckets(InventoryAPI.count(ItemID.BUCKET_WATER));
        next.setFreeSlots(InventoryAPI.getEmptySlots());
        next.setRope(InventoryAPI.count(ItemID.ROPE) > 0);
        next.setNet(InventoryAPI.count(ItemID.NET) > 0);
        Set<Integer> extras = new LinkedHashSet<>();
        Harpoon carried = null;
        boolean hammer = false;
        for (ItemEx item : InventoryAPI.getItems())
        {
            int id = item.getId();
            Harpoon harpoon = Harpoon.of(id);
            if (harpoon != null && (carried == null || harpoon.ordinal() > carried.ordinal()))
            {
                carried = harpoon;
            }
            if (contains(HAMMERS, id))
            {
                hammer = true;
            }
            if (!kit.contains(id))
            {
                extras.add(id);
            }
        }
        next.getExtras().addAll(extras);
        Harpoon wielded = null;
        int outfit = 0;
        for (ItemEx item : EquipmentAPI.getAll())
        {
            int id = item.getId();
            Harpoon harpoon = Harpoon.of(id);
            if (harpoon != null)
            {
                wielded = harpoon;
            }
            if (contains(HAMMERS, id))
            {
                hammer = true;
            }
            if (contains(SPIRIT_OUTFIT, id))
            {
                outfit++;
            }
        }
        next.setHarpoon(wielded != null ? wielded : carried);
        next.setHarpoonWielded(wielded != null);
        next.setHammer(hammer);
        next.setSpiritOutfit(outfit == SPIRIT_OUTFIT.length);
    }

    private void readUnkah(CoveState next)
    {
        if (next.getWhere() != Where.UNKAH)
        {
            return;
        }
        TileObjectEx ladder = TileObjectAPI.search().withId(ObjectID.TEMPOROSS_LOBBY_LADDER).sortNearest().first();
        if (ladder == null)
        {
            ladder = TileObjectAPI.search().withNameContains("ladder").withAnyOfActions("Climb", "Quick-climb").sortNearest().first();
        }
        next.setLadder(object(ladder, false));
        TileObjectEx chest = TileObjectAPI.search().withId(ObjectID.TEMPOROSS_LOBBY_BANK_CHEST).sortNearest().first();
        if (chest == null)
        {
            chest = TileObjectAPI.search().withNameContains("Bank").withAnyOfActions("Use", "Bank").sortNearest().first();
        }
        next.setBankChest(object(chest, false));
        TileObjectEx pool = TileObjectAPI.search().withId(REWARD_POOLS).sortNearest().first();
        if (pool == null)
        {
            pool = TileObjectAPI.search().withNameContains("Reward pool").sortNearest().first();
        }
        next.setRewardPool(object(pool, false));
        next.setAngler(npc(NpcAPI.search().withIds(NpcID.TEMPOROSS_LOBBY_POOL_WATCHER).nearest(), false));
    }

    private void readCove(CoveState next, WorldPoint player, int tick)
    {
        hud(next);
        next.setWaveActive(wave.update(tick, next.isTethered()));
        next.setSubmerged(NpcAPI.search().withIds(NpcID.TEMPOROSS_BOSS_WHIRLPOOL).first() != null);
        List<NpcEx> hosts = NpcAPI.search().withIds(ALL_HOSTS).collect();
        List<NpcEx> victory = NpcAPI.search().withIds(VICTORY_HOSTS).collect();
        if (side == null)
        {
            NpcEx nearest = nearestNpc(hosts, player);
            side = nearest == null ? null : Side.ofHost(nearest.getId());
        }
        next.setSide(side);
        if (!victory.isEmpty())
        {
            next.setLeave(npc(nearestNpc(victory, player), false));
        }
        for (NpcEx fire : NpcAPI.search().withIds(NpcID.TEMPOROSS_FIRE_HITBOX).collect())
        {
            next.getFires().add(npc(fire, false));
        }
        for (SeenShadow seen : shadows.values())
        {
            int life = seen.id == ObjectID.TEMPOROSS_LIGHTNING_SHADOW_SHORT ? SHORT_SHADOW_TICKS : SHADOW_TICKS;
            next.getShadows().add(new Shadow(seen.area, Math.max(0, seen.spawned + life - tick)));
        }
        if (side == null)
        {
            return;
        }
        List<WorldPoint> ours = new ArrayList<>();
        List<WorldPoint> theirs = new ArrayList<>();
        for (NpcEx host : hosts)
        {
            (Side.ofHost(host.getId()) == side ? ours : theirs).add(host.getWorldPoint());
        }
        if (landing == null)
        {
            landing = player;
        }
        TileObjectEx ourMast = nearestObject(TileObjectAPI.search().withId(MASTS).collect(), landing, true);
        TileObjectEx theirMast = nearestObject(TileObjectAPI.search().withId(MASTS).collect(), landing, false);
        if (ourMast != null)
        {
            ours.add(ourMast.getWorldPoint());
            next.setShip(ourMast.getWorldPoint());
            TileObjectEx ourTotem = nearestObject(TileObjectAPI.search().withId(TOTEMS).collect(), ourMast.getWorldPoint(), true);
            if (ourTotem != null)
            {
                ours.add(ourTotem.getWorldPoint());
                next.setIsland(ourTotem.getWorldPoint());
            }
        }
        if (theirMast != null && ourMast != null && !theirMast.getWorldPoint().equals(ourMast.getWorldPoint()))
        {
            theirs.add(theirMast.getWorldPoint());
            TileObjectEx theirTotem = nearestObject(TileObjectAPI.search().withId(TOTEMS).collect(), theirMast.getWorldPoint(), true);
            if (theirTotem != null)
            {
                theirs.add(theirTotem.getWorldPoint());
            }
        }
        if (ours.isEmpty())
        {
            ours.add(player);
        }
        for (NpcEx spot : spots())
        {
            if (ours(spot.getWorldArea(), ours, theirs))
            {
                next.getSpots().add(npc(spot, spot.getId() == NpcID.TEMPOROSS_HARPOONFISH_FISHINGSPOT_SPECIAL));
            }
        }
        for (NpcEx pool : NpcAPI.search().withIds(NpcID.TEMPOROSS_P2_FISHINGSPOT).collect())
        {
            if (ours(pool.getWorldArea(), ours, theirs))
            {
                next.setPool(npc(pool, false));
                break;
            }
        }
        List<TileObjectEx> cannons = TileObjectAPI.search().withId(DISABLED_CANNONS).collect();
        for (NpcEx crate : NpcAPI.search().withIds(CRATES).collect())
        {
            if (!ours(crate.getWorldArea(), ours, theirs))
            {
                continue;
            }
            boolean electrified = false;
            for (TileObjectEx cannon : cannons)
            {
                if (cannon.getWorldArea().distanceTo(crate.getWorldArea()) <= CANNON_REACH)
                {
                    electrified = true;
                    break;
                }
            }
            next.getCrates().add(npc(crate, electrified));
        }
        for (TileObjectEx tether : TileObjectAPI.search().withId(TETHERS).collect())
        {
            if (ours(tether.getWorldArea(), ours, theirs))
            {
                next.getTethers().add(object(tether, false));
            }
        }
        for (TileObjectEx broken : TileObjectAPI.search().withId(BROKEN_TETHERS).collect())
        {
            if (ours(broken.getWorldArea(), ours, theirs))
            {
                next.getTethers().add(object(broken, true));
            }
        }
        next.setShrine(ourObject(new int[]{ObjectID.TEMPOROSS_SHRINE_FIRE}, ours, theirs, player));
        next.setPump(ourObject(PUMPS, ours, theirs, player));
        next.setBucketCrate(ourObject(new int[]{ObjectID.TEMPOROSS_CRATE_BUCKET}, ours, theirs, player));
        next.setRopeCrate(ourObject(new int[]{ObjectID.TEMPOROSS_CRATE_ROPE}, ours, theirs, player));
        next.setHammerCrate(ourObject(new int[]{ObjectID.TEMPOROSS_CRATE_HAMMER}, ours, theirs, player));
        if (inDanger(next, player))
        {
            for (WorldPoint tile : SceneAPI.reachableTiles(player))
            {
                if (tile.distanceTo(player) <= ESCAPE_RADIUS)
                {
                    next.getWalkable().add(tile);
                }
            }
        }
    }

    private static List<NpcEx> spots()
    {
        List<NpcEx> found = NpcAPI.search().withIds(SPOTS).collect();
        for (NpcEx npc : NpcAPI.search().withNameContains("Fishing spot").withAction("Harpoon").collect())
        {
            if (!found.contains(npc))
            {
                found.add(npc);
            }
        }
        return found;
    }

    private static TileObjectEx nearestObject(List<TileObjectEx> objects, WorldPoint from, boolean nearest)
    {
        TileObjectEx best = null;
        int bestDistance = nearest ? Integer.MAX_VALUE : -1;
        for (TileObjectEx object : objects)
        {
            int distance = object.getWorldArea().distanceTo(from);
            if (nearest ? distance < bestDistance : distance > bestDistance)
            {
                best = object;
                bestDistance = distance;
            }
        }
        return best;
    }

    private void hud(CoveState next)
    {
        next.setEnergy(percent(text(InterfaceID.TemporossHud.ENERGY_TITLE)));
        next.setEssence(percent(text(InterfaceID.TemporossHud.ESSENCE_TITLE)));
        next.setStorm(percent(text(InterfaceID.TemporossHud.STORM_INTENSITY_TITLE)));
    }

    private String text(int widgetId)
    {
        Widget widget = client.getWidget(widgetId);
        return widget == null || widget.isHidden() ? null : widget.getText();
    }

    private static boolean inDanger(CoveState next, WorldPoint player)
    {
        for (Thing fire : next.getFires())
        {
            if (fire.covers(player))
            {
                return true;
            }
        }
        for (Shadow shadow : next.getShadows())
        {
            if (shadow.threatens(player, ESCAPE_CHECK_TICKS))
            {
                return true;
            }
        }
        return false;
    }

    private Thing ourObject(int[] ids, List<WorldPoint> ours, List<WorldPoint> theirs, WorldPoint player)
    {
        TileObjectEx best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (TileObjectEx candidate : TileObjectAPI.search().withId(ids).collect())
        {
            WorldArea area = candidate.getWorldArea();
            if (!ours(area, ours, theirs))
            {
                continue;
            }
            int distance = area.distanceTo(player);
            if (distance < bestDistance)
            {
                best = candidate;
                bestDistance = distance;
            }
        }
        return object(best, false);
    }

    private static boolean ours(WorldArea area, List<WorldPoint> ours, List<WorldPoint> theirs)
    {
        return nearest(area, ours) <= nearest(area, theirs);
    }

    private static int nearest(WorldArea area, List<WorldPoint> anchors)
    {
        int best = Integer.MAX_VALUE;
        for (WorldPoint anchor : anchors)
        {
            best = Math.min(best, area.distanceTo(anchor));
        }
        return best;
    }

    private static int percent(String text)
    {
        if (text == null)
        {
            return -1;
        }
        int value = -1;
        for (int i = 0; i < text.length(); i++)
        {
            char c = text.charAt(i);
            if (c >= '0' && c <= '9')
            {
                value = (value < 0 ? 0 : value * 10) + (c - '0');
            }
            else if (value >= 0)
            {
                break;
            }
        }
        return value;
    }

    private static NpcEx nearestNpc(List<NpcEx> npcs, WorldPoint player)
    {
        NpcEx best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (NpcEx npc : npcs)
        {
            int distance = npc.getWorldPoint().distanceTo(player);
            if (distance < bestDistance)
            {
                best = npc;
                bestDistance = distance;
            }
        }
        return best;
    }

    private static Thing npc(NpcEx npc, boolean flagged)
    {
        return npc == null ? null : new Thing(npc.getId(), npc.getWorldArea(), true, flagged);
    }

    private static Thing object(TileObjectEx object, boolean flagged)
    {
        return object == null ? null : new Thing(object.getId(), object.getWorldArea(), false, flagged);
    }

    private static boolean contains(int[] ids, int id)
    {
        for (int value : ids)
        {
            if (value == id)
            {
                return true;
            }
        }
        return false;
    }

    private static String key(TileObject object)
    {
        WorldPoint tile = object.getWorldLocation();
        return object.getId() + ":" + tile.getX() + ":" + tile.getY() + ":" + tile.getPlane();
    }

    private static final class SeenShadow
    {
        private final int id;
        private final WorldArea area;
        private final int spawned;

        private SeenShadow(int id, WorldArea area, int spawned)
        {
            this.id = id;
            this.area = area;
            this.spawned = spawned;
        }
    }
}
