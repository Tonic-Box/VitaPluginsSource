package com.tonic.vitamta.api;

import com.tonic.api.entities.NpcAPI;
import com.tonic.api.entities.TileItemAPI;
import com.tonic.api.entities.TileObjectAPI;
import com.tonic.api.game.ClientScriptAPI;
import com.tonic.api.game.MovementAPI;
import com.tonic.api.widgets.WidgetAPI;
import com.tonic.data.magic.Spell;
import com.tonic.data.magic.spellbooks.Standard;
import com.tonic.data.wrappers.NpcEx;
import com.tonic.data.wrappers.TileItemEx;
import com.tonic.data.wrappers.TileObjectEx;
import com.tonic.vitamta.data.AlchemySpell;
import com.tonic.vitamta.data.AlchemyItem;
import com.tonic.vitamta.data.EnchantShape;
import com.tonic.vitamta.data.Reward;
import com.tonic.vitamta.data.Room;
import com.tonic.vitamta.data.Setup;
import com.tonic.vitamta.rooms.AlchemistLayout;
import com.tonic.vitamta.rooms.Mazes;
import com.tonic.vitamta.rooms.TelekineticSolver;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.GraphicsObject;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.NPC;
import net.runelite.api.Projectile;
import net.runelite.api.Skill;
import net.runelite.api.WorldView;
import net.runelite.api.coords.Angle;
import net.runelite.api.coords.Direction;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.gameval.SpotanimID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemEquipmentStats;
import net.runelite.client.game.ItemStats;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reads the arena from the game each tick into an ArenaState, and keeps what has to be remembered between ticks: every room's points, the Alchemists' Playground layout and the rewards bought.
 */
public final class ArenaTracker
{
    /**
     * The lobby around the four room doors, where walking to the arena ends.
     */
    public static final WorldArea LOBBY = new WorldArea(3361, 3316, 5, 5, 0);

    /**
     * Where the Rewards Guardian stands.
     */
    public static final WorldPoint REWARDS = new WorldPoint(3365, 3318, 1);

    /**
     * The tile in front of the Emir's Arena bank chest, where coins are banked.
     */
    public static final WorldPoint BANK = new WorldPoint(3383, 3270, 0);

    /**
     * Where the Entrance Guardian stands.
     */
    public static final WorldPoint ENTRANCE = new WorldPoint(3363, 3304, 0);

    private static final WorldArea BUILDING = new WorldArea(3340, 3290, 50, 45, 0);
    private static final int ROOMS_REGION = 13462;
    private static final int MAZES_REGION = 13463;
    private static final int SCRIPT_RUNE_COUNT = 19;
    private static final int INVENTORY_SIZE = 28;
    private static final int BEST_VALUE = 30;
    private static final int[] BONES = {ItemID.MAGICTRAINING_BONES1, ItemID.MAGICTRAINING_BONES2, ItemID.MAGICTRAINING_BONES3, ItemID.MAGICTRAINING_BONES4};
    private static final int[] BONE_PILES = {ObjectID.MAGICTRAINING_BONES_PILE1, ObjectID.MAGICTRAINING_BONES_PILE2, ObjectID.MAGICTRAINING_BONES_PILE3, ObjectID.MAGICTRAINING_BONES_PILE4};
    private static final int[] CUPBOARDS = {ObjectID.MAGICTRAINING_ALCHEM_CUPBOARD0, ObjectID.MAGICTRAINING_ALCHEM_CUPBOARD0_OPEN, ObjectID.MAGICTRAINING_ALCHEM_CUPBOARD1, ObjectID.MAGICTRAINING_ALCHEM_CUPBOARD1_OPEN, ObjectID.MAGICTRAINING_ALCHEM_CUPBOARD2, ObjectID.MAGICTRAINING_ALCHEM_CUPBOARD2_OPEN, ObjectID.MAGICTRAINING_ALCHEM_CUPBOARD3, ObjectID.MAGICTRAINING_ALCHEM_CUPBOARD3_OPEN, ObjectID.MAGICTRAINING_ALCHEM_CUPBOARD4, ObjectID.MAGICTRAINING_ALCHEM_CUPBOARD4_OPEN, ObjectID.MAGICTRAINING_ALCHEM_CUPBOARD5, ObjectID.MAGICTRAINING_ALCHEM_CUPBOARD5_OPEN};
    private static final int[] ALCHEMY_NAMES = {InterfaceID.MagictrainingAlchem.MAGICTRAIN_ALCHEM_ITEM1, InterfaceID.MagictrainingAlchem.MAGICTRAIN_ALCHEM_ITEM2, InterfaceID.MagictrainingAlchem.MAGICTRAIN_ALCHEM_ITEM3, InterfaceID.MagictrainingAlchem.MAGICTRAIN_ALCHEM_ITEM4, InterfaceID.MagictrainingAlchem.MAGICTRAIN_ALCHEM_ITEM5};
    private static final int[] ALCHEMY_VALUES = {InterfaceID.MagictrainingAlchem.MAGICTRAIN_ALCHEM_COST1, InterfaceID.MagictrainingAlchem.MAGICTRAIN_ALCHEM_COST2, InterfaceID.MagictrainingAlchem.MAGICTRAIN_ALCHEM_COST3, InterfaceID.MagictrainingAlchem.MAGICTRAIN_ALCHEM_COST4, InterfaceID.MagictrainingAlchem.MAGICTRAIN_ALCHEM_COST5};
    private static final Spell[] ENCHANTS = {Standard.LVL_7_ENCHANT, Standard.LVL_6_ENCHANT, Standard.LVL_5_ENCHANT, Standard.LVL_4_ENCHANT, Standard.LVL_3_ENCHANT, Standard.LVL_2_ENCHANT, Standard.LVL_1_ENCHANT};
    private static final String FOUND = "You found:";
    private static final String CUPBOARD_EMPTY = "The cupboard is empty.";
    private static final String INTRODUCE = "Entrance Guardian";

    private final Client client;
    private final ItemManager itemManager;
    private final Map<Room, Integer> points = new EnumMap<>(Room.class);
    private final Map<Room, Setup> setups = new EnumMap<>(Room.class);
    private final AlchemistLayout layout = new AlchemistLayout();
    private final Set<Reward> bought = new HashSet<>();

    @Getter
    private ArenaState state = new ArenaState();
    private AlchemyItem bestItem;
    private int searchingCupboard = -1;
    private boolean needsIntroduction;
    private int gearHash;
    private AlchemySpell alchemySpell;

    /**
     * Creates a tracker.
     *
     * @param client the client
     * @param itemManager the item manager, for which slot an item is worn in
     */
    public ArenaTracker(Client client, ItemManager itemManager)
    {
        this.client = client;
        this.itemManager = itemManager;
    }

    /**
     * Reads the game into a fresh state. Must run on the client thread.
     *
     * @param alchemy the alchemy spell setting
     * @return the state
     */
    public ArenaState update(AlchemySpell alchemy)
    {
        ArenaState next = new ArenaState();
        WorldPoint player = client.getLocalPlayer().getWorldLocation();
        next.setPlayer(player);
        Room room = roomAt(player);
        next.setRoom(room);
        next.setInArena(room != null || inBuilding(player));
        next.setNeedsIntroduction(needsIntroduction);
        next.setAnimating(client.getLocalPlayer().getAnimation() != -1);
        next.setMoving(MovementAPI.isMoving());
        next.setHitpoints(client.getBoostedSkillLevel(Skill.HITPOINTS));
        next.setMaxHitpoints(client.getRealSkillLevel(Skill.HITPOINTS));

        readPoints(room);
        next.getPoints().putAll(points);
        readInventory(next);
        readGear(next, alchemy);
        readOwned(next);

        if (room == Room.ENCHANTING)
        {
            readEnchanting(next);
        }
        else if (room == Room.GRAVEYARD)
        {
            readGraveyard(next);
        }
        else if (room == Room.ALCHEMIST)
        {
            readAlchemist(next);
        }
        else if (room == Room.TELEKINETIC)
        {
            readTelekinetic(next);
        }
        state = next;
        return next;
    }

    /**
     * Reads a game message: cupboard search results and the Entrance Guardian's introduction.
     *
     * @param message the message
     */
    public void gameMessage(String message)
    {
        if (message == null)
        {
            return;
        }
        if (message.contains(INTRODUCE) && message.toLowerCase().contains("talk"))
        {
            needsIntroduction = true;
        }
        if (searchingCupboard < 0)
        {
            return;
        }
        if (message.contains(FOUND))
        {
            AlchemyItem item = AlchemyItem.find(message.replace(FOUND, ""));
            if (item != null)
            {
                layout.found(searchingCupboard, item);
            }
            searchingCupboard = -1;
        }
        else if (message.equals(CUPBOARD_EMPTY))
        {
            layout.found(searchingCupboard, AlchemyItem.EMPTY);
            searchingCupboard = -1;
        }
    }

    /**
     * Marks a cupboard as being searched, so its result places the layout.
     *
     * @param cupboard the cupboard number
     */
    public void searching(int cupboard)
    {
        searchingCupboard = cupboard;
    }

    /**
     * @return whether a cupboard search is still waiting for its result
     */
    public boolean isSearching()
    {
        return searchingCupboard >= 0;
    }

    /**
     * Stops waiting for a cupboard search result.
     */
    public void cancelSearch()
    {
        searchingCupboard = -1;
    }

    /**
     * Records that the Entrance Guardian has introduced the arena.
     */
    public void introduced()
    {
        needsIntroduction = false;
    }

    /**
     * Records that a rewards item was bought.
     *
     * @param reward the item
     */
    public void bought(Reward reward)
    {
        bought.add(reward);
    }

    /**
     * @return the rewards bought, as remembered
     */
    public Set<Reward> getBought()
    {
        return Collections.unmodifiableSet(bought);
    }

    /**
     * Restores the rewards bought in earlier sessions.
     *
     * @param rewards the rewards
     */
    public void restoreBought(Set<Reward> rewards)
    {
        bought.clear();
        bought.addAll(rewards);
    }

    /**
     * Forgets everything read from the game, for a fresh start.
     */
    public void clear()
    {
        points.clear();
        setups.clear();
        layout.reset();
        bestItem = null;
        searchingCupboard = -1;
        needsIntroduction = false;
        gearHash = 0;
        state = new ArenaState();
    }

    /**
     * @param tile a tile
     * @return the room holding it, or null in the lobby or elsewhere
     */
    public Room roomAt(WorldPoint tile)
    {
        if (WidgetAPI.isVisible(InterfaceID.MagictrainingTele.UNIVERSE) || tile.getRegionID() == MAZES_REGION)
        {
            return Room.TELEKINETIC;
        }
        if (tile.getRegionID() != ROOMS_REGION)
        {
            return null;
        }
        switch (tile.getPlane())
        {
            case 0:
                return Room.ENCHANTING;
            case 1:
                return Room.GRAVEYARD;
            case 2:
                return Room.ALCHEMIST;
            default:
                return null;
        }
    }

    private static boolean inBuilding(WorldPoint tile)
    {
        return tile.getX() >= BUILDING.getX() && tile.getX() < BUILDING.getX() + BUILDING.getWidth() && tile.getY() >= BUILDING.getY() && tile.getY() < BUILDING.getY() + BUILDING.getHeight();
    }

    private void readPoints(Room room)
    {
        for (Room each : Room.values())
        {
            Integer value = number(each.getLobbyPointsWidget());
            if (value != null)
            {
                points.put(each, value);
            }
        }
        if (room != null)
        {
            Integer value = number(room.getRoomPointsWidget());
            if (value != null)
            {
                points.put(room, value);
            }
        }
    }

    private static Integer number(int widgetId)
    {
        Widget widget = WidgetAPI.get(widgetId);
        if (widget == null || widget.isHidden() || widget.getText() == null)
        {
            return null;
        }
        String digits = widget.getText().replaceAll("[^0-9]", "");
        return digits.isEmpty() ? null : Integer.parseInt(digits);
    }

    private void readInventory(ArenaState next)
    {
        ItemContainer container = client.getItemContainer(InventoryID.INV);
        int used = 0;
        if (container != null)
        {
            for (Item item : container.getItems())
            {
                if (item.getId() > 0)
                {
                    used++;
                    next.getInventory().merge(item.getId(), item.getQuantity(), Integer::sum);
                }
            }
        }
        next.setFreeSlots(Math.max(0, INVENTORY_SIZE - used));
    }

    private void readGear(ArenaState next, AlchemySpell alchemy)
    {
        ItemContainer worn = client.getItemContainer(InventoryID.WORN);
        int weapon = wornAt(worn, EquipmentInventorySlot.WEAPON);
        int shield = wornAt(worn, EquipmentInventorySlot.SHIELD);
        next.setWeapon(weapon);
        next.setShield(shield);

        List<Integer> weapons = new ArrayList<>();
        List<Integer> shields = new ArrayList<>();
        Set<Integer> twoHanded = new HashSet<>();
        for (Map.Entry<Integer, Integer> entry : next.getInventory().entrySet())
        {
            sort(entry.getKey(), weapons, shields, twoHanded);
        }
        sort(weapon, new ArrayList<>(), new ArrayList<>(), twoHanded);

        int hash = Arrays.hashCode(new int[]{next.getInventory().hashCode(), weapon, shield, client.getBoostedSkillLevel(Skill.MAGIC), client.getVarbitValue(VarbitID.MAGICTRAINING_BONESPEACHES)}) * 31 + alchemy.ordinal();
        if (hash != gearHash || alchemy != alchemySpell)
        {
            gearHash = hash;
            alchemySpell = alchemy;
            setups.clear();
            Castability.RuneCounter runes = this::runeCount;
            setups.put(Room.TELEKINETIC, setup(Standard.TELEKINETIC_GRAB, weapon, shield, weapons, shields, twoHanded, runes));
            setups.put(Room.ALCHEMIST, alchemySetup(alchemy, weapon, shield, weapons, shields, twoHanded, runes));
            setups.put(Room.ENCHANTING, enchantSetup(weapon, shield, weapons, shields, twoHanded, runes));
            setups.put(Room.GRAVEYARD, fruitSetup(weapon, shield, weapons, shields, twoHanded, runes));
        }
        for (Map.Entry<Room, Setup> entry : setups.entrySet())
        {
            if (entry.getValue() != null)
            {
                next.getSetups().put(entry.getKey(), entry.getValue());
            }
        }
    }

    private Setup alchemySetup(AlchemySpell alchemy, int weapon, int shield, List<Integer> weapons, List<Integer> shields, Set<Integer> twoHanded, Castability.RuneCounter runes)
    {
        Setup high = alchemy == AlchemySpell.LOW ? null : setup(Standard.HIGH_LEVEL_ALCHEMY, weapon, shield, weapons, shields, twoHanded, runes);
        if (high != null || alchemy == AlchemySpell.HIGH)
        {
            return high;
        }
        return setup(Standard.LOW_LEVEL_ALCHEMY, weapon, shield, weapons, shields, twoHanded, runes);
    }

    private Setup enchantSetup(int weapon, int shield, List<Integer> weapons, List<Integer> shields, Set<Integer> twoHanded, Castability.RuneCounter runes)
    {
        for (Spell spell : ENCHANTS)
        {
            Setup setup = setup(spell, weapon, shield, weapons, shields, twoHanded, runes);
            if (setup != null)
            {
                return setup;
            }
        }
        return null;
    }

    private Setup fruitSetup(int weapon, int shield, List<Integer> weapons, List<Integer> shields, Set<Integer> twoHanded, Castability.RuneCounter runes)
    {
        if (client.getVarbitValue(VarbitID.MAGICTRAINING_BONESPEACHES) > 0)
        {
            Setup peaches = setup(Standard.BONES_TO_PEACHES, weapon, shield, weapons, shields, twoHanded, runes);
            if (peaches != null)
            {
                return peaches;
            }
        }
        return setup(Standard.BONES_TO_BANANAS, weapon, shield, weapons, shields, twoHanded, runes);
    }

    private Setup setup(Spell spell, int weapon, int shield, List<Integer> weapons, List<Integer> shields, Set<Integer> twoHanded, Castability.RuneCounter runes)
    {
        if (client.getBoostedSkillLevel(Skill.MAGIC) < spell.getLevel())
        {
            return null;
        }
        return Castability.best(spell, weapon, shield, weapons, shields, twoHanded, runes);
    }

    private int runeCount(int runeId, int weapon, int shield)
    {
        ClientScriptAPI.runScript(SCRIPT_RUNE_COUNT, runeId, weapon, shield);
        return client.getIntStack()[client.getIntStackSize() - 1];
    }

    private void sort(int itemId, List<Integer> weapons, List<Integer> shields, Set<Integer> twoHanded)
    {
        if (itemId <= 0)
        {
            return;
        }
        ItemStats stats = itemManager.getItemStats(itemId);
        ItemEquipmentStats equipment = stats == null ? null : stats.getEquipment();
        if (equipment == null)
        {
            return;
        }
        if (equipment.getSlot() == EquipmentInventorySlot.WEAPON.getSlotIdx())
        {
            weapons.add(itemId);
            if (equipment.isTwoHanded())
            {
                twoHanded.add(itemId);
            }
        }
        else if (equipment.getSlot() == EquipmentInventorySlot.SHIELD.getSlotIdx())
        {
            shields.add(itemId);
        }
    }

    private static int wornAt(ItemContainer worn, EquipmentInventorySlot slot)
    {
        if (worn == null)
        {
            return Castability.NONE;
        }
        Item item = worn.getItem(slot.getSlotIdx());
        return item == null || item.getId() <= 0 ? Castability.NONE : item.getId();
    }

    private void readOwned(ArenaState next)
    {
        next.getOwned().addAll(bought);
        ItemContainer worn = client.getItemContainer(InventoryID.WORN);
        for (Reward reward : Reward.values())
        {
            if (!reward.isItem())
            {
                continue;
            }
            if (reward == Reward.BONES_TO_PEACHES)
            {
                if (client.getVarbitValue(VarbitID.MAGICTRAINING_BONESPEACHES) > 0)
                {
                    next.getOwned().add(reward);
                }
                continue;
            }
            if (next.count(reward.getItemId()) > 0 || worn != null && worn.contains(reward.getItemId()))
            {
                next.getOwned().add(reward);
            }
        }
    }

    private void readEnchanting(ArenaState next)
    {
        for (EnchantShape shape : EnchantShape.values())
        {
            if (WidgetAPI.isVisible(shape.getBonusWidget()))
            {
                next.setBonusShape(shape);
            }
            TileObjectEx pile = TileObjectAPI.search().withId(shape.getPile()).sortNearest().first();
            if (pile != null)
            {
                next.getNearestPiles().put(shape, new ArenaState.Spot(pile.getWorldPoint(), pile.getId(), 0));
            }
        }
        for (TileItemEx item : TileItemAPI.search().withId(ItemID.MAGICTRAINING_DRAGONSTONE).collect())
        {
            next.getDragonstones().add(item.getWorldPoint());
        }
    }

    private void readGraveyard(ArenaState next)
    {
        int fruit = 0;
        int bones = 0;
        for (int i = 0; i < BONES.length; i++)
        {
            int count = next.count(BONES[i]);
            bones += count;
            fruit += count * (i + 1);
        }
        next.setBonesHeld(bones);
        next.setFruitFromBones(fruit);
        TileObjectEx pile = TileObjectAPI.search().withId(BONE_PILES).sortNearest().first();
        if (pile != null)
        {
            next.setNearestBonePile(new ArenaState.Spot(pile.getWorldPoint(), pile.getId(), 0));
        }
        TileObjectEx chute = TileObjectAPI.search().withId(ObjectID.MAGICTRAINING_GRAVE_FOODSHUTE).sortNearest().first();
        if (chute != null)
        {
            next.setNearestChute(new ArenaState.Spot(chute.getWorldPoint(), chute.getId(), 0));
        }
        WorldView view = client.getTopLevelWorldView();
        Set<WorldPoint> falling = new HashSet<>();
        for (GraphicsObject graphic : view.getGraphicsObjects())
        {
            if (!graphic.finished())
            {
                falling.add(WorldPoint.fromLocal(client, graphic.getLocation()));
            }
        }
        WorldPoint player = next.getPlayer();
        if (falling.contains(player))
        {
            next.setBoneFalling(true);
            next.setSafeTile(safeTile(view, player, falling));
        }
    }

    private static WorldPoint safeTile(WorldView view, WorldPoint player, Set<WorldPoint> falling)
    {
        int[][] steps = {{0, 1}, {1, 0}, {0, -1}, {-1, 0}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
        WorldArea here = new WorldArea(player, 1, 1);
        for (int[] step : steps)
        {
            WorldPoint tile = player.dx(step[0]).dy(step[1]);
            if (!falling.contains(tile) && here.canTravelInDirection(view, step[0], step[1]))
            {
                return tile;
            }
        }
        return null;
    }

    private void readAlchemist(ArenaState next)
    {
        AlchemyItem best = null;
        for (int i = 0; i < ALCHEMY_NAMES.length; i++)
        {
            Integer value = number(ALCHEMY_VALUES[i]);
            Widget name = WidgetAPI.get(ALCHEMY_NAMES[i]);
            if (value != null && value == BEST_VALUE && name != null)
            {
                best = AlchemyItem.find(name.getText());
            }
        }
        if (best != bestItem)
        {
            bestItem = best;
            layout.reset();
            searchingCupboard = -1;
        }
        next.setBestItem(best);
        next.setLayout(layout);
        for (TileObjectEx cupboard : TileObjectAPI.search().withId(CUPBOARDS).collect())
        {
            int index = Arrays.binarySearch(CUPBOARDS, cupboard.getId()) / 2;
            next.getCupboards().add(new ArenaState.Spot(cupboard.getWorldPoint(), cupboard.getId(), index));
        }
        TileObjectEx collector = TileObjectAPI.search().withId(ObjectID.MAGICTRAINING_COIN_COLLECTOR).sortNearest().first();
        if (collector != null)
        {
            next.setCoinCollector(new ArenaState.Spot(collector.getWorldPoint(), collector.getId(), 0));
        }
    }

    private void readTelekinetic(ArenaState next)
    {
        NpcEx guardianEx = NpcAPI.search().withIds(NpcID.MAGICTRAINING_GUARD_MAZE_INCOMPLETE, NpcID.MAGICTRAINING_GUARD_MAZE_MOVING, NpcID.MAGICTRAINING_GUARD_MAZE_COMPLETE).nearest();
        if (guardianEx == null)
        {
            return;
        }
        NPC guardian = guardianEx.getNpc();
        next.setGuardianPresent(true);
        next.setGuardianMoving(guardian.getId() == NpcID.MAGICTRAINING_GUARD_MAZE_MOVING);
        next.setGuardianComplete(guardian.getId() == NpcID.MAGICTRAINING_GUARD_MAZE_COMPLETE);
        for (Projectile projectile : client.getProjectiles())
        {
            if (projectile.getId() == SpotanimID.TELEGRAB_TRAVEL)
            {
                next.setTelegrabInFlight(true);
            }
        }
        WorldPoint tile = guardian.getWorldLocation();
        WorldArea area = Mazes.areaOf(WorldPoint.fromLocalInstance(client, guardian.getLocalLocation()));
        if (area == null)
        {
            return;
        }
        List<WorldPoint> walls = new ArrayList<>();
        WorldPoint finish = null;
        for (TileObjectEx object : TileObjectAPI.search().withId(ObjectID.MAGICTRAINING_MINIWALL, ObjectID.MAGICTRAINING_MINI_STATUE_PITFALL).collect())
        {
            if (!area.contains(WorldPoint.fromLocalInstance(client, object.getLocalPoint())))
            {
                continue;
            }
            WorldPoint at = object.getWorldPoint();
            if (object.getId() == ObjectID.MAGICTRAINING_MINI_STATUE_PITFALL)
            {
                finish = at;
            }
            else
            {
                walls.add(at);
            }
        }
        if (walls.isEmpty() || finish == null)
        {
            return;
        }
        TelekineticSolver maze = new TelekineticSolver(walls);
        next.setMaze(maze);
        WorldView view = client.getTopLevelWorldView();
        TelekineticSolver.Slider slider = (from, direction) -> slide(view, from, direction);
        WorldPoint start = next.isGuardianMoving() ? slider.slide(tile, new Angle(guardian.getOrientation()).getNearestDirection()) : tile;
        if (!start.equals(finish))
        {
            next.getMoves().addAll(TelekineticSolver.solve(start, finish, slider));
        }
    }

    private static WorldPoint slide(WorldView view, WorldPoint from, Direction direction)
    {
        int dx = direction == Direction.EAST ? 1 : direction == Direction.WEST ? -1 : 0;
        int dy = direction == Direction.NORTH ? 1 : direction == Direction.SOUTH ? -1 : 0;
        WorldPoint tile = from;
        WorldArea area = new WorldArea(tile, 1, 1);
        while (area.canTravelInDirection(view, dx, dy))
        {
            tile = tile.dx(dx).dy(dy);
            area = new WorldArea(tile, 1, 1);
        }
        return tile;
    }
}
