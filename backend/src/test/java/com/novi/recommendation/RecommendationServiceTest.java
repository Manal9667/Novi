package com.novi.recommendation;

import com.novi.dto.recommendation.RecommendationResponse;
import com.novi.entity.Book;
import com.novi.service.CandidateRetrievalService;
import com.novi.service.CandidateRetrievalService.ScoredCandidate;
import com.novi.service.EmbeddingService;
import com.novi.service.ExplanationService;
import com.novi.service.QueryIntent;
import com.novi.service.QueryIntentService;
import com.novi.service.RankingService;
import com.novi.service.RankingService.RankedBook;
import com.novi.service.RecommendationService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.novi.support.TestBooks.book;
import static com.novi.support.TestBooks.genre;
import static com.novi.support.TestBooks.set;
import static com.novi.support.TestBooks.theme;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RecommendationServiceTest {

    private final QueryIntentService queryIntentService = mock(QueryIntentService.class);
    private final EmbeddingService embeddingService = mock(EmbeddingService.class);
    private final CandidateRetrievalService candidateRetrievalService = mock(CandidateRetrievalService.class);
    private final RankingService rankingService = mock(RankingService.class);
    private final ExplanationService explanationService = mock(ExplanationService.class);

    private final RecommendationService service = new RecommendationService(
            queryIntentService, embeddingService, candidateRetrievalService, rankingService, explanationService);

    @Test
    void returnsEmptyWhenNoCandidatesRetrieved() {
        when(queryIntentService.extract("x")).thenReturn(QueryIntent.keywordFallback("x"));
        when(embeddingService.isAvailable()).thenReturn(false);
        when(candidateRetrievalService.retrieve(any(), any(), any(), any())).thenReturn(List.of());

        assertThat(service.recommend(null, "x")).isEmpty();
        verifyNoInteractions(rankingService, explanationService);
    }

    @Test
    void mapsRankedBooksToResponsesWithScoreAndExplanation() {
        Book b = book(2, "The Silent Patient", set(genre(1, "Psychological Thriller")), set(theme(1, "twist")));
        when(queryIntentService.extract("thriller")).thenReturn(QueryIntent.keywordFallback("thriller"));
        when(embeddingService.isAvailable()).thenReturn(false);
        when(candidateRetrievalService.retrieve(any(), any(), any(), any()))
                .thenReturn(List.of(new ScoredCandidate(b, 0.5)));
        when(rankingService.rank(any(), any(), any(), any()))
                .thenReturn(List.of(new RankedBook(b, 0.87, 0.9, 0.5, 0.0)));
        when(explanationService.explain(any(), any())).thenReturn(Map.of(2L, "Because it fits."));

        List<RecommendationResponse> out = service.recommend(null, "thriller");

        assertThat(out).hasSize(1);
        RecommendationResponse r = out.get(0);
        assertThat(r.bookId()).isEqualTo(2L);
        assertThat(r.relevanceScore()).isEqualTo(87);
        assertThat(r.explanation()).isEqualTo("Because it fits.");
        assertThat(r.genres()).contains("Psychological Thriller");
        assertThat(r.themes()).contains("twist");
    }
}
