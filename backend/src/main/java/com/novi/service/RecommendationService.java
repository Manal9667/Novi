package com.novi.service;

import com.novi.dto.recommendation.RecommendationResponse;
import com.novi.entity.Author;
import com.novi.entity.Book;
import com.novi.entity.Genre;
import com.novi.entity.Theme;
import com.novi.entity.User;
import com.novi.service.CandidateRetrievalService.ScoredCandidate;
import com.novi.service.RankingService.RankedBook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Orchestrates Novi's Retrieval-Augmented Generation recommendation pipeline:
 * <ol>
 *   <li><b>Understand</b> the request into a structured intent
 *       ({@link QueryIntentService}).</li>
 *   <li><b>Embed + retrieve</b>: embed the request once and retrieve a candidate
 *       pool by semantic (pgvector) + lexical relevance
 *       ({@link CandidateRetrievalService}).</li>
 *   <li><b>Rank</b> with a transparent weighted hybrid score
 *       ({@link RankingService}).</li>
 *   <li><b>Explain</b> each top pick with a grounded Gemini explanation
 *       ({@link ExplanationService}).</li>
 *   <li><b>Respond</b> with structured, frontend-ready recommendations.</li>
 * </ol>
 * Each step degrades gracefully: without Gemini, intent falls back to keywords,
 * retrieval to lexical search, and explanations to deterministic sentences.
 * Results are ephemeral and never persisted.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService {

    private final QueryIntentService queryIntentService;
    private final EmbeddingService embeddingService;
    private final CandidateRetrievalService candidateRetrievalService;
    private final RankingService rankingService;
    private final ExplanationService explanationService;

    @Transactional(readOnly = true)
    public List<RecommendationResponse> recommend(User user, String query) {
        // Step 1 — understand the request.
        QueryIntent intent = queryIntentService.extract(query);

        // Step 2 — embed the request once, then retrieve candidates.
        Optional<float[]> queryVec = embeddingService.isAvailable()
                ? embeddingService.embed(intent.expandedQueryText(query))
                : Optional.empty();
        List<ScoredCandidate> candidates = candidateRetrievalService.retrieve(user, intent, query, queryVec);

        if (candidates.isEmpty()) {
            log.info("Recommend[\"{}\"]: no candidates retrieved", query);
            return List.of();
        }

        // Step 3 — transparent hybrid ranking (trims to the final count).
        List<RankedBook> ranked = rankingService.rank(candidates, intent, queryVec, user);

        // Step 4 — grounded explanations for the final picks.
        Map<Long, String> explanations = explanationService.explain(query, ranked);

        // Step 5 — structured response.
        return ranked.stream()
                .map(rb -> toResponse(rb, explanations.get(rb.book().getId())))
                .toList();
    }

    private RecommendationResponse toResponse(RankedBook rb, String explanation) {
        Book book = rb.book();
        return new RecommendationResponse(
                book.getId(),
                book.getTitle(),
                book.getAuthors().stream().map(Author::getName).toList(),
                book.getCoverImageUrl(),
                book.getDescription(),
                book.getGenres().stream().map(Genre::getName).toList(),
                book.getThemes().stream().map(Theme::getName).toList(),
                (int) Math.round(rb.score() * 100),
                explanation
        );
    }
}
