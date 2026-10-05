package com.novi.service;

import com.novi.config.AiProperties;
import com.novi.entity.Book;
import com.novi.entity.Genre;
import com.novi.entity.User;
import com.novi.entity.UserGenreAffinity;
import com.novi.repository.BookRepository;
import com.novi.repository.UserBookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Narrows the full catalog down to a manageable candidate set before any LLM
 * call is made - per the brief, the entire book database is never sent to an
 * LLM.
 *
 * <p>When the user has a taste vector and books have embeddings, retrieval is
 * done in the database via pgvector's index-backed nearest-neighbour search
 * (cosine distance / HNSW), so we never scan the whole catalog. If no embedded
 * candidates exist yet (e.g. a fresh catalog, or the embedding provider is
 * unconfigured), it falls back to a genre-affinity overlap heuristic over a
 * bounded catalog scan, so recommendations still work without the AI providers.
 */
@Service
@RequiredArgsConstructor
public class CandidateRetrievalService {

    private final BookRepository bookRepository;
    private final UserBookRepository userBookRepository;
    private final BookEmbeddingService bookEmbeddingService;
    private final EmbeddingService embeddingService;
    private final TasteProfileService tasteProfileService;
    private final AiProperties aiProperties;
    private final PgVectorSupport pgVectorSupport;

    // For "Find Your Next Read", a candidate's relevance blends semantic
    // similarity to the request with how many of the request's concrete terms
    // its metadata matches. Semantic is weighted a little higher when present.
    private static final double SEMANTIC_WEIGHT = 0.6;
    private static final double LEXICAL_WEIGHT = 0.4;
    private static final int PER_TERM_LIMIT = 40;

    public record ScoredCandidate(Book book, double baselineScore) {}

    /**
     * "Recommended for You" retrieval: a candidate pool selected purely from the
     * reader's long-term taste (taste vector ANN, or genre-affinity overlap).
     * No explicit query is involved - this is the personalized mode.
     */
    public List<ScoredCandidate> getCandidates(User user) {
        Set<Long> ownedBookIds = ownedBookIds(user);

        Optional<float[]> tasteVector = tasteProfileService.getTasteVector(user);

        // Primary path: index-backed ANN retrieval via pgvector, only when the
        // native vector column is present (otherwise fall through to the scan).
        if (tasteVector.isPresent() && pgVectorSupport.isAvailable()) {
            List<ScoredCandidate> viaVector = retrieveByVector(tasteVector.get(), ownedBookIds);
            if (!viaVector.isEmpty()) {
                return viaVector;
            }
        }

        // Fallback: bounded catalog scan scored by genre-affinity overlap.
        return retrieveByGenreOverlap(user, ownedBookIds);
    }

    /**
     * Retrieves the nearest candidates to the taste vector using pgvector, then
     * computes the exact cosine similarity in-app for the baseline score the
     * reranker orders by. Over-fetches by the number of owned books so filtering
     * them out still leaves a full candidate pool.
     */
    private List<ScoredCandidate> retrieveByVector(float[] tasteVector, Set<Long> ownedBookIds) {
        int poolSize = aiProperties.getCandidatePoolSize();
        int fetch = poolSize + ownedBookIds.size();
        List<Book> nearest = bookRepository.findNearestByTasteVector(VectorUtils.toJson(tasteVector), fetch);

        return nearest.stream()
                .filter(book -> !ownedBookIds.contains(book.getId()))
                .map(book -> new ScoredCandidate(book, bookEmbeddingService.getVector(book)
                        .map(v -> VectorUtils.cosineSimilarity(v, tasteVector))
                        .orElse(0.0)))
                .limit(poolSize)
                .toList();
    }

    private List<ScoredCandidate> retrieveByGenreOverlap(User user, Set<Long> ownedBookIds) {
        // Bound the working set in the database rather than loading the whole
        // catalog and filtering in memory.
        Pageable scanLimit = PageRequest.of(0, Math.max(1, aiProperties.getMaxScanBooks()));
        List<Book> pool = ownedBookIds.isEmpty()
                ? bookRepository.findScoringCandidates(scanLimit)
                : bookRepository.findScoringCandidatesExcluding(ownedBookIds, scanLimit);

        Map<Long, Double> genreAffinity = tasteProfileService.getGenreAffinities(user).stream()
                .collect(Collectors.toMap(a -> a.getGenre().getId(), UserGenreAffinity::getScore));

        return pool.stream()
                .map(book -> new ScoredCandidate(book, scoreByGenreOverlap(book, genreAffinity)))
                .sorted(Comparator.comparingDouble(ScoredCandidate::baselineScore).reversed())
                .limit(aiProperties.getCandidatePoolSize())
                .toList();
    }

    private double scoreByGenreOverlap(Book book, Map<Long, Double> genreAffinity) {
        if (book.getGenres().isEmpty() || genreAffinity.isEmpty()) return 0.0;
        double sum = 0;
        for (Genre g : book.getGenres()) {
            sum += genreAffinity.getOrDefault(g.getId(), 0.5); // 0.5 = neutral prior for an unseen genre
        }
        return sum / book.getGenres().size();
    }

    /**
     * "Find Your Next Read" retrieval: a candidate pool selected by RELEVANCE TO
     * THE REQUEST, not the reader's taste. This is the fix for over-personalized
     * explicit queries - a request like "Chinese books" must retrieve books that
     * actually match "Chinese", regardless of what the reader usually reads.
     *
     * <p>Hybrid retrieval:
     * <ol>
     *   <li>Semantic - embed the (intent-expanded) request and ANN-search the
     *       catalog via pgvector (when embeddings are configured).</li>
     *   <li>Lexical - one keyword search per concrete intent term against
     *       title/description/author/genre/theme; matching more terms scores
     *       higher.</li>
     *   <li>Raw-query fallback - if nothing matched (e.g. no embeddings and no
     *       extracted terms), search the raw query text directly.</li>
     * </ol>
     * The reader's own books are always excluded. Personalization is applied
     * later, in the reranker, only as a secondary tie-breaker.
     */
    public List<ScoredCandidate> getQueryCandidates(User user, QueryIntent intent, String rawQuery) {
        return getQueryCandidates(user, intent, rawQuery, List.of());
    }

    /**
     * Same as {@link #getQueryCandidates(User, QueryIntent, String)} but with a
     * set of seed books - typically LLM-suggested titles already resolved and
     * imported from the metadata provider - which are injected as strong,
     * query-relevant candidates (the model named them specifically for this
     * request). They're merged with the semantic/lexical results and the
     * reranker makes the final call.
     */
    public List<ScoredCandidate> getQueryCandidates(User user, QueryIntent intent, String rawQuery, List<Book> seedBooks) {
        Set<Long> ownedBookIds = ownedBookIds(user);
        int poolSize = aiProperties.getCandidatePoolSize();

        Map<Long, Double> scoreById = new HashMap<>();
        Map<Long, Book> bookById = new HashMap<>();

        // 0. AI-suggested, provider-grounded books: strongest signal, since the
        //    model named them specifically to satisfy this request.
        for (Book b : seedBooks) {
            if (b != null && !ownedBookIds.contains(b.getId())) {
                accumulate(scoreById, bookById, b, 1.0);
            }
        }

        // 1. Semantic retrieval against the embedded request.
        if (embeddingService.isAvailable() && pgVectorSupport.isAvailable()) {
            embeddingService.embed(intent.expandedQueryText(rawQuery)).ifPresent(queryVec -> {
                List<Book> nearest = bookRepository.findNearestByVector(
                        VectorUtils.toJson(queryVec), poolSize + ownedBookIds.size());
                for (Book b : nearest) {
                    if (ownedBookIds.contains(b.getId())) continue;
                    double sim = bookEmbeddingService.getVector(b)
                            .map(v -> VectorUtils.cosineSimilarity(v, queryVec))
                            .orElse(0.0);
                    accumulate(scoreById, bookById, b, SEMANTIC_WEIGHT * Math.max(0.0, sim));
                }
            });
        }

        // 2. Lexical retrieval: one search per concrete request term.
        List<String> terms = intent.retrievalTerms();
        if (!terms.isEmpty()) {
            Pageable limit = PageRequest.of(0, PER_TERM_LIMIT);
            double maxTerms = terms.size();
            for (String term : terms) {
                String pattern = "%" + term.toLowerCase(Locale.ROOT) + "%";
                for (Book b : bookRepository.searchByTerm(pattern, limit)) {
                    if (ownedBookIds.contains(b.getId())) continue;
                    accumulate(scoreById, bookById, b, LEXICAL_WEIGHT * (1.0 / maxTerms));
                }
            }
        }

        // 3. Raw-query fallback when neither path produced anything.
        if (bookById.isEmpty() && rawQuery != null && !rawQuery.isBlank()) {
            Pageable limit = PageRequest.of(0, poolSize);
            String pattern = "%" + rawQuery.toLowerCase(Locale.ROOT) + "%";
            for (Book b : bookRepository.searchByTerm(pattern, limit)) {
                if (ownedBookIds.contains(b.getId())) continue;
                accumulate(scoreById, bookById, b, 0.5);
            }
        }

        return scoreById.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(poolSize)
                .map(e -> new ScoredCandidate(bookById.get(e.getKey()), e.getValue()))
                .toList();
    }

    private void accumulate(Map<Long, Double> scoreById, Map<Long, Book> bookById, Book book, double delta) {
        bookById.putIfAbsent(book.getId(), book);
        scoreById.merge(book.getId(), delta, Double::sum);
    }

    private Set<Long> ownedBookIds(User user) {
        return userBookRepository.findByUser(user).stream()
                .map(ub -> ub.getBook().getId())
                .collect(Collectors.toSet());
    }
}
