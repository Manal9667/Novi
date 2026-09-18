package com.novi.recommendation;

import com.novi.config.AiProperties;
import com.novi.entity.Book;
import com.novi.service.AnthropicClient;
import com.novi.service.CandidateRetrievalService.ScoredCandidate;
import com.novi.service.RecommendationRerankingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationRerankingServiceTest {

    @Mock private AnthropicClient anthropicClient;
    @Mock private AiProperties aiProperties;

    @InjectMocks
    private RecommendationRerankingService recommendationRerankingService;

    @Test
    void rerank_withNoCandidates_returnsEmptyList() {
        var result = recommendationRerankingService.rerank(null, List.of(), List.of(), List.of(), null);

        assertThat(result).isEmpty();
    }

    @Test
    void rerank_whenAnthropicUnavailable_fallsBackToBaselineScoreOrdering() {
        when(anthropicClient.isAvailable()).thenReturn(false);
        when(aiProperties.getRecommendationCount()).thenReturn(10);

        Book lowScoreBook = Book.builder().id(1L).title("Low Match").build();
        Book highScoreBook = Book.builder().id(2L).title("High Match").build();

        List<ScoredCandidate> candidates = List.of(
                new ScoredCandidate(lowScoreBook, 0.2),
                new ScoredCandidate(highScoreBook, 0.9)
        );

        var result = recommendationRerankingService.rerank(null, candidates, List.of(), List.of(), null);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).book()).isEqualTo(highScoreBook);
        assertThat(result.get(1).book()).isEqualTo(lowScoreBook);
    }
}
