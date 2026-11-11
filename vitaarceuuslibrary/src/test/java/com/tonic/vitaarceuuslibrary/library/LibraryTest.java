package com.tonic.vitaarceuuslibrary.library;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LibraryTest
{
    @Test
    void startsWithNothingKnown()
    {
        Library library = new Library();
        assertEquals(SolvedState.NO_DATA, library.getState());
        assertEquals(353, library.getBookcases().size());
        assertTrue(library.getBookcases().stream().noneMatch(Bookcase::isBookSet));
    }

    @Test
    void oneFoundBookPredictsTheOthers()
    {
        Library library = new Library();
        library.mark(0, Book.KILLING_OF_A_KING);

        assertNotEquals(SolvedState.NO_DATA, library.getState());
        assertEquals(Book.KILLING_OF_A_KING, library.getBookcases().get(0).getBook());
        assertTrue(library.getBookcases().stream().anyMatch(b -> !b.getPossibleBooks().isEmpty()));
    }

    @Test
    void confirmingAPredictionKeepsTheLayout()
    {
        Library library = new Library();
        library.mark(0, Book.KILLING_OF_A_KING);
        Bookcase predicted = library.getBookcases().stream().filter(b -> !b.isBookSet() && b.getPossibleBooks().size() == 1 && b.getIndex().size() == 1).findFirst().orElse(null);
        assertNotNull(predicted);

        library.mark(predicted.getLocation(), predicted.getPossibleBooks().iterator().next());

        assertNotEquals(SolvedState.NO_DATA, library.getState());
        assertTrue(predicted.isBookSet());
    }

    @Test
    void aContradictionResetsTheLayout()
    {
        Library library = new Library();
        library.mark(0, Book.KILLING_OF_A_KING);
        Bookcase predicted = library.getBookcases().stream().filter(b -> !b.isBookSet() && !b.getPossibleBooks().isEmpty() && b.getIndex().size() == 1).findFirst().orElse(null);
        assertNotNull(predicted);
        Book wrong = null;
        for (Book book : Book.values())
        {
            if (!predicted.getPossibleBooks().contains(book))
            {
                wrong = book;
                break;
            }
        }
        assertNotNull(wrong);

        library.mark(predicted.getLocation(), wrong);

        assertFalse(library.getBookcases().get(0).isBookSet());
    }

    @Test
    void resetForgetsEverything()
    {
        Library library = new Library();
        library.mark(0, Book.KILLING_OF_A_KING);
        library.reset();

        assertEquals(SolvedState.NO_DATA, library.getState());
        assertTrue(library.getBookcases().stream().allMatch(b -> !b.isBookSet() && b.getPossibleBooks().isEmpty()));
    }
}
