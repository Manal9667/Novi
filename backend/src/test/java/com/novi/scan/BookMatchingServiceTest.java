package com.novi.scan;

import com.novi.entity.Book;
import com.novi.service.BookMatchingService;
import com.novi.service.BookService;
import com.novi.service.OpenLibraryService;
import com.novi.service.OpenLibraryService.ExternalBook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookMatchingServiceTest {

    @Mock private OpenLibraryService openLibraryService;
    @Mock private BookService bookService;

    @InjectMocks
    private BookMatchingService bookMatchingService;

    private ExternalBook external(String id, String title, String... authors) {
        return new ExternalBook(id, title, null, null, null, List.of(authors));
    }

    @Test
    void match_whenProviderReturnsNothing_returnsNoMatch() {
        when(openLibraryService.search(anyString(), anyInt())).thenReturn(List.of());

        BookMatchingService.MatchResult result = bookMatchingService.match("Some Obscure Title", "Nobody");

        assertThat(result.book()).isNull();
        assertThat(result.confidence()).isZero();
        verifyNoInteractions(bookService);
    }

    @Test
    void match_picksBestCandidateByTitleAndAuthor_andImportsIt() {
        ExternalBook wrong = external("/works/1", "Doon", "Someone Else");
        ExternalBook right = external("/works/2", "Dune", "Frank Herbert");
        when(openLibraryService.search(anyString(), anyInt())).thenReturn(List.of(wrong, right));

        Book imported = Book.builder().id(10L).title("Dune").build();
        when(bookService.importIfNeeded(right)).thenReturn(imported);

        BookMatchingService.MatchResult result = bookMatchingService.match("Dune", "Frank Herbert");

        assertThat(result.book()).isEqualTo(imported);
        assertThat(result.confidence()).isGreaterThan(0.9);
        verify(bookService).importIfNeeded(right);
        verify(bookService, never()).importIfNeeded(wrong);
    }

    @Test
    void match_withoutDetectedAuthor_usesTitleOnly() {
        ExternalBook right = external("/works/2", "The Hobbit", "J.R.R. Tolkien");
        when(openLibraryService.search(anyString(), anyInt())).thenReturn(List.of(right));
        when(bookService.importIfNeeded(any())).thenReturn(Book.builder().id(11L).title("The Hobbit").build());

        BookMatchingService.MatchResult result = bookMatchingService.match("The Hobbit", "");

        assertThat(result.book()).isNotNull();
        assertThat(result.confidence()).isEqualTo(1.0);
    }
}
