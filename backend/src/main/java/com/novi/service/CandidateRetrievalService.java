package com.novi.service;

import com.novi.config.AiProperties;
import com.novi.entity.Book;
import com.novi.entity.Genre;
import com.novi.entity.User;
import com.novi.entity.UserGenreAffinity;
import com.novi.repository.BookRepository;
import com.novi.repository.UserBookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Narrows the full catalog down to a manageable candidate set before any LLM
 * call is made - per the brief, the entire book database is never sent to an
 * LLM. Candidates are scored by cosine similarity to the user's taste vector
 * when embeddings are available, falling back to a genre-affinity overlap
 * heuristic otherwise, so recommendations still work even with the AI
 * providers unconfigured.
 */
@Service
@RequiredArgsConstructor
public class CandidateRetrievalService {

    private final BookRepository bookRepository;
    private final UserBookRepository userBookRepository;
    private final BookEmbeddingService bookEmbeddingService;
    private final TasteProfileService tasteProfileService;
    private final AiProperties aiProperties;

    public record ScoredCandidate(Book book, double baselineScore) {}

    public List<ScoredCandidate> getCandidates(User user) {
        Set<Long> ownedBookIds = userBookRepository.findByUser(user).stream()
                .map(ub -> ub.getBook().getId())
                .collect(Collectors.toSet());

        List<Book> pool = bookRepository.findAll().stream()
                .filter(b -> !ownedBookIds.contains(b.getId()))
                .toList();

        Optional<float[]> tasteVector = tasteProfileService.getTasteVector(user);
        Map<Long, Double> genreAffinity = tasteProfileService.getGenreAffinities(user).stream()
            .collect(Collectors.toMap(a -> a.getGenre().getId(), UserGenreAffinity::getScore));

        List<ScoredCandidate> scored;
        if (tasteVector.isPresent()) {
            scored = pool.stream()
                .map(book -> new ScoredCandidate(book, scoreByEmbeddingOrGenre(book, tasteVector.get(), genreAffinity)))
                    .toList();
        } else {
            scored = pool.stream()
                    .map(book -> new ScoredCandidate(book, scoreByGenreOverlap(book, genreAffinity)))
                    .toList();
        }

        return scored.stream()
                .sorted(Comparator.comparingDouble(ScoredCandidate::baselineScore).reversed())
                .limit(aiProperties.getCandidatePoolSize())
                .toList();
    }

    private double scoreByEmbedding(Book book, float[] tasteVector) {
        return bookEmbeddingService.getVector(book)
                .map(v -> VectorUtils.cosineSimilarity(v, tasteVector))
                .orElse(0.0);
    }

    private double scoreByEmbeddingOrGenre(Book book, float[] tasteVector, Map<Long, Double> genreAffinity) {
        Optional<float[]> bookVector = bookEmbeddingService.getVector(book);
        return bookVector.isPresent()
                ? VectorUtils.cosineSimilarity(bookVector.get(), tasteVector)
                : scoreByGenreOverlap(book, genreAffinity);
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
