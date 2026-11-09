package com.novi.recommendation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.novi.entity.Book;
import com.novi.service.ExplanationService;
import com.novi.service.LlmClient;
import com.novi.service.RankingService.RankedBook;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.novi.support.TestBooks.book;
import static com.novi.support.TestBooks.genre;
import static com.novi.support.TestBooks.set;
import static com.novi.support.TestBooks.theme;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExplanationServiceTest {

    private final LlmClient llm = mock(LlmClient.class);
    private final ExplanationService service = new ExplanationService(llm, new ObjectMapper());

    private static RankedBook ranked(Book b) {
        return new RankedBook(b, 0.8, 0.5, 0.3, 0.0);
    }

    @Test
    void usesDeterministicFallbackWhenLlmUnavailable() {
        when(llm.isAvailable()).thenReturn(false);
        Book b = book(1, "The Silent Patient", set(genre(1, "Psychological Thriller")), set(theme(1, "twist ending")));

        Map<Long, String> out = service.explain("a thriller", List.of(ranked(b)));

        assertThat(out).containsKey(1L);
        assertThat(out.get(1L).toLowerCase()).contains("psychological thriller");
    }

    @Test
    void usesGeminiExplanationsAndIgnoresUnknownIds() {
        when(llm.isAvailable()).thenReturn(true);
        when(llm.complete(anyString(), anyString(), anyInt())).thenReturn(Optional.of("""
                [{"bookId":1,"explanation":"A perfect twist-driven match."},
                 {"bookId":999,"explanation":"should be ignored"}]
                """));
        Book b = book(1, "A", set(genre(1, "Mystery")), set(theme(1, "twist")));

        Map<Long, String> out = service.explain("a thriller", List.of(ranked(b)));

        assertThat(out.get(1L)).isEqualTo("A perfect twist-driven match.");
        assertThat(out).doesNotContainKey(999L);
    }

    @Test
    void fillsMissingBooksWithFallback() {
        when(llm.isAvailable()).thenReturn(true);
        when(llm.complete(anyString(), anyString(), anyInt()))
                .thenReturn(Optional.of("[{\"bookId\":1,\"explanation\":\"Covered.\"}]"));
        Book covered = book(1, "Covered", set(genre(1, "Mystery")), set());
        Book missing = book(2, "Missing", set(genre(2, "Fantasy")), set());

        Map<Long, String> out = service.explain("q", List.of(ranked(covered), ranked(missing)));

        assertThat(out.get(1L)).isEqualTo("Covered.");
        assertThat(out.get(2L)).isNotBlank();
        assertThat(out.get(2L).toLowerCase()).contains("fantasy");
    }
}
