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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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
@Slf4j
public class RecommendationService {

    // When an explicit query finds fewer than this many relevant books in the
    // local catalog, pull matching books from the external provider (Open
    // Library) and retry - so "Find Your Next Read" isn't limited to whatever
    // has already been imported.
    private static final int MIN_LOCAL_CANDIDATES = 5;
    // Bound on how many external searches one request can trigger, to cap cost.
    private static final int MAX_IMPORT_SEARCHES = 2;
    // How many real titles to ask the LLM to name (and resolve) per request.
    private static final int MAX_SUGGESTIONS = 8;

    private final TasteProfileService tasteProfileService;
    private final CandidateRetrievalService candidateRetrievalService;
    private final RecommendationRerankingService recommendationRerankingService;
    private final QueryIntentService queryIntentService;
    private final RecommendationAgentService recommendationAgentService;
    private final BookSuggestionService bookSuggestionService;
    private final BookService bookService;
    private final RecommendationRepository recommendationRepository;
    private final RecommendationFeedbackRepository recommendationFeedbackRepository;
    private final LibraryService libraryService;
    // Injected Spring-managed bean (shared, pre-configured) rather than a new
    // ObjectMapper() per service instance.
    private final ObjectMapper objectMapper;

    /**
     * "Recommended for You": a candidate pool chosen from the reader's long-term
     * taste, reranked with personalization as the primary signal.
     */
    @Transactional
    public List<RecommendationResponse> getPersonalizedRecommendations(User user) {
        // Recompute only if a taste signal changed since the last profile build,
        // so recommendations still reflect the latest ratings/feedback without
        // paying for a full recompute on every request.
        tasteProfileService.recomputeIfStale(user);

        List<ScoredCandidate> candidates = candidateRetrievalService.getCandidates(user);
        List<UserGenreAffinity> genreAffinities = tasteProfileService.getGenreAffinities(user);
        List<UserThemeAffinity> themeAffinities = tasteProfileService.getThemeAffinities(user);

        List<RankedResult> ranked = recommendationRerankingService.rerank(
                user, candidates, genreAffinities, themeAffinities, null);

        return ranked.stream()
                .map(result -> persistAndConvert(user, result, RecommendationSource.PROFILE, null))
                .toList();
    }

    /**
     * "Find Your Next Read": the reader's explicit request is the PRIMARY signal.
     * The request is interpreted into a structured intent, used to retrieve
     * query-relevant candidates, then reranked with personalization only as a
     * tie-breaker. Works for brand-new users with no reading history, since
     * retrieval is driven by the request rather than the taste profile.
     */
    @Transactional
    public List<RecommendationResponse> getRecommendationsForQuery(User user, String query) {
        // Personalization is a secondary signal here, but still kept fresh so a
        // tie-break reflects the reader's latest taste.
        tasteProfileService.recomputeIfStale(user);

        // Agentic path: the LLM drives its own tool use (search the catalog,
        // reach out to Open Library, read the reader's taste) and submits
        // grounded picks. If it can't complete (no LLM, rate limit, step limit),
        // fall back to the deterministic retrieval pipeline so the request is
        // never silently empty and existing behavior is preserved.
        List<RankedResult> ranked = recommendationAgentService.run(user, query)
                .orElseGet(() -> rankByRetrievalPipeline(user, query));

        return ranked.stream()
                .map(result -> persistAndConvert(user, result, RecommendationSource.NATURAL_LANGUAGE, query))
                .toList();
    }

    /**
     * Deterministic fallback for explicit queries (used when the agent can't
     * complete): LLM-suggested + provider-grounded seeds blended with
     * semantic/lexical catalog retrieval, then query-first rerank.
     */
    private List<RankedResult> rankByRetrievalPipeline(User user, String query) {
        QueryIntent intent = queryIntentService.extract(query);
        List<UserGenreAffinity> genreAffinities = tasteProfileService.getGenreAffinities(user);
        List<UserThemeAffinity> themeAffinities = tasteProfileService.getThemeAffinities(user);

        List<Book> seeds = suggestAndImport(query, genreAffinities);

        List<ScoredCandidate> candidates = candidateRetrievalService.getQueryCandidates(user, intent, query, seeds);
        if (candidates.size() < MIN_LOCAL_CANDIDATES) {
            importRelevantBooks(query, intent);
            candidates = candidateRetrievalService.getQueryCandidates(user, intent, query, seeds);
        }

        return recommendationRerankingService.rerankForQuery(
                user, candidates, genreAffinities, themeAffinities, query);
    }

    /**
     * Asks the LLM to name real books that satisfy the request, then grounds
     * each against Open Library (importing it) so only verifiable books become
     * candidates. Returns the resolved Book entities to seed retrieval.
     */
    private List<Book> suggestAndImport(String query, List<UserGenreAffinity> genreAffinities) {
        List<String> topGenres = genreAffinities.stream()
                .limit(3)
                .map(a -> a.getGenre().getName())
                .toList();

        List<BookSuggestionService.Suggestion> suggestions =
                bookSuggestionService.suggest(query, topGenres, MAX_SUGGESTIONS);
        if (suggestions.isEmpty()) {
            return List.of();
        }

        List<Book> seeds = new ArrayList<>();
        for (BookSuggestionService.Suggestion s : suggestions) {
            if (seeds.size() >= MAX_SUGGESTIONS) break;
            try {
                bookService.importByTitleAuthor(s.title(), s.author()).ifPresent(seeds::add);
            } catch (Exception e) {
                log.warn("Could not resolve suggested book \"{}\": {}", s.title(), e.getMessage());
            }
        }
        log.info("Find-a-book: LLM suggested {} titles, grounded {} to real books for \"{}\"",
                suggestions.size(), seeds.size(), query);
        return seeds;
    }

    /**
     * On-demand catalog growth for explicit queries: searches the external
     * provider (Open Library) and imports matches so a request for a real book
     * not yet in Novi returns something. Prefers the specific titles the reader
     * referenced ("similar to X") plus the raw request, capped to bound cost.
     * Best-effort: a provider failure must not break the recommendation request.
     */
    private void importRelevantBooks(String query, QueryIntent intent) {
        List<String> searches = new ArrayList<>();
        for (String title : intent.similarTo()) {
            if (title != null && !title.isBlank() && !searches.contains(title)) {
                searches.add(title);
            }
        }
        if (query != null && !query.isBlank() && !searches.contains(query)) {
            searches.add(query);
        }

        int done = 0;
        for (String term : searches) {
            if (done >= MAX_IMPORT_SEARCHES) break;
            try {
                int imported = bookService.search(term).size();
                log.info("Find-a-book: imported {} external results for \"{}\"", imported, term);
            } catch (Exception e) {
                log.warn("Find-a-book external import failed for \"{}\": {}", term, e.getMessage());
            }
            done++;
        }
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

        // Feedback is itself a taste signal: mark the profile stale so the next
        // recommendation request reflects it.
        tasteProfileService.markStale(user);
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
