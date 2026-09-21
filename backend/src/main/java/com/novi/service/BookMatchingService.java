package com.novi.service;

import com.novi.entity.Book;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Resolves free-text a vision model read off a book cover/spine to a real
 * catalog {@link Book}, via the same external metadata provider the rest of
 * Novi uses. Returns the best match together with a confidence score derived
 * from how closely the provider result matches the detected text - so the
 * scanner can flag uncertain matches instead of adding them blindly.
 */
@Service
@RequiredArgsConstructor
public class BookMatchingService {

    private final OpenLibraryService openLibraryService;
    private final BookService bookService;

    /** A resolved match. {@code book} is null when nothing plausible was found. */
    public record MatchResult(Book book, double confidence) {
        public static MatchResult none() {
            return new MatchResult(null, 0.0);
        }
    }

    @Transactional
    public MatchResult match(String detectedTitle, String detectedAuthor) {
        String query = (detectedTitle + " " + (detectedAuthor == null ? "" : detectedAuthor)).trim();
        List<OpenLibraryService.ExternalBook> results = openLibraryService.search(query, 5);
        if (results.isEmpty()) {
            return MatchResult.none();
        }

        OpenLibraryService.ExternalBook best = null;
        double bestScore = -1.0;
        for (OpenLibraryService.ExternalBook candidate : results) {
            double score = score(detectedTitle, detectedAuthor, candidate);
            if (score > bestScore) {
                bestScore = score;
                best = candidate;
            }
        }

        if (best == null) {
            return MatchResult.none();
        }
        Book book = bookService.importIfNeeded(best);
        return new MatchResult(book, round(bestScore));
    }

    /**
     * Confidence that {@code candidate} is the detected book. Title similarity
     * dominates (0.7); author similarity contributes the rest (0.3) but only
     * when we actually detected an author, so a missing author never penalizes
     * an otherwise-strong title match.
     */
    private double score(String detectedTitle, String detectedAuthor, OpenLibraryService.ExternalBook candidate) {
        double titleScore = TextSimilarity.ratio(detectedTitle, candidate.title());

        boolean haveAuthor = detectedAuthor != null && !detectedAuthor.isBlank()
                && candidate.authorNames() != null && !candidate.authorNames().isEmpty();
        if (!haveAuthor) {
            return titleScore;
        }

        double authorScore = candidate.authorNames().stream()
                .mapToDouble(name -> TextSimilarity.ratio(detectedAuthor, name))
                .max()
                .orElse(0.0);
        return (titleScore * 0.7) + (authorScore * 0.3);
    }

    private static double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
