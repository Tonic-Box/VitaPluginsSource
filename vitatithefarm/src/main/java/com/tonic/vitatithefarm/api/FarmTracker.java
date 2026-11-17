package com.tonic.vitatithefarm.api;

import com.tonic.api.game.MovementAPI;
import com.tonic.api.widgets.EquipmentAPI;
import com.tonic.api.widgets.InventoryAPI;
import com.tonic.data.magic.spellbooks.Lunar;
import com.tonic.data.wrappers.ItemEx;
import com.tonic.vitatithefarm.data.FarmMap;
import com.tonic.vitatithefarm.data.PlantState;
import com.tonic.vitatithefarm.data.Reward;
import com.tonic.vitatithefarm.data.Seed;
import com.tonic.vitatithefarm.data.Where;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarbitID;


/**
 * Reads the farm each tick into a state, with each patch keyed by its template tile.
 */
public final class FarmTracker
{
    private static final int[] STAMINA = {ItemID._1DOSESTAMINA, ItemID._2DOSESTAMINA, ItemID._3DOSESTAMINA, ItemID._4DOSESTAMINA};
    private static final int[] SUPER_ENERGY = {ItemID._1DOSE2ENERGY, ItemID._2DOSE2ENERGY, ItemID._3DOSE2ENERGY, ItemID._4DOSE2ENERGY};
    private static final int[] ENERGY = {ItemID._1DOSE1ENERGY, ItemID._2DOSE1ENERGY, ItemID._3DOSE1ENERGY, ItemID._4DOSE1ENERGY};
    private static final int[] FRUIT = {ItemID.HOSIDIUS_TITHE_FRUIT_A, ItemID.HOSIDIUS_TITHE_FRUIT_B, ItemID.HOSIDIUS_TITHE_FRUIT_C};
    private static final int SACK_SIZE = 100;
    private static final int BAREHANDED_PLANTING_LEARNED = 3;

    private final Client client;

    @Getter
    private final FarmCoords coords;

    @Getter
    private FarmState state = new FarmState();

    @Getter
    private int pointsGained;

    @Getter
    private int sacks;

    @Getter
    private String lastMessage;

    @Getter
    private int lastMessageTick = -1;

    private FarmGrid grid;
    private WorldPoint lastPlayer;
    private int lastScore = -1;
    private int lastPoints = -1;

    /**
     * @param client the client
     */
    public FarmTracker(Client client)
    {
        this.client = client;
        this.coords = new FarmCoords(client);
    }

    /**
     * Reads the tick's state. Runs on the client thread.
     *
     * @return the state
     */
    public FarmState update()
    {
        Player local = client.getLocalPlayer();
        if (local == null)
        {
            return state;
        }
        FarmState next = new FarmState();
        int tick = client.getTickCount();
        WorldPoint player = coords.template(local.getLocalLocation());
        next.setTick(tick);
        next.setPlayer(player);
        next.setWhere(where(player));
        next.setAnimating(local.getAnimation() != -1);
        next.setMoving(MovementAPI.isMoving() || player != null && !player.equals(lastPlayer));
        lastPlayer = player;
        next.setRunEnabled(MovementAPI.isRunEnabled());
        next.setEnergy(MovementAPI.getRunEnergy());
        next.setStaminaActive(MovementAPI.staminaInEffect());
        next.setFarmingLevel(client.getRealSkillLevel(Skill.FARMING));
        next.setHerbloreLevel(client.getRealSkillLevel(Skill.HERBLORE));
        next.setScore(client.getVarbitValue(VarbitID.HOSIDIUS_TITHE_SCORE));
        next.setPoints(client.getVarbitValue(VarbitID.HOSIDIUS_TITHE_REWARDPOINTS));
        next.setHumidify(Lunar.HUMIDIFY.canCast());
        readItems(next);
        readRewards(next);
        if (next.getWhere() == Where.FARM)
        {
            readPlots(next);
        }
        else
        {
            coords.clear();
            grid = null;
        }
        count(next);
        state = next;
        return next;
    }

    /**
     * Records a game message, so a failed action can say why.
     *
     * @param message the message
     */
    public void gameMessage(String message)
    {
        lastMessage = message;
        lastMessageTick = client.getTickCount();
    }

    /**
     * Forgets the farm, after a logout or hop where the plants are lost.
     */
    public void clear()
    {
        coords.clear();
        grid = null;
        lastScore = -1;
        lastPoints = -1;
    }

    /**
     * Restarts the session counters.
     */
    public void resetCounters()
    {
        pointsGained = 0;
        sacks = 0;
    }

    private Where where(WorldPoint player)
    {
        if (player == null || player.getRegionID() != FarmMap.REGION)
        {
            return Where.ELSEWHERE;
        }
        if (client.getTopLevelWorldView().isInstance())
        {
            return Where.FARM;
        }
        return FarmMap.distance(player, FarmMap.SEED_TABLE) <= FarmMap.LOBBY_RADIUS ? Where.LOBBY : Where.ELSEWHERE;
    }

    private void readPlots(FarmState next)
    {
        boolean ready = coords.refresh();
        next.setLayoutReady(ready);
        next.setLayoutProblem(coords.getProblem());
        if (!ready)
        {
            grid = null;
            return;
        }
        if (grid == null)
        {
            grid = coords.grid();
        }
        next.setGrid(grid);
        for (WorldPoint key : FarmMap.PATCHES)
        {
            GameObject object = coords.patch(key);
            PlantState plant = object == null ? null : PlantState.decode(object.getId());
            if (plant == null)
            {
                continue;
            }
            next.getPlots().add(new Plot(key, plant));
        }
    }

    private void readItems(FarmState next)
    {
        int charges = 0;
        int capacity = 0;
        int cans = 0;
        boolean gricoller = false;
        boolean emptyVessel = false;
        int refillItem = -1;
        int refillCharges = Integer.MAX_VALUE;
        int fruit = 0;
        for (ItemEx item : InventoryAPI.getItems())
        {
            int id = item.getId();
            int can = canCharges(id);
            if (can >= 0)
            {
                cans++;
                charges += can;
                capacity += Water.CAN_CHARGES;
                emptyVessel |= can == 0;
                if (can < Water.CAN_CHARGES && can < refillCharges)
                {
                    refillItem = id;
                    refillCharges = can;
                }
            }
            else if (id == ItemID.ZEAH_WATERINGCAN)
            {
                gricoller = true;
            }
            Seed seed = Seed.ofSeedItem(id);
            if (seed != null)
            {
                next.setHeldSeed(seed);
                next.setSeeds(next.getSeeds() + item.getQuantity());
            }
            for (int fruitId : FRUIT)
            {
                if (fruitId == id)
                {
                    fruit++;
                }
            }
        }
        if (gricoller)
        {
            int held = client.getVarbitValue(VarbitID.ZEAH_WATERINGCAN_CHARGES);
            charges += held;
            capacity += Water.GRICOLLER_CHARGES;
            if (held < Water.GRICOLLER_CHARGES)
            {
                emptyVessel = true;
                if (refillItem == -1)
                {
                    refillItem = ItemID.ZEAH_WATERINGCAN;
                }
            }
        }
        next.setWater(new Water(charges, capacity, cans, gricoller, emptyVessel, refillItem));
        next.setFruit(fruit);
        next.setFreeSlots(InventoryAPI.getEmptySlots());
        next.setSpade(InventoryAPI.count(ItemID.SPADE) > 0);
        next.setDibber(InventoryAPI.count(ItemID.DIBBER) > 0);
        next.setBarehandedPlanting(client.getVarbitValue(VarbitID.BRUT_FARMING_PLANTING) == BAREHANDED_PLANTING_LEARNED);
        next.setPotion(potion(next.isStaminaActive()));
    }

    private void readRewards(FarmState next)
    {
        for (ItemEx item : InventoryAPI.getItems())
        {
            own(next, item.getId(), true);
        }
        for (ItemEx item : EquipmentAPI.getAll())
        {
            own(next, item.getId(), false);
        }
        ItemContainer bank = client.getItemContainer(InventoryID.BANK);
        if (bank != null)
        {
            for (Item item : bank.getItems())
            {
                own(next, item.getId(), false);
            }
        }
    }

    private static void own(FarmState next, int itemId, boolean carried)
    {
        Reward reward = Reward.of(itemId);
        if (reward == null)
        {
            return;
        }
        next.getOwned().add(reward);
        if (carried && reward.isOutfit())
        {
            next.setOutfitToWear(itemId);
        }
    }

    private void count(FarmState next)
    {
        int score = next.getScore();
        if (lastScore >= 0 && score > lastScore)
        {
            sacks += score / SACK_SIZE - lastScore / SACK_SIZE;
        }
        lastScore = score;
        int points = next.getPoints();
        if (lastPoints >= 0 && points > lastPoints)
        {
            pointsGained += points - lastPoints;
        }
        lastPoints = points;
    }

    private static int potion(boolean staminaActive)
    {
        if (!staminaActive)
        {
            int stamina = first(STAMINA);
            if (stamina != -1)
            {
                return stamina;
            }
        }
        int superEnergy = first(SUPER_ENERGY);
        return superEnergy != -1 ? superEnergy : first(ENERGY);
    }

    private static int first(int[] ids)
    {
        for (int id : ids)
        {
            if (InventoryAPI.count(id) > 0)
            {
                return id;
            }
        }
        return -1;
    }

    /**
     * @param itemId an item id
     * @return the charges in a watering can, or -1 when the item is not one
     */
    public static int canCharges(int itemId)
    {
        if (itemId == ItemID.WATERING_CAN_0)
        {
            return 0;
        }
        if (itemId >= ItemID.WATERING_CAN_1 && itemId <= ItemID.WATERING_CAN_8)
        {
            return itemId - ItemID.WATERING_CAN_1 + 1;
        }
        return -1;
    }
}
