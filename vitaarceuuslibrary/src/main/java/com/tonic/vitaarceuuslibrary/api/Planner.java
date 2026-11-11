package com.tonic.vitaarceuuslibrary.api;

import com.tonic.vitaarceuuslibrary.data.Action;
import com.tonic.vitaarceuuslibrary.data.EfficientMode;
import com.tonic.vitaarceuuslibrary.data.ReadMode;
import com.tonic.vitaarceuuslibrary.library.Book;
import com.tonic.vitaarceuuslibrary.library.Bookcase;
import com.tonic.vitaarceuuslibrary.library.Customer;
import com.tonic.vitaarceuuslibrary.library.Library;
import com.tonic.vitaarceuuslibrary.library.SolvedState;
import net.runelite.api.coords.WorldPoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Chooses the next action in the library from what is known: read rewards, hand in, fetch or search for the requested book, or ask the nearest customer, with an optional stop for a spare book on the way.
 */
public final class Planner
{
    static final int REGRAB_MULTIPLIER = 3;

    private final Distances distances;
    private boolean restocking;

    /**
     * Creates a planner.
     *
     * @param distances the walking distances to plan with
     */
    public Planner(Distances distances)
    {
        this.distances = distances;
    }

    /**
     * Chooses the next action.
     *
     * @param player the player's tile
     * @param tracker what is known about the session
     * @param customers where each customer stands now
     * @param efficientMode how spare books are collected
     * @param maxDetour the most tiles a spare book may add in detour mode
     * @param restockAt how many books may be missing in stock-up mode before every missing book is collected
     * @param readMode when rewards are read
     * @return the action
     */
    public Action next(WorldPoint player, LibraryTracker tracker, Map<Customer, WorldPoint> customers, EfficientMode efficientMode, int maxDetour, int restockAt, ReadMode readMode)
    {
        if (!LibraryMap.contains(player))
        {
            return Action.walkToLibrary();
        }

        Set<Book> held = tracker.getHeld();
        Book request = tracker.getRequest();
        boolean holdingRequest = request != null && held.contains(request);
        int reserve = holdingRequest ? 0 : 1;
        if (tracker.getRewards() > 0 && (readMode == ReadMode.AFTER_HAND_IN || tracker.getFreeSlots() <= reserve))
        {
            return Action.read();
        }

        Distances.Table fromPlayer = distances.from(Collections.singletonList(player));
        Action main;
        List<WorldPoint> mainTiles;
        if (holdingRequest)
        {
            Customer customer = tracker.getRequester();
            if (customer == null)
            {
                return Action.waitFor("Holding " + request.getShortName() + " but the customer is unknown");
            }
            main = Action.handIn(customer, request);
            mainTiles = distances.standTiles(customers.getOrDefault(customer, customer.getHome()));
        }
        else if (request != null)
        {
            if (tracker.getFreeSlots() == 0)
            {
                return Action.waitFor("Inventory is full: free a slot for " + request.getShortName());
            }
            Bookcase shelf = shelfFor(tracker.getLibrary(), request, fromPlayer);
            if (shelf == null)
            {
                tracker.getLibrary().reset();
                return Action.waitFor("No shelf left to search, starting over");
            }
            main = Action.search(shelf, isKnownAt(shelf, request) ? request : null, false);
            mainTiles = distances.standTiles(shelf);
        }
        else
        {
            Customer customer = nearestCustomer(tracker, customers, fromPlayer);
            if (customer == null)
            {
                return Action.waitFor("No customer can give a request");
            }
            main = Action.talk(customer);
            mainTiles = distances.standTiles(customers.getOrDefault(customer, customer.getHome()));
        }

        if (efficientMode != EfficientMode.OFF && tracker.getFreeSlots() >= reserve + 1)
        {
            Action spare = spare(tracker, isRestocking(tracker, efficientMode, restockAt), maxDetour, fromPlayer, mainTiles);
            if (spare == null)
            {
                restocking = false;
            }
            if (spare != null)
            {
                return spare;
            }
        }
        return main;
    }

    private boolean isRestocking(LibraryTracker tracker, EfficientMode mode, int restockAt)
    {
        if (mode != EfficientMode.STOCK_UP)
        {
            restocking = false;
            return false;
        }
        int missing = 0;
        for (Book book : Book.values())
        {
            if (book.isRequestable() && !tracker.getHeld().contains(book))
            {
                missing++;
            }
        }
        if (missing >= restockAt)
        {
            restocking = true;
        }
        return restocking;
    }

    private Bookcase shelfFor(Library library, Book book, Distances.Table fromPlayer)
    {
        List<Bookcase> known = new ArrayList<>();
        List<Bookcase> possible = new ArrayList<>();
        List<Bookcase> unsearched = new ArrayList<>();
        for (Bookcase bookcase : library.getBookcases())
        {
            if (bookcase.isBookSet())
            {
                if (bookcase.getBook() == book)
                {
                    known.add(bookcase);
                }
                continue;
            }
            unsearched.add(bookcase);
            if (library.getState() != SolvedState.NO_DATA && bookcase.getPossibleBooks().contains(book))
            {
                possible.add(bookcase);
            }
        }
        List<Bookcase> candidates = !known.isEmpty() ? known : !possible.isEmpty() ? possible : unsearched;
        return nearestShelf(candidates, fromPlayer);
    }

    private Customer nearestCustomer(LibraryTracker tracker, Map<Customer, WorldPoint> customers, Distances.Table fromPlayer)
    {
        Customer best = null;
        int bestDistance = Distances.UNREACHABLE;
        for (Customer customer : Customer.values())
        {
            boolean eligible = tracker.isRequestUnknown() ? !tracker.getBusy().contains(customer) : customer != tracker.getLastServed();
            if (!eligible)
            {
                continue;
            }
            int distance = fromPlayer.toAny(distances.standTiles(customers.getOrDefault(customer, customer.getHome())));
            if (best == null || distance < bestDistance)
            {
                best = customer;
                bestDistance = distance;
            }
        }
        return best;
    }

    private Action spare(LibraryTracker tracker, boolean unlimited, int maxDetour, Distances.Table fromPlayer, List<WorldPoint> mainTiles)
    {
        int base = fromPlayer.toAny(mainTiles);
        if (base == Distances.UNREACHABLE)
        {
            return null;
        }
        Distances.Table fromMain = null;
        Bookcase best = null;
        Book bestBook = null;
        int bestDetour = Integer.MAX_VALUE;
        Library library = tracker.getLibrary();
        for (Bookcase bookcase : library.getBookcases())
        {
            Book book = knownBook(library, bookcase);
            if (book == null || !book.isRequestable() || book == tracker.getRequest() || tracker.getHeld().contains(book))
            {
                continue;
            }
            int allowed = unlimited ? Integer.MAX_VALUE : maxDetour * (book == tracker.getLastDelivered() ? REGRAB_MULTIPLIER : 1);
            List<WorldPoint> stands = distances.standTiles(bookcase);
            int toShelf = fromPlayer.toAny(stands);
            if (toShelf == Distances.UNREACHABLE || (allowed != Integer.MAX_VALUE && toShelf - base > allowed))
            {
                continue;
            }
            if (fromMain == null)
            {
                fromMain = distances.from(mainTiles);
            }
            int onward = fromMain.toAny(stands);
            if (onward == Distances.UNREACHABLE)
            {
                continue;
            }
            int detour = toShelf + onward - base;
            if (detour <= allowed && detour < bestDetour)
            {
                best = bookcase;
                bestBook = book;
                bestDetour = detour;
            }
        }
        return best == null ? null : Action.search(best, bestBook, true);
    }

    private Bookcase nearestShelf(List<Bookcase> candidates, Distances.Table fromPlayer)
    {
        Bookcase best = null;
        int bestDistance = Distances.UNREACHABLE;
        for (Bookcase bookcase : candidates)
        {
            int distance = fromPlayer.toAny(distances.standTiles(bookcase));
            if (best == null || distance < bestDistance)
            {
                best = bookcase;
                bestDistance = distance;
            }
        }
        return best;
    }

    private static boolean isKnownAt(Bookcase bookcase, Book book)
    {
        return bookcase.isBookSet() ? bookcase.getBook() == book : bookcase.getPossibleBooks().size() == 1 && bookcase.getPossibleBooks().contains(book);
    }

    private static Book knownBook(Library library, Bookcase bookcase)
    {
        if (bookcase.isBookSet())
        {
            return bookcase.getBook();
        }
        if (library.getState() == SolvedState.COMPLETE && bookcase.getPossibleBooks().size() == 1)
        {
            return bookcase.getPossibleBooks().iterator().next();
        }
        return null;
    }
}
