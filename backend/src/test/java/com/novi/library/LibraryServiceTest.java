package com.novi.library;

import com.novi.dto.library.UserBookResponse;
import com.novi.entity.Book;
import com.novi.entity.User;
import com.novi.entity.UserBook;
import com.novi.entity.enums.ReadingStatus;
import com.novi.exception.DuplicateResourceException;
import com.novi.exception.ResourceNotFoundException;
import com.novi.repository.UserBookRepository;
import com.novi.service.BookService;
import com.novi.service.LibraryService;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.novi.support.TestBooks.book;
import static com.novi.support.TestBooks.genre;
import static com.novi.support.TestBooks.set;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LibraryServiceTest {

    private final UserBookRepository userBookRepository = mock(UserBookRepository.class);
    private final BookService bookService = mock(BookService.class);
    private final LibraryService service = new LibraryService(userBookRepository, bookService);

    private final User user = User.builder().id(1L).username("reader").build();
    private final Book book = book(2, "The Silent Patient", set(genre(1, "Psychological Thriller")), set());

    @Test
    void savesANewBookWithDefaultStatus() {
        when(bookService.getEntityById(2L)).thenReturn(book);
        when(userBookRepository.existsByUserAndBook(user, book)).thenReturn(false);
        when(userBookRepository.save(any(UserBook.class))).thenAnswer(inv -> inv.getArgument(0));

        UserBookResponse response = service.addBook(user, 2L, null);

        assertThat(response.status()).isEqualTo(ReadingStatus.SAVED);
        assertThat(response.book().id()).isEqualTo(2L);
    }

    @Test
    void rejectsDuplicateSave() {
        when(bookService.getEntityById(2L)).thenReturn(book);
        when(userBookRepository.existsByUserAndBook(user, book)).thenReturn(true);

        assertThatThrownBy(() -> service.addBook(user, 2L, ReadingStatus.SAVED))
                .isInstanceOf(DuplicateResourceException.class);
        verify(userBookRepository, never()).save(any());
    }

    @Test
    void removingABookNotInLibraryThrowsNotFound() {
        when(bookService.getEntityById(2L)).thenReturn(book);
        when(userBookRepository.findByUserAndBook(user, book)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.removeBook(user, 2L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(userBookRepository, never()).delete(any());
    }

    @Test
    void removesABookInLibrary() {
        UserBook entry = UserBook.builder().user(user).book(book).status(ReadingStatus.SAVED).build();
        when(bookService.getEntityById(2L)).thenReturn(book);
        when(userBookRepository.findByUserAndBook(user, book)).thenReturn(Optional.of(entry));

        service.removeBook(user, 2L);

        verify(userBookRepository).delete(entry);
    }
}
