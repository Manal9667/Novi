package com.novi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.novi.dto.book.BookSummaryResponse;
import com.novi.dto.recommendation.FeedbackRequest;
import com.novi.dto.recommendation.RecommendationResponse;
import com.novi.entity.*;
import com.novi.entity.enums.FeedbackType;
import com.novi.entity.enums.ReadingStatus;
import com.novi.entity.enums.RecommendationSource;
import com.novi.exception.ForbiddenException;
import com.novi.exception.ResourceNotFoundException;
import com.novi.repository.RecommendationFeedbackRepository;
import com.novi.repository.RecommendationRepository;
import com.novi.service.CandidateRetrievalService.ScoredCandidate;
import com.novi.service.RecommendationRerankingService.RankedResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * The end-to-end Phase 2 pipeline: recompute the taste profile from every
 * signal collected so far, retrieve a manageable candidate set, ask Claude to
 * rank and explain the best matches, and persist the result so feedback can
 * reference it. This is the class Phase 1's completion-criteria diagram maps
 * onto directly.
 */
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final TasteProfileService tasteProfileService;
    private final CandidateRetrievalService candidateRetrievalService;
    private final RecommendationRerankingService recommendationRerankingService;
    private final RecommendationRepository recommendationRepository;
    private final RecommendationFeedbackRepository recommendationFeedbackRepository;
    private final LibraryService libraryService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public List<RecommendationResponse> getPersonalizedRecommendations(User user) {
        return generate(user, null, RecommendationSource.PROFILE);
    }

    @Transactional
    public List<RecommendationResponse> getRecommendationsForQuery(User user, String query) {
        return generate(user, query, RecommendationSource.NATURAL_LANGUAGE);
    }

    private List<RecommendationResponse> generate(User user, String query, RecommendationSource source) {
        // Recompute first so recommendations always reflect the latest ratings/feedback.
        tasteProfileService.recompute(user);

        List<ScoredCandidate> candidates = candidateRetrievalService.getCandidates(user);
        List<UserGenreAffinity> genreAffinities = tasteProfileService.getGenreAffinities(user);
        List<UserThemeAffinity> themeAffinities = tasteProfileService.getThemeAffinities(user);

        List<RankedResult> ranked = recommendationRerankingService.rerank(
                user, candidates, genreAffinities, themeAffinities, query);

        return ranked.stream().map(result -> persistAndConvert(user, result, source, query)).toList();
    }

    private RecommendationResponse persistAndConvert(User user, RankedResult result,
                                                       RecommendationSource source, String query) {
        Book book = result.book();

        Recommendation recommendation = findExistingRecommendation(user, book, source, query)
            .orElseGet(() -> Recommendation.builder()
                .user(user)
                .book(book)
                .source(source)
                .queryText(query)
                .build());
        recommendation.setMatchScore(result.matchScore());
        recommendation.setReasons(toJson(result.reasons()));
        recommendation.setPotentialDownside(result.potentialDownside());

        recommendation = recommendationRepository.save(recommendation);

        BookSummaryResponse summary = new BookSummaryResponse(
                book.getId(), book.getTitle(), book.getCoverImageUrl(),
                book.getAuthors().stream().map(Author::getName).toList());

        return new RecommendationResponse(
                recommendation.getId(),
                summary,
                (int) Math.round(result.matchScore() * 100),
                result.reasons(),
                result.potentialDownside(),
                recommendation.getCreatedAt()
        );
    }

    @Transactional
    public void recordFeedback(User user, Long recommendationId, FeedbackRequest request) {
        Recommendation recommendation = recommendationRepository.findById(recommendationId)
                .orElseThrow(() -> new ResourceNotFoundException("Recommendation " + recommendationId + " not found"));

        if (!recommendation.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("You can only give feedback on your own recommendations");
        }

        recommendationFeedbackRepository.findByRecommendation(recommendation).ifPresentOrElse(
                existing -> {
                    existing.setFeedbackType(request.feedbackType());
                    existing.setReason(request.reason());
                    recommendationFeedbackRepository.save(existing);
                },
                () -> recommendationFeedbackRepository.save(RecommendationFeedback.builder()
                        .recommendation(recommendation)
                        .user(user)
                        .feedbackType(request.feedbackType())
                        .reason(request.reason())
                        .build())
        );

        if (request.feedbackType() == FeedbackType.ADDED_TO_WANT_TO_READ) {
            try {
                libraryService.addBook(user, recommendation.getBook().getId(), ReadingStatus.WANT_TO_READ);
            } catch (com.novi.exception.DuplicateResourceException ignored) {
                // already in the library - feedback is still recorded
            }
        }

        // Feedback is itself a taste signal, so future recommendations reflect it immediately.
        tasteProfileService.recompute(user);
    }

    private String toJson(List<String> reasons) {
        try {
            return objectMapper.writeValueAsString(reasons);
        } catch (Exception e) {
            return "[]";
        }
    }

    private java.util.Optional<Recommendation> findExistingRecommendation(
            User user, Book book, RecommendationSource source, String query) {
        if (query == null || query.isBlank()) {
            return recommendationRepository.findFirstByUserAndBookAndSourceAndQueryTextIsNullOrderByCreatedAtDesc(
                    user, book, source);
        }
        return recommendationRepository.findFirstByUserAndBookAndSourceAndQueryTextOrderByCreatedAtDesc(
                user, book, source, query);
    }
}
