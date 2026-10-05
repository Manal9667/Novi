package com.novi.service;

import com.novi.dto.book.BookSummaryResponse;
import com.novi.dto.recommendation.ReadingPersonalityResponse;
import com.novi.dto.recommendation.RecommendationResponse;
import com.novi.dto.wrapped.WrappedResponse;
import com.novi.dto.wrapped.WrappedResponse.Achievement;
import com.novi.dto.wrapped.WrappedResponse.MemorableBook;
import com.novi.dto.wrapped.WrappedResponse.NamedCount;
import com.novi.dto.wrapped.WrappedResponse.PersonalityCard;
import com.novi.dto.wrapped.WrappedResponse.YearInBooks;
import com.novi.entity.*;
import com.novi.entity.enums.HistoryEventType;
import com.novi.entity.enums.ReadingStatus;
import com.novi.repository.RatingRepository;
import com.novi.repository.ReadingHistoryRepository;
import com.novi.repository.ReviewRepository;
import com.novi.repository.UserBookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;
import java.time.Year;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds "Novi Wrapped" from the reader's real data. All numeric facts are
 * computed deterministically here; the LLM is used only to narrate those facts
 * (never to invent them). Every section degrades gracefully: a brand-new reader
 * gets a valid, honest Wrapped with empty/omitted sections rather than errors.
 */
@Service
@RequiredArgsConstructor
public class WrappedService {

    private final UserBookRepository userBookRepository;
    private final RatingRepository ratingRepository;
    private final ReviewRepository reviewRepository;
    private final ReadingHistoryRepository readingHistoryRepository;
    private final TasteProfileService tasteProfileService;
    private final ReadingPersonalityService readingPersonalityService;
    private final RecommendationService recommendationService;
    private final LlmClient llmClient;

    private static final int TOP_N = 5;
    private static final int NEXT_CHAPTER_COUNT = 5;

    @Transactional
    public WrappedResponse getWrapped(User user) {
        int year = Year.now().getValue();

        List<UserBook> library = userBookRepository.findByUser(user);
        List<Rating> ratings = ratingRepository.findByUser(user);
        long reviewsCount = reviewRepository.countByUser(user);
        List<ReadingHistoryEvent> history = readingHistoryRepository.findByUserOrderByOccurredAtDesc(user);

        List<UserBook> readBooks = library.stream()
                .filter(ub -> ub.getStatus() == ReadingStatus.READ)
                .toList();

        // ---- Deterministic facts ----
        int booksRead = readBooks.size();
        Integer booksReadThisYear = countFinishedThisYear(history, year);

        Map<String, Integer> genreCounts = countNames(library, ub -> ub.getBook().getGenres().stream().map(Genre::getName).toList());
        Map<String, Integer> authorCounts = countNames(library, ub -> ub.getBook().getAuthors().stream().map(Author::getName).toList());

        int genresExplored = genreCounts.size();
        int authorsRead = authorCounts.size();

        Double averageRating = ratings.isEmpty() ? null
                : Math.round(ratings.stream().mapToInt(Rating::getStars).average().orElse(0) * 10.0) / 10.0;
        Integer ratingsCount = ratings.isEmpty() ? null : ratings.size();
        Integer reviewsCountOrNull = reviewsCount == 0 ? null : (int) reviewsCount;

        YearInBooks yearInBooks = new YearInBooks(
                booksRead, booksReadThisYear, genresExplored, authorsRead,
                averageRating, ratingsCount, reviewsCountOrNull);

        List<NamedCount> topGenres = topCounts(genreCounts);
        List<NamedCount> topAuthors = topCounts(authorCounts);
        List<String> topThemes = tasteProfileService.getThemeAffinities(user).stream()
                .limit(TOP_N).map(a -> a.getTheme().getName()).toList();

        PersonalityCard personality = buildPersonality(user, topGenres, genresExplored);
        MemorableBook mostMemorable = pickMostMemorable(user, ratings, readBooks);
        List<Achievement> achievements = buildAchievements(
                booksRead, genresExplored, authorsRead, ratings, averageRating, reviewsCount);

        String narrative = buildNarrative(year, yearInBooks, topGenres, topAuthors, topThemes);

        List<RecommendationResponse> nextChapter = safeNextChapter(user);

        return new WrappedResponse(
                year, yearInBooks, topGenres, topAuthors, topThemes,
                personality, mostMemorable, achievements, narrative, nextChapter);
    }

    private Integer countFinishedThisYear(List<ReadingHistoryEvent> history, int year) {
        long count = history.stream()
                .filter(e -> e.getEventType() == HistoryEventType.FINISHED_READING)
                .filter(e -> e.getOccurredAt() != null
                        && e.getOccurredAt().atZone(ZoneOffset.UTC).getYear() == year)
                .map(e -> e.getBook().getId())
                .distinct()
                .count();
        return count == 0 ? null : (int) count;
    }

    private Map<String, Integer> countNames(List<UserBook> library, java.util.function.Function<UserBook, List<String>> extractor) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (UserBook ub : library) {
            for (String name : extractor.apply(ub)) {
                if (name != null && !name.isBlank()) {
                    counts.merge(name, 1, Integer::sum);
                }
            }
        }
        return counts;
    }

    private List<NamedCount> topCounts(Map<String, Integer> counts) {
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(TOP_N)
                .map(e -> new NamedCount(e.getKey(), e.getValue()))
                .toList();
    }

    private PersonalityCard buildPersonality(User user, List<NamedCount> topGenres, int genresExplored) {
        ReadingPersonalityResponse personality = readingPersonalityService.getReadingPersonality(user);
        String title;
        if (topGenres.isEmpty()) {
            title = "The Emerging Reader";
        } else if (genresExplored >= 6) {
            title = "The Curious Explorer";
        } else {
            title = "The " + topGenres.get(0).name() + " Devotee";
        }
        return new PersonalityCard(title, personality.summary());
    }

    private MemorableBook pickMostMemorable(User user, List<Rating> ratings, List<UserBook> readBooks) {
        // Prefer the highest-rated book; tie-break by most recently rated.
        Rating best = ratings.stream()
                .max(Comparator.comparingInt(Rating::getStars)
                        .thenComparing(r -> r.getUpdatedAt() == null ? r.getCreatedAt() : r.getUpdatedAt()))
                .orElse(null);
        if (best != null) {
            Book b = best.getBook();
            String note = best.getStars() >= 4
                    ? "One of your highest-rated reads — you gave it " + best.getStars() + " stars."
                    : "You rated it " + best.getStars() + " stars.";
            return new MemorableBook(summary(b), best.getStars(), note);
        }
        // No ratings: fall back to a finished book if there is one.
        if (!readBooks.isEmpty()) {
            Book b = readBooks.get(0).getBook();
            return new MemorableBook(summary(b), null, "A book you finished this year.");
        }
        return null;
    }

    private List<Achievement> buildAchievements(int booksRead, int genresExplored, int authorsRead,
                                                List<Rating> ratings, Double averageRating, long reviewsCount) {
        List<Achievement> achievements = new ArrayList<>();

        if (genresExplored >= 5) {
            achievements.add(new Achievement("Genre Hopper", "You explored " + genresExplored + " different genres."));
        }
        if (booksRead >= 25) {
            achievements.add(new Achievement("Voracious Reader", "You finished " + booksRead + " books. Incredible."));
        } else if (booksRead >= 10) {
            achievements.add(new Achievement("Bibliophile", "You finished " + booksRead + " books."));
        } else if (booksRead >= 5) {
            achievements.add(new Achievement("On a Roll", "You finished " + booksRead + " books."));
        }
        if (authorsRead >= 8) {
            achievements.add(new Achievement("Well-Read", "You read " + authorsRead + " different authors."));
        }
        if (reviewsCount >= 3) {
            achievements.add(new Achievement("Thoughtful Critic", "You wrote " + reviewsCount + " reviews."));
        }
        if (ratings.size() >= 5 && averageRating != null) {
            if (averageRating >= 4.0) {
                achievements.add(new Achievement("Generous Rater", "Your average rating is " + averageRating + " stars."));
            } else if (averageRating <= 2.5) {
                achievements.add(new Achievement("Tough Critic", "Your average rating is " + averageRating + " stars."));
            }
        }
        return achievements;
    }

    private String buildNarrative(int year, YearInBooks y, List<NamedCount> topGenres,
                                  List<NamedCount> topAuthors, List<String> topThemes) {
        String facts = formatFacts(year, y, topGenres, topAuthors, topThemes);

        if (llmClient.isAvailable()) {
            String system = """
                    You are Novi, writing a reader's personal "year in books" blurb.
                    Using ONLY the statistics provided, write 2-4 warm, upbeat sentences
                    summarizing their reading year. Do NOT invent any numbers, titles,
                    genres or facts not present in the input. Plain text only.
                    """;
            var ai = llmClient.complete(system, facts, 220);
            if (ai.isPresent() && !ai.get().isBlank()) {
                return ai.get().trim();
            }
        }

        // Deterministic fallback.
        if (y.booksRead() == 0) {
            return "Your reading story is just getting started. Add and rate a few books, and your Wrapped will fill up.";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("You read ").append(y.booksRead()).append(y.booksRead() == 1 ? " book" : " books");
        if (!topGenres.isEmpty()) {
            sb.append(", gravitating toward ").append(topGenres.get(0).name());
        }
        sb.append(". ");
        if (!topAuthors.isEmpty()) {
            sb.append(topAuthors.get(0).name()).append(" was a favourite author this year. ");
        }
        return sb.toString().trim();
    }

    private String formatFacts(int year, YearInBooks y, List<NamedCount> topGenres,
                               List<NamedCount> topAuthors, List<String> topThemes) {
        StringBuilder sb = new StringBuilder();
        sb.append("Year: ").append(year).append("\n");
        sb.append("Books finished (all time): ").append(y.booksRead()).append("\n");
        if (y.booksReadThisYear() != null) sb.append("Books finished this year: ").append(y.booksReadThisYear()).append("\n");
        sb.append("Genres explored: ").append(y.genresExplored()).append("\n");
        sb.append("Different authors: ").append(y.authorsRead()).append("\n");
        if (y.averageRating() != null) sb.append("Average rating given: ").append(y.averageRating()).append("\n");
        if (y.reviewsCount() != null) sb.append("Reviews written: ").append(y.reviewsCount()).append("\n");
        if (!topGenres.isEmpty()) sb.append("Top genres: ").append(topGenres.stream().map(NamedCount::name).toList()).append("\n");
        if (!topAuthors.isEmpty()) sb.append("Top authors: ").append(topAuthors.stream().map(NamedCount::name).toList()).append("\n");
        if (!topThemes.isEmpty()) sb.append("Recurring themes: ").append(topThemes).append("\n");
        return sb.toString();
    }

    private List<RecommendationResponse> safeNextChapter(User user) {
        try {
            List<RecommendationResponse> recs = recommendationService.getPersonalizedRecommendations(user);
            return recs.size() > NEXT_CHAPTER_COUNT ? recs.subList(0, NEXT_CHAPTER_COUNT) : recs;
        } catch (Exception e) {
            return List.of();
        }
    }

    private BookSummaryResponse summary(Book b) {
        return new BookSummaryResponse(
                b.getId(), b.getTitle(), b.getCoverImageUrl(),
                b.getAuthors().stream().map(Author::getName).toList());
    }
}
