package com.tonic.vitaarceuuslibrary.data;

import com.tonic.vitaarceuuslibrary.library.Book;
import com.tonic.vitaarceuuslibrary.library.Bookcase;
import com.tonic.vitaarceuuslibrary.library.Customer;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * The next thing to do in the library, as chosen by the planner.
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class Action
{
    /**
     * The kinds of action.
     */
    public enum Type
    {
        WALK_TO_LIBRARY,
        TALK,
        HAND_IN,
        SEARCH,
        READ,
        WAIT
    }

    private final Type type;
    private final Customer customer;
    private final Bookcase bookcase;
    private final Book book;
    private final String reason;

    /**
     * @return an action that walks to the library
     */
    public static Action walkToLibrary()
    {
        return new Action(Type.WALK_TO_LIBRARY, null, null, null, "Walking to the library");
    }

    /**
     * Creates an action that asks a customer for a request.
     *
     * @param customer the customer
     * @return the action
     */
    public static Action talk(Customer customer)
    {
        return new Action(Type.TALK, customer, null, null, "Asking " + customer + " for a book");
    }

    /**
     * Creates an action that hands a book in.
     *
     * @param customer the customer who asked for it
     * @param book the book
     * @return the action
     */
    public static Action handIn(Customer customer, Book book)
    {
        return new Action(Type.HAND_IN, customer, null, book, "Taking " + book.getShortName() + " to " + customer);
    }

    /**
     * Creates an action that searches a bookshelf.
     *
     * @param bookcase the bookshelf
     * @param book the book hoped for there, or null when exploring
     * @param spare whether the book is a spare rather than the requested one
     * @return the action
     */
    public static Action search(Bookcase bookcase, Book book, boolean spare)
    {
        String what = book == null ? "Searching shelves" : (spare ? "Picking up a spare " : "Fetching ") + book.getShortName();
        return new Action(Type.SEARCH, null, bookcase, book, what);
    }

    /**
     * @return an action that reads a Book of arcane knowledge
     */
    public static Action read()
    {
        return new Action(Type.READ, null, null, null, "Reading a Book of arcane knowledge");
    }

    /**
     * Creates an action that waits a moment.
     *
     * @param reason why it waits
     * @return the action
     */
    public static Action waitFor(String reason)
    {
        return new Action(Type.WAIT, null, null, null, reason);
    }

    @Override
    public String toString()
    {
        return reason;
    }
}
