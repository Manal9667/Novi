package com.novi.service;

import com.novi.config.AiProperties;
import com.novi.entity.Author;
import com.novi.entity.Book;
import com.novi.entity.Genre;
import com.novi.entity.Theme;
import com.novi.entity.User;
import com.novi.entity.UserBook;
import com.novi.repository.UserBookRepository;
import com.novi.service.CandidateRetrievalService.ScoredCandidate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Step 3 of the RAG pipeline: a transparent, deterministic hybrid ranking.
 *
 * <p>Each candidate's relevance is a weighted blend of three interpretable
 * signals, with weights configurable under {@code novi.ai.ranking}:
 * <ul>
 *   <li><b>semantic</b> — cosine similarity between the query embedding and the
 *       book embedding;</li>
 *   <li><b>metadata</b> — the share of the request's concrete terms (genres,
 *       themes, authors, cultural context) that the book's metadata matches;</li>
 *   <li><b>preference</b> — a bonus for overlap with the genres/themes of the
 *       signed-in reader's saved books (0 for anonymous readers).</li>
 * </ul>
 * Semantic and metadata form the request-relevance (normalized to sum to 1);
 * preference is applied as a bounded bonus so a strong request match still
 * scores highly for a signed-out reader. No LLM is involved, so the final
 * relevance score is reproducible and explainable.
 */
@Service
@RequiredArgsConstructor
public class RankingService {

    private final UserBookRepository userBookRepository;
    private final BookEmbeddingService bookEmbeddingService;
    private final AiProperties aiProperties;

    /** A ranked book with its final relevance (0..1) and the component breakdown (for transparency/tests). */
    public record RankedBook(Book book, double score, double semantic, double metadata, double preference) {}

    public List<RankedBook> rank(List<ScoredCandidate> candidates, QueryIntent intent,
                                 Optional<float[]> queryVec, User user) {
        if (candidates.isEmpty()) {
            return List.of();
        }

        List<String> terms = intent == null ? List.of() : intent.retrievalTerms();
        PreferenceProfile prefs = buildPreferenceProfile(user);

        AiProperties.Ranking w = aiProperties.getRanking();
        double relevanceDenom = w.getSemanticWeight() + w.getMetadataWeight();
        if (relevanceDenom <= 0) relevanceDenom = 1.0;

        List<RankedBook> ranked = new ArrayList<>(candidates.size());
        Set<Long> seen = new HashSet<>(); // defensive dedup
        for (ScoredCandidate c : candidates) {
            Book book = c.book();
            if (!seen.add(book.getId())) continue;

            double semantic = semanticSimilarity(book, queryVec);
            double metadata = metadataMatch(book, terms);
            double preference = prefs.score(book);

            double relevance = (w.getSemanticWeight() * semantic + w.getMetadataWeight() * metadata) / relevanceDenom;

            double score = prefs.isEmpty()
                    ? relevance
                    : (1.0 - w.getPreferenceWeight()) * relevance + w.getPreferenceWeight() * preference;

            ranked.add(new RankedBook(book, clamp01(score), semantic, metadata, preference));
        }

        ranked.sort(Comparator.comparingDouble(RankedBook::score).reversed());
        int limit = Math.max(1, aiProperties.getRecommendationCount());
        return ranked.size() > limit ? new ArrayList<>(ranked.subList(0, limit)) : ranked;
    }

    /** Cosine similarity of the book to the query embedding, clamped to [0,1]; 0 when embeddings are unavailable. */
    public double semanticSimilarity(Book book, Optional<float[]> queryVec) {
        if (queryVec.isEmpty()) {
            return 0.0;
        }
        return bookEmbeddingService.getVector(book)
                .map(v -> Math.max(0.0, VectorUtils.cosineSimilarity(v, queryVec.get())))
                .orElse(0.0);
    }

    /** Fraction of the request's concrete terms that appear in the book's title/authors/genres/themes. */
    public double metadataMatch(Book book, List<String> terms) {
        if (terms == null || terms.isEmpty()) {
            return 0.0;
        }
        Set<String> haystack = new HashSet<>();
        haystack.add(safeLower(book.getTitle()));
        for (Author a : book.getAuthors()) haystack.add(safeLower(a.getName()));
        for (Genre g : book.getGenres()) haystack.add(safeLower(g.getName()));
        for (Theme t : book.getThemes()) haystack.add(safeLower(t.getName()));
        String joined = String.join(" | ", haystack);

        int matched = 0;
        for (String term : terms) {
            String t = safeLower(term);
            if (!t.isBlank() && joined.contains(t)) {
                matched++;
            }
        }
        return (double) matched / terms.size();
    }

    private PreferenceProfile buildPreferenceProfile(User user) {
        if (user == null) {
            return PreferenceProfile.EMPTY;
        }
        List<UserBook> saved = userBookRepository.findByUser(user);
        if (saved.isEmpty()) {
            return PreferenceProfile.EMPTY;
        }
        Set<Long> genreIds = new HashSet<>();
        Set<Long> themeIds = new HashSet<>();
        for (UserBook ub : saved) {
            for (Genre g : ub.getBook().getGenres()) genreIds.add(g.getId());
            for (Theme t : ub.getBook().getThemes()) themeIds.add(t.getId());
        }
        return new PreferenceProfile(genreIds, themeIds);
    }

    private static double clamp01(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }

    private static String safeLower(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT);
    }

    /** The genres/themes a reader has shown preference for via their saved books. */
    private record PreferenceProfile(Set<Long> genreIds, Set<Long> themeIds) {
        static final PreferenceProfile EMPTY = new PreferenceProfile(Set.of(), Set.of());

        boolean isEmpty() {
            return genreIds.isEmpty() && themeIds.isEmpty();
        }

        /** Share of the book's genres+themes that the reader has previously saved. */
        double score(Book book) {
            int total = book.getGenres().size() + book.getThemes().size();
            if (total == 0 || isEmpty()) {
                return 0.0;
            }
            int overlap = 0;
            for (Genre g : book.getGenres()) if (genreIds.contains(g.getId())) overlap++;
            for (Theme t : book.getThemes()) if (themeIds.contains(t.getId())) overlap++;
            return (double) overlap / total;
        }
    }
}
