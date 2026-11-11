package com.tonic.vitaarceuuslibrary.api;

import com.tonic.vitaarceuuslibrary.library.Book;
import com.tonic.vitaarceuuslibrary.library.Customer;
import com.tonic.vitaarceuuslibrary.library.Library;
import lombok.Getter;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * What is known about the library session: the request, the books carried, and every search result fed to the layout solver. Fed raw game text and items, so it holds no client references.
 */
public final class LibraryTracker
{
    static final String REQUEST_OPTION = "Sure, what book do you want?";
    static final String EMPTY_SHELF = "You don't find anything useful here.";
    static final String RESHUFFLE = "You hear the shifting of books due to a mysterious force...or are you just hearing things?";

    private static final int INVENTORY_SIZE = 28;
    private static final Pattern BOOK_PATTERN = Pattern.compile("'<col=0000ff>(.*)</col>'");
    private static final Pattern TAG_PATTERN = Pattern.compile("<[^>]*>");
    private static final String[] HAND_IN = {"please accept a token of my thanks.", "You can have this other book", "Thanks, I'll get on with reading it."};
    private static final String ALREADY_SERVED = "for finding";
    private static final String[] BUSY = {"currently assisting another customer", "Aren't you helping someone else", "when you're not busy helping someone else"};

    @Getter
    private final Library library = new Library();
    private final Set<Customer> busy = EnumSet.noneOf(Customer.class);
    private final Set<Book> held = EnumSet.noneOf(Book.class);

    @Getter
    private Book request;
    @Getter
    private Customer requester;
    @Getter
    private Customer lastServed;
    @Getter
    private Book lastDelivered;
    @Getter
    private int rewards;
    @Getter
    private int freeSlots = INVENTORY_SIZE;
    @Getter
    private int delivered;
    @Getter
    private WorldPoint searching;
    private String lastDialogue = "";

    /**
     * Reads a line of customer dialogue: a request or reminder naming the book, a hand-in, or a refusal because another customer's request is open.
     *
     * @param customer the customer speaking, or null if the speaker is not a library customer
     * @param text the dialogue text, with its tags
     */
    public void dialogue(Customer customer, String text)
    {
        String key = (customer == null ? "" : customer.name()) + '|' + text;
        if (customer == null || text == null || key.equals(lastDialogue))
        {
            lastDialogue = key;
            return;
        }
        lastDialogue = key;

        Matcher matcher = BOOK_PATTERN.matcher(text);
        if (matcher.find())
        {
            Book book = Book.byName(TAG_PATTERN.matcher(matcher.group(1).replace("<br>", " ")).replaceAll(""));
            if (book != null)
            {
                request = book;
                requester = customer;
                busy.clear();
            }
            return;
        }
        if (containsAny(text, HAND_IN))
        {
            lastServed = customer;
            lastDelivered = request;
            request = null;
            requester = null;
            busy.clear();
            delivered++;
            return;
        }
        if (containsAny(text, BUSY) && request == null)
        {
            busy.add(customer);
            return;
        }
        if (text.contains(ALREADY_SERVED) && customer != requester)
        {
            lastServed = customer;
        }
    }

    /**
     * Records that the dialogue closed, so the same line spoken again later counts as new.
     */
    public void dialogueClosed()
    {
        lastDialogue = "";
    }

    /**
     * Marks a bookshelf as being searched, so the result can be credited to it.
     *
     * @param shelf the bookshelf's tile
     */
    public void searching(WorldPoint shelf)
    {
        searching = shelf;
    }

    /**
     * Stops waiting for a search result.
     */
    public void cancelSearch()
    {
        searching = null;
    }

    /**
     * Reads the item shown after a search found a book.
     *
     * @param itemId the item shown
     */
    public void found(int itemId)
    {
        Book book = Book.byId(itemId);
        if (searching == null || book == null)
        {
            return;
        }
        library.mark(searching, book);
        searching = null;
    }

    /**
     * Reads a game message: an empty shelf or the books being reshuffled.
     *
     * @param message the message, with its tags
     */
    public void gameMessage(String message)
    {
        if (message == null)
        {
            return;
        }
        if (message.equals(EMPTY_SHELF) && searching != null)
        {
            library.mark(searching, null);
            searching = null;
        }
        else if (message.endsWith(RESHUFFLE))
        {
            library.reset();
        }
    }

    /**
     * Reads the inventory.
     *
     * @param itemIds the item id in every slot, 0 or less for an empty slot
     */
    public void inventory(int[] itemIds)
    {
        held.clear();
        int used = 0;
        int rewardCount = 0;
        for (int id : itemIds)
        {
            if (id <= 0)
            {
                continue;
            }
            used++;
            if (id == ItemID.ARCEUUS_LIBRARY_REWARD)
            {
                rewardCount++;
            }
            Book book = Book.byId(id);
            if (book != null)
            {
                held.add(book);
            }
        }
        rewards = rewardCount;
        freeSlots = Math.max(0, INVENTORY_SIZE - used);
    }

    /**
     * Forgets the book layout after a world change, since every world shuffles on its own. The request is kept: it belongs to the player, not the world.
     */
    public void worldChanged()
    {
        library.reset();
        searching = null;
        lastDialogue = "";
    }

    /**
     * Forgets everything, for a fresh start.
     */
    public void clear()
    {
        worldChanged();
        request = null;
        requester = null;
        lastServed = null;
        lastDelivered = null;
        busy.clear();
        delivered = 0;
    }

    /**
     * @return the books carried
     */
    public Set<Book> getHeld()
    {
        return Collections.unmodifiableSet(held);
    }

    /**
     * @return the customers who said another customer's request is open
     */
    public Set<Customer> getBusy()
    {
        return Collections.unmodifiableSet(busy);
    }

    /**
     * @return whether a request is open whose book and customer are not known yet
     */
    public boolean isRequestUnknown()
    {
        return request == null && !busy.isEmpty();
    }

    private static boolean containsAny(String text, String[] needles)
    {
        for (String needle : needles)
        {
            if (text.contains(needle))
            {
                return true;
            }
        }
        return false;
    }
}
