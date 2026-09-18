package com.novi.library;

import com.novi.entity.Book;
import com.novi.entity.User;
import com.novi.entity.UserBook;
import com.novi.entity.enums.HistoryEventType;
import com.novi.entity.enums.ReadingStatus;
import com.novi.exception.DuplicateResourceException;
import com.novi.exception.ResourceNotFoundException;
import com.novi.repository.UserBookRepository;
import com.novi.service.BookService;
import com.novi.service.LibraryService;
import com.novi.service.ReadingHistoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LibraryServiceTest {

    @Mock private UserBookRepository userBookRepository;
    @Mock private BookService bookService;
    @Mock private ReadingHistoryService readingHistoryService;

    @InjectMocks
    private LibraryService libraryService;

    private User user;
    private Book book;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).username("reader").displayName("Reader").build();
        book = Book.builder().id(10L).title("Dune").build();
    }

    @Test
    void addBook_whenNotAlreadyInLibrary_savesAndRecordsHistory() {
        when(bookService.getEntityById(10L)).thenReturn(book);
        when(userBookRepository.existsByUserAndBook(user, book)).thenReturn(false);
        when(userBookRepository.save(any(UserBook.class))).thenAnswer(inv -> {
            UserBook ub = inv.getArgument(0);
            ub.setId(100L);
            return ub;
        });

        var response = libraryService.addBook(user, 10L, ReadingStatus.WANT_TO_READ);

        assertThat(response.status()).isEqualTo(ReadingStatus.WANT_TO_READ);
        verify(readingHistoryService).record(user, book, HistoryEventType.ADDED_TO_LIBRARY);
    }

    @Test
    void addBook_whenAlreadyInLibrary_throwsDuplicate() {
        when(bookService.getEntityById(10L)).thenReturn(book);
        when(userBookRepository.existsByUserAndBook(user, book)).thenReturn(true);

        assertThatThrownBy(() -> libraryService.addBook(user, 10L, null))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void removeBook_whenNotInLibrary_throwsNotFound() {
        when(bookService.getEntityById(10L)).thenReturn(book);
        when(userBookRepository.findByUserAndBook(user, book)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> libraryService.removeBook(user, 10L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateStatus_toRead_setsFinishedAtAndRecordsHistory() {
        UserBook existing = UserBook.builder().id(5L).user(user).book(book).status(ReadingStatus.CURRENTLY_READING).build();
        when(bookService.getEntityById(10L)).thenReturn(book);
        when(userBookRepository.findByUserAndBook(user, book)).thenReturn(Optional.of(existing));
        when(userBookRepository.save(any(UserBook.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = libraryService.updateStatus(user, 10L, ReadingStatus.READ);

        assertThat(response.status()).isEqualTo(ReadingStatus.READ);
        assertThat(response.finishedAt()).isNotNull();
        verify(readingHistoryService).record(user, book, HistoryEventType.FINISHED_READING);
    }
}
