package com.tonic.vitaarceuuslibrary.api;

import com.tonic.vitaarceuuslibrary.data.Action;
import com.tonic.vitaarceuuslibrary.data.EfficientMode;
import com.tonic.vitaarceuuslibrary.data.ReadMode;
import com.tonic.vitaarceuuslibrary.library.Book;
import com.tonic.vitaarceuuslibrary.library.Bookcase;
import com.tonic.vitaarceuuslibrary.library.Customer;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class PlannerTest
{
    private static final WorldPoint MIDDLE = new WorldPoint(1632, 3808, 0);

    private static final Distances FLAT = new Distances()
    {
        @Override
        public Table from(Collection<WorldPoint> sources)
        {
            return point -> {
                int best = UNREACHABLE;
                for (WorldPoint source : sources)
                {
                    int planes = Math.abs(source.getPlane() - point.getPlane()) * 40;
                    best = Math.min(best, Math.max(Math.abs(source.getX() - point.getX()), Math.abs(source.getY() - point.getY())) + planes);
                }
                return best;
            };
        }

        @Override
        public List<WorldPoint> standTiles(Bookcase bookcase)
        {
            return Collections.singletonList(bookcase.getLocation());
        }

        @Override
        public List<WorldPoint> standTiles(WorldPoint npc)
        {
            return Collections.singletonList(npc);
        }
    };

    private final Planner planner = new Planner(FLAT);

    private static Map<Customer, WorldPoint> homes()
    {
        Map<Customer, WorldPoint> tiles = new EnumMap<>(Customer.class);
        for (Customer customer : Customer.values())
        {
            tiles.put(customer, customer.getHome());
        }
        return tiles;
    }

    private Action plan(WorldPoint player, LibraryTracker tracker, EfficientMode mode, ReadMode readMode)
    {
        return planner.next(player, tracker, homes(), mode, 6, 4, readMode);
    }

    private static LibraryTracker withInventory(int... items)
    {
        LibraryTracker tracker = new LibraryTracker();
        int[] inventory = new int[28];
        System.arraycopy(items, 0, inventory, 0, items.length);
        tracker.inventory(inventory);
        return tracker;
    }

    private static Bookcase mark(LibraryTracker tracker, int index, Book book)
    {
        Bookcase bookcase = tracker.getLibrary().getBookcases().get(index);
        tracker.searching(bookcase.getLocation());
        tracker.found(book.getItem());
        return bookcase;
    }

    @Test
    void walksToTheLibraryFromOutside()
    {
        Action action = plan(new WorldPoint(3200, 3200, 0), new LibraryTracker(), EfficientMode.OFF, ReadMode.AFTER_HAND_IN);
        assertEquals(Action.Type.WALK_TO_LIBRARY, action.getType());
    }

    @Test
    void asksTheNearestCustomerButNotTheLastOneServed()
    {
        LibraryTracker tracker = new LibraryTracker();
        Action first = plan(Customer.VILLIA.getHome(), tracker, EfficientMode.OFF, ReadMode.AFTER_HAND_IN);
        assertEquals(Customer.VILLIA, first.getCustomer());

        tracker.dialogue(Customer.VILLIA, "Thank you for finding my book. It is most interesting.");
        Action second = plan(Customer.VILLIA.getHome(), tracker, EfficientMode.OFF, ReadMode.AFTER_HAND_IN);
        assertEquals(Action.Type.TALK, second.getType());
        assertNotEquals(Customer.VILLIA, second.getCustomer());
    }

    @Test
    void looksForAnOpenRequestAmongTheOtherCustomers()
    {
        LibraryTracker tracker = new LibraryTracker();
        tracker.dialogue(Customer.PROFESSOR_GRACKLEBONE, "Aren't you helping someone else at the moment? Don't let me interrupt.");
        Action action = plan(Customer.PROFESSOR_GRACKLEBONE.getHome(), tracker, EfficientMode.OFF, ReadMode.AFTER_HAND_IN);

        assertEquals(Action.Type.TALK, action.getType());
        assertNotEquals(Customer.PROFESSOR_GRACKLEBONE, action.getCustomer());
    }

    @Test
    void fetchesTheRequestedBookWhereItIsKnown()
    {
        LibraryTracker tracker = new LibraryTracker();
        Bookcase shelf = mark(tracker, 0, Book.KILLING_OF_A_KING);
        tracker.dialogue(Customer.VILLIA, LibraryTrackerTest.request(Book.KILLING_OF_A_KING));
        Action action = plan(MIDDLE, tracker, EfficientMode.OFF, ReadMode.AFTER_HAND_IN);

        assertEquals(Action.Type.SEARCH, action.getType());
        assertEquals(shelf, action.getBookcase());
    }

    @Test
    void handsTheBookInWhenHoldingIt()
    {
        LibraryTracker tracker = withInventory(Book.TWILL_ACCORD.getItem());
        tracker.dialogue(Customer.SAM, LibraryTrackerTest.request(Book.TWILL_ACCORD));
        Action action = plan(MIDDLE, tracker, EfficientMode.OFF, ReadMode.AFTER_HAND_IN);

        assertEquals(Action.Type.HAND_IN, action.getType());
        assertEquals(Customer.SAM, action.getCustomer());
    }

    @Test
    void readsRewardsPerTheReadMode()
    {
        LibraryTracker tracker = withInventory(ItemID.ARCEUUS_LIBRARY_REWARD);
        assertEquals(Action.Type.READ, plan(MIDDLE, tracker, EfficientMode.OFF, ReadMode.AFTER_HAND_IN).getType());
        assertEquals(Action.Type.TALK, plan(MIDDLE, tracker, EfficientMode.OFF, ReadMode.WHEN_SPACE_NEEDED).getType());

        int[] full = new int[28];
        java.util.Arrays.fill(full, ItemID.COINS);
        full[0] = ItemID.ARCEUUS_LIBRARY_REWARD;
        LibraryTracker crowded = new LibraryTracker();
        crowded.inventory(full);
        assertEquals(Action.Type.READ, plan(MIDDLE, crowded, EfficientMode.OFF, ReadMode.WHEN_SPACE_NEEDED).getType());
    }

    @Test
    void waitsWhenThereIsNoRoomForTheBook()
    {
        int[] full = new int[28];
        java.util.Arrays.fill(full, ItemID.COINS);
        LibraryTracker tracker = new LibraryTracker();
        tracker.inventory(full);
        tracker.dialogue(Customer.VILLIA, LibraryTrackerTest.request(Book.TWILL_ACCORD));

        assertEquals(Action.Type.WAIT, plan(MIDDLE, tracker, EfficientMode.OFF, ReadMode.AFTER_HAND_IN).getType());
    }

    @Test
    void detourModeOnlyTakesNearbySpares()
    {
        LibraryTracker tracker = new LibraryTracker();
        Bookcase spare = mark(tracker, 0, Book.KILLING_OF_A_KING);
        WorldPoint nearSpare = spare.getLocation();

        Action near = plan(nearSpare, tracker, EfficientMode.DETOUR, ReadMode.AFTER_HAND_IN);
        assertEquals(Action.Type.SEARCH, near.getType());
        assertEquals(Book.KILLING_OF_A_KING, near.getBook());

        WorldPoint far = new WorldPoint(1655, 3830, 0);
        Action stayOnRoute = plan(far, tracker, EfficientMode.DETOUR, ReadMode.AFTER_HAND_IN);
        assertEquals(Action.Type.TALK, stayOnRoute.getType());

        Action stockUp = plan(far, tracker, EfficientMode.STOCK_UP, ReadMode.AFTER_HAND_IN);
        assertEquals(Action.Type.SEARCH, stockUp.getType());
    }

    @Test
    void stockUpServesFromStockUntilEnoughBooksAreMissing()
    {
        WorldPoint far = new WorldPoint(1655, 3830, 0);
        int[] inventory = new int[28];
        int slot = 0;
        for (Book book : Book.values())
        {
            if (book.isRequestable() && book != Book.KILLING_OF_A_KING)
            {
                inventory[slot++] = book.getItem();
            }
        }
        LibraryTracker tracker = new LibraryTracker();
        tracker.inventory(inventory);
        mark(tracker, 0, Book.KILLING_OF_A_KING);

        assertEquals(Action.Type.TALK, plan(far, tracker, EfficientMode.STOCK_UP, ReadMode.AFTER_HAND_IN).getType());

        for (int i = 0; i < 3; i++)
        {
            inventory[i] = 0;
        }
        tracker.inventory(inventory);
        Action refill = plan(far, tracker, EfficientMode.STOCK_UP, ReadMode.AFTER_HAND_IN);
        assertEquals(Action.Type.SEARCH, refill.getType());
        assertEquals(Book.KILLING_OF_A_KING, refill.getBook());
    }

    @Test
    void sparesNeverTakeTheLastFreeSlot()
    {
        int[] inventory = new int[28];
        java.util.Arrays.fill(inventory, ItemID.COINS);
        inventory[0] = 0;
        LibraryTracker tracker = new LibraryTracker();
        tracker.inventory(inventory);
        Bookcase spare = mark(tracker, 0, Book.KILLING_OF_A_KING);

        Action action = plan(spare.getLocation(), tracker, EfficientMode.STOCK_UP, ReadMode.AFTER_HAND_IN);
        assertEquals(Action.Type.TALK, action.getType());
    }
}
