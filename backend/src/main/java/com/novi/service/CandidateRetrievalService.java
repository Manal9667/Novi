package com.novi.service;

import com.novi.config.AiProperties;
import com.novi.entity.Book;
import com.novi.entity.User;
import com.novi.repository.BookRepository;
import com.novi.repository.UserBookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Step 2 of the RAG pipeline: retrieve candidate books relevant to the reader's
 * request. Hybrid retrieval blends semantic similarity (embed the request and
 * ANN-search pgvector) with lexical matching (one keyword search per concrete
 * intent term), over-fetching more candidates than are ultimately shown so the
 * ranking step has room to work.
 */
@Service
@RequiredArgsConstructor
public class CandidateRetrievalService {

    private final BookRepository bookRepository;
    private final UserBookRepository userBookRepository;
    private final BookEmbeddingService bookEmbeddingService;
    private final AiProperties aiProperties;
    private final PgVectorSupport pgVectorSupport;

    // A candidate's baseline relevance blends semantic similarity to the request
    // with how many of the request's concrete terms its metadata matches.
    private static final double SEMANTIC_WEIGHT = 0.6;
    private static final double LEXICAL_WEIGHT = 0.4;
    private static final int PER_TERM_LIMIT = 40;

    /** A retrieved book with its baseline retrieval relevance (semantic + lexical). */
    public record ScoredCandidate(Book book, double baselineScore) {}

    /**
     * Retrieve a candidate pool by RELEVANCE TO THE REQUEST.
     *
     * <ol>
     *   <li>Semantic — embed the (intent-expanded) request and ANN-search the
     *       catalog via pgvector (when embeddings are configured).</li>
     *   <li>Lexical — one keyword search per concrete intent term against
     *       title/description/author/genre/theme; matching more terms scores
     *       higher.</li>
     *   <li>Raw-query fallback — if nothing matched, search the raw query text.</li>
     * </ol>
     * Books already in the signed-in reader's library are excluded; an anonymous
     * reader ({@code user == null}) excludes nothing.
     */
    public List<ScoredCandidate> retrieve(User user, QueryIntent intent, String rawQuery, Optional<float[]> queryVec) {
        Set<Long> ownedBookIds = ownedBookIds(user);
        int poolSize = aiProperties.getCandidatePoolSize();

        Map<Long, Double> scoreById = new HashMap<>();
        Map<Long, Book> bookById = new HashMap<>();

        // 1. Semantic retrieval against the (precomputed) embedded request.
        if (queryVec.isPresent() && pgVectorSupport.isAvailable()) {
            float[] qv = queryVec.get();
            List<Book> nearest = bookRepository.findNearestByVector(
                    VectorUtils.toJson(qv), poolSize + ownedBookIds.size());
            for (Book b : nearest) {
                if (ownedBookIds.contains(b.getId())) continue;
                double sim = bookEmbeddingService.getVector(b)
                        .map(v -> VectorUtils.cosineSimilarity(v, qv))
                        .orElse(0.0);
                accumulate(scoreById, bookById, b, SEMANTIC_WEIGHT * Math.max(0.0, sim));
            }
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
        if (user == null) {
            return Set.of();
        }
        return userBookRepository.findByUser(user).stream()
                .map(ub -> ub.getBook().getId())
                .collect(Collectors.toSet());
    }
}
