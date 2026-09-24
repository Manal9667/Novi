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
    private final TasteProfileService tasteProfileService;
    private final AiProperties aiProperties;
    private final PgVectorSupport pgVectorSupport;

    public record ScoredCandidate(Book book, double baselineScore) {}

    public List<ScoredCandidate> getCandidates(User user) {
        Set<Long> ownedBookIds = userBookRepository.findByUser(user).stream()
                .map(ub -> ub.getBook().getId())
                .collect(Collectors.toSet());

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
}
