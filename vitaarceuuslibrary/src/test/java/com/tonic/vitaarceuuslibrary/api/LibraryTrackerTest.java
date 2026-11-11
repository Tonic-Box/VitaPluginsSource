package com.tonic.vitaarceuuslibrary.api;

import com.tonic.vitaarceuuslibrary.library.Book;
import com.tonic.vitaarceuuslibrary.library.Customer;
import com.tonic.vitaarceuuslibrary.library.SolvedState;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LibraryTrackerTest
{
    static String request(Book book)
    {
        return "You are very kind. Please find: '<col=0000ff>" + book.getName() + "</col>'";
    }

    @Test
    void readsARequest()
    {
        LibraryTracker tracker = new LibraryTracker();
        tracker.dialogue(Customer.VILLIA, request(Book.RADAS_CENSUS));

        assertEquals(Book.RADAS_CENSUS, tracker.getRequest());
        assertEquals(Customer.VILLIA, tracker.getRequester());
    }

    @Test
    void readsARequestSplitOverLines()
    {
        LibraryTracker tracker = new LibraryTracker();
        String name = Book.RADAS_CENSUS.getName().replace(" by ", "<br>by ");
        tracker.dialogue(Customer.SAM, "I think I'd like to read: '<col=0000ff>" + name + "</col>'");

        assertEquals(Book.RADAS_CENSUS, tracker.getRequest());
    }

    @Test
    void handInsServeTheCustomer()
    {
        String[] lines = {"Thank you very much. In return, please accept a token of my thanks.", "Thanks, human. You can have this other book that I don't want.", "Thanks, I'll get on with reading it. Meanwhile, you can have this other book of mine - I won't be wanting to read it again."};
        for (String line : lines)
        {
            LibraryTracker tracker = new LibraryTracker();
            tracker.dialogue(Customer.VILLIA, request(Book.TWILL_ACCORD));
            tracker.dialogue(Customer.VILLIA, line);

            assertNull(tracker.getRequest());
            assertEquals(Customer.VILLIA, tracker.getLastServed());
            assertEquals(Book.TWILL_ACCORD, tracker.getLastDelivered());
            assertEquals(1, tracker.getDelivered());
        }
    }

    @Test
    void refusalsMeanAnotherRequestIsOpen()
    {
        LibraryTracker tracker = new LibraryTracker();
        tracker.dialogue(Customer.PROFESSOR_GRACKLEBONE, "Aren't you helping someone else at the moment? Don't let me interrupt.");

        assertTrue(tracker.isRequestUnknown());
        assertTrue(tracker.getBusy().contains(Customer.PROFESSOR_GRACKLEBONE));
    }

    @Test
    void thanksMeanTheCustomerWasJustServed()
    {
        LibraryTracker tracker = new LibraryTracker();
        tracker.dialogue(Customer.SAM, "Thanks for finding the book. I'll have to think up what I need next.");

        assertEquals(Customer.SAM, tracker.getLastServed());
        assertFalse(tracker.isRequestUnknown());
    }

    @Test
    void searchResultsReachTheSolver()
    {
        LibraryTracker tracker = new LibraryTracker();
        WorldPoint shelf = tracker.getLibrary().getBookcases().get(0).getLocation();
        tracker.searching(shelf);
        tracker.found(Book.KILLING_OF_A_KING.getItem());

        assertNull(tracker.getSearching());
        assertTrue(tracker.getLibrary().getBookcases().get(0).isBookSet());

        WorldPoint empty = tracker.getLibrary().getBookcases().get(1).getLocation();
        tracker.searching(empty);
        tracker.gameMessage(LibraryTracker.EMPTY_SHELF);
        assertNull(tracker.getSearching());
    }

    @Test
    void reshuffleForgetsTheLayout()
    {
        LibraryTracker tracker = new LibraryTracker();
        tracker.searching(tracker.getLibrary().getBookcases().get(0).getLocation());
        tracker.found(Book.KILLING_OF_A_KING.getItem());
        tracker.gameMessage("<col=7f0000>" + LibraryTracker.RESHUFFLE);

        assertEquals(SolvedState.NO_DATA, tracker.getLibrary().getState());
    }

    @Test
    void countsBooksRewardsAndSpace()
    {
        LibraryTracker tracker = new LibraryTracker();
        int[] inventory = new int[28];
        inventory[0] = Book.RADAS_CENSUS.getItem();
        inventory[1] = ItemID.ARCEUUS_LIBRARY_REWARD;
        inventory[2] = ItemID.ARCEUUS_LIBRARY_REWARD;
        inventory[3] = ItemID.COINS;
        tracker.inventory(inventory);

        assertTrue(tracker.getHeld().contains(Book.RADAS_CENSUS));
        assertEquals(2, tracker.getRewards());
        assertEquals(24, tracker.getFreeSlots());
    }
}
