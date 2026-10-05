package com.novi.dto.wrapped;

import com.novi.dto.book.BookSummaryResponse;
import com.novi.dto.recommendation.RecommendationResponse;

import java.util.List;

/**
 * "Novi Wrapped": a Spotify-Wrapped-style summary of a reader's activity, built
 * entirely from real database data. Every numeric fact is computed
 * deterministically in {@code WrappedService}; only the narrative is AI-written
 * (around those facts). Optional sections are null/empty when the underlying
 * data isn't available, so the UI can hide them rather than show empty metrics.
 */
public record WrappedResponse(
        int year,
        YearInBooks yearInBooks,
        List<NamedCount> topGenres,
        List<NamedCount> topAuthors,
        List<String> topThemes,
        PersonalityCard personality,
        MemorableBook mostMemorable,
        List<Achievement> achievements,
        String narrative,
        List<RecommendationResponse> nextChapter
) {

    /** Headline counts. Nullable fields are omitted by the UI when unavailable. */
    public record YearInBooks(
            int booksRead,
            Integer booksReadThisYear,
            int genresExplored,
            int authorsRead,
            Double averageRating,
            Integer ratingsCount,
            Integer reviewsCount
    ) {}

    public record NamedCount(String name, int count) {}

    /** A playful, data-grounded reading profile - explicitly not a psychological assessment. */
    public record PersonalityCard(String title, String summary) {}

    public record MemorableBook(BookSummaryResponse book, Integer rating, String note) {}

    public record Achievement(String title, String description) {}
}
