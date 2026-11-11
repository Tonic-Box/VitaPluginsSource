package com.tonic.vitaarceuuslibrary.api;

import com.tonic.Static;
import com.tonic.api.entities.NpcAPI;
import com.tonic.api.entities.TileObjectAPI;
import com.tonic.api.widgets.WidgetAPI;
import com.tonic.data.wrappers.NpcEx;
import com.tonic.data.wrappers.PlayerEx;
import com.tonic.data.wrappers.TileObjectEx;
import com.tonic.vitaarceuuslibrary.library.Customer;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.widgets.Widget;

import java.util.EnumMap;
import java.util.Map;

/**
 * Game queries for the Arceuus Library: the customers, the bookshelves, and the dialogue and item box the tracker reads.
 */
public final class LibraryAPI
{
    /**
     * The middle of the library's ground floor, where walking into the library ends.
     */
    public static final WorldArea GROUND_FLOOR = new WorldArea(1625, 3800, 12, 14, 0);

    private static final int INVENTORY_SIZE = 28;

    private LibraryAPI()
    {
    }

    /**
     * @return the player's tile, or null when not logged in
     */
    public static WorldPoint playerTile()
    {
        PlayerEx player = PlayerEx.getLocal();
        return player == null ? null : player.getWorldPoint();
    }

    /**
     * Finds a customer.
     *
     * @param customer the customer
     * @return the customer's NPC, or null if not loaded
     */
    public static NpcEx npc(Customer customer)
    {
        return NpcAPI.search().withIds(customer.getNpcId()).nearest();
    }

    /**
     * @return where every customer stands now, at their usual spot when not loaded
     */
    public static Map<Customer, WorldPoint> customerTiles()
    {
        Map<Customer, WorldPoint> tiles = new EnumMap<>(Customer.class);
        for (Customer customer : Customer.values())
        {
            NpcEx npc = npc(customer);
            tiles.put(customer, npc == null ? customer.getHome() : npc.getWorldPoint());
        }
        return tiles;
    }

    /**
     * Finds the bookshelf on a tile.
     *
     * @param tile the bookshelf's tile
     * @return the bookshelf, or null if not loaded
     */
    public static TileObjectEx bookshelf(WorldPoint tile)
    {
        return TileObjectAPI.search().atLocation(tile).withAction("Search").first();
    }

    /**
     * @return the customer speaking in the open NPC dialogue, or null
     */
    public static Customer speaker()
    {
        return Static.invoke(() -> {
            Widget head = WidgetAPI.get(InterfaceID.ChatLeft.HEAD);
            return head == null || head.isHidden() ? null : Customer.byNpcId(head.getModelId());
        });
    }

    /**
     * @return the open NPC dialogue's text, or null
     */
    public static String npcText()
    {
        return Static.invoke(() -> {
            Widget text = WidgetAPI.get(InterfaceID.ChatLeft.TEXT);
            return text == null || text.isHidden() ? null : text.getText();
        });
    }

    /**
     * @return the item shown in the item box, or -1 when it is not open
     */
    public static int itemBoxItem()
    {
        return Static.invoke(() -> {
            Widget item = WidgetAPI.get(InterfaceID.Objectbox.ITEM);
            return item == null || item.isHidden() ? -1 : item.getItemId();
        });
    }

    /**
     * @return the item id in every inventory slot, 0 for an empty slot
     */
    public static int[] inventory()
    {
        return Static.invoke(() -> {
            Client client = Static.getClient();
            int[] ids = new int[INVENTORY_SIZE];
            ItemContainer container = client.getItemContainer(InventoryID.INV);
            if (container != null)
            {
                Item[] items = container.getItems();
                for (int i = 0; i < Math.min(items.length, INVENTORY_SIZE); i++)
                {
                    ids[i] = items[i].getId();
                }
            }
            return ids;
        });
    }
}
