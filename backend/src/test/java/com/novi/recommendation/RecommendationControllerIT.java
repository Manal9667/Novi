package com.novi.recommendation;

import com.novi.dto.recommendation.NaturalLanguageRequest;
import com.novi.dto.recommendation.RecommendationResponse;
import com.novi.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the public natural-language recommendation endpoint against the real
 * seeded catalog. No Gemini key is configured under the test profile, so this
 * verifies the graceful, no-AI path: keyword intent + lexical retrieval + a
 * deterministic ranking and explanation.
 */
class RecommendationControllerIT extends AbstractIntegrationTest {

    @Test
    void ask_withBlankQuery_returnsBadRequest() {
        ResponseEntity<Object> response =
                restTemplate.postForEntity("/api/recommendations/ask", new NaturalLanguageRequest(""), Object.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void ask_withRealQuery_returnsRankedRecommendations() {
        ResponseEntity<RecommendationResponse[]> response = restTemplate.postForEntity(
                "/api/recommendations/ask",
                new NaturalLanguageRequest("a psychological thriller"),
                RecommendationResponse[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().length).isGreaterThan(0);

        RecommendationResponse first = response.getBody()[0];
        assertThat(first.bookId()).isNotNull();
        assertThat(first.title()).isNotBlank();
        assertThat(first.explanation()).isNotBlank();
        assertThat(first.relevanceScore()).isBetween(0, 100);
    }
}
