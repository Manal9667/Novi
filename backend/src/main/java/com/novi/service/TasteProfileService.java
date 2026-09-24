package com.novi.service;

import com.novi.entity.*;
import com.novi.entity.enums.FeedbackType;
import com.novi.entity.enums.ReadingStatus;
import com.novi.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

/**
 * Builds the dynamic, interpretable representation of what a user likes, from
 * every signal Phase 1 already collects: ratings, reading status, DNFs, and
 * (from this phase on) recommendation feedback. This is the single source of
 * truth the candidate-retrieval and reranking steps read from - it never talks
 * to an LLM itself, so it stays fast, deterministic, and cheap to recompute.
 *
 * Signal strengths follow the brief directly:
 *   5-star completed book   -> strong positive ( +1.0)
 *   4-star completed book   -> positive         (+0.6)
 *   3-star completed book   -> neutral           (0.0, no signal)
 *   2-star completed book   -> negative          (-0.6)
 *   1-star completed book   -> strong negative   (-1.0)
 *   DNF                     -> negative          (-0.5)
 *   Want-to-read only       -> weak positive     (+0.2)
 *   Recommendation feedback -> INTERESTED (+0.3), NOT_FOR_ME (-0.4)
 */
@Service
@RequiredArgsConstructor
public class TasteProfileService {

    private final UserBookRepository userBookRepository;
    private final RatingRepository ratingRepository;
    private final ReviewRepository reviewRepository;
    private final ReadingHistoryRepository readingHistoryRepository;
    private final RecommendationFeedbackRepository recommendationFeedbackRepository;
    private final UserGenreAffinityRepository userGenreAffinityRepository;
    private final UserThemeAffinityRepository userThemeAffinityRepository;
    private final UserRepository userRepository;
    private final BookEmbeddingService bookEmbeddingService;

    private record WeightedBook(Book book, double weight) {}

    /**
     * Recomputes the profile only if it has been marked stale (or was never
     * computed). This is what the read paths (recommendations, reading
     * personality) call, so a page view with no new signal is essentially free
     * instead of triggering a full delete/reinsert of every affinity row.
     */
    @Transactional
    public void recomputeIfStale(User user) {
        if (user.isTasteProfileStale() || user.getTasteVectorUpdatedAt() == null) {
            recompute(user);
        }
    }

    /**
     * Flags the profile for recomputation on the next read. Called from the
     * write paths that change taste signals (ratings, library changes, reviews,
     * recommendation feedback). Cheap: a single-row update, and a no-op when the
     * profile is already known to be stale.
     */
    @Transactional
    public void markStale(User user) {
        if (!user.isTasteProfileStale()) {
            user.setTasteProfileStale(true);
            userRepository.save(user);
        }
    }

    @Transactional
    public void recompute(User user) {
        List<WeightedBook> signals = collectSignals(user);

        Map<Genre, Double> genreScores = new HashMap<>();
        Map<Theme, Double> themeScores = new HashMap<>();

        for (WeightedBook wb : signals) {
            Book book = wb.book();
            double weight = wb.weight();
            if (weight == 0.0) continue;

            if (!book.getGenres().isEmpty()) {
                double perGenre = weight / book.getGenres().size();
                for (Genre g : book.getGenres()) {
                    genreScores.merge(g, perGenre, Double::sum);
                }
            }
            if (!book.getThemes().isEmpty()) {
                double perTheme = weight / book.getThemes().size();
                for (Theme t : book.getThemes()) {
                    themeScores.merge(t, perTheme, Double::sum);
                }
            }
        }

        userGenreAffinityRepository.deleteByUser(user);
        userGenreAffinityRepository.flush();
        genreScores.forEach((genre, raw) -> userGenreAffinityRepository.save(
                UserGenreAffinity.builder().user(user).genre(genre).score(sigmoid(raw)).build()));

        userThemeAffinityRepository.deleteByUser(user);
        userThemeAffinityRepository.flush();
        themeScores.forEach((theme, raw) -> userThemeAffinityRepository.save(
                UserThemeAffinity.builder().user(user).theme(theme).score(sigmoid(raw)).build()));

        // Taste vector: weighted average embedding of positively-signaled books only.
        List<float[]> vectors = new ArrayList<>();
        List<Double> weights = new ArrayList<>();
        for (WeightedBook wb : signals) {
            if (wb.weight() <= 0) continue;
            bookEmbeddingService.getVector(wb.book()).ifPresent(v -> {
                vectors.add(v);
                weights.add(wb.weight());
            });
        }

        if (!vectors.isEmpty()) {
            float[] tasteVector = VectorUtils.weightedAverage(vectors, weights);
            user.setTasteVector(tasteVector != null ? VectorUtils.toJson(tasteVector) : null);
        } else {
            user.setTasteVector(null);
        }
        user.setTasteVectorUpdatedAt(Instant.now());
        user.setTasteProfileStale(false);
        userRepository.save(user);
    }

    public Optional<float[]> getTasteVector(User user) {
        return Optional.ofNullable(VectorUtils.fromJson(user.getTasteVector()));
    }

    public List<UserGenreAffinity> getGenreAffinities(User user) {
        return userGenreAffinityRepository.findByUserOrderByScoreDesc(user);
    }

    public List<UserThemeAffinity> getThemeAffinities(User user) {
        return userThemeAffinityRepository.findByUserOrderByScoreDesc(user);
    }

    private List<WeightedBook> collectSignals(User user) {
        List<WeightedBook> signals = new ArrayList<>();

        Map<Long, ReadingHistoryEvent> latestHistoryByBook = new HashMap<>();
        for (ReadingHistoryEvent event : readingHistoryRepository.findByUserOrderByOccurredAtDesc(user)) {
            latestHistoryByBook.putIfAbsent(event.getBook().getId(), event);
        }

        // Fetch all of the user's ratings once, keyed by book, rather than
        // querying per library entry (was an N+1 over the whole library).
        Map<Long, Rating> ratingsByBookId = new HashMap<>();
        for (Rating rating : ratingRepository.findByUser(user)) {
            ratingsByBookId.put(rating.getBook().getId(), rating);
        }

        for (UserBook ub : userBookRepository.findByUser(user)) {
            Book book = ub.getBook();
            Rating rating = ratingsByBookId.get(book.getId());

            double weight;
            if (rating != null) {
                weight = switch (rating.getStars()) {
                    case 5 -> 1.0;
                    case 4 -> 0.6;
                    case 3 -> 0.0;
                    case 2 -> -0.6;
                    default -> -1.0; // 1 star
                };
            } else if (ub.getStatus() == ReadingStatus.DNF) {
                weight = -0.5;
            } else if (ub.getStatus() == ReadingStatus.WANT_TO_READ) {
                weight = 0.2;
            } else {
                weight = 0.0; // currently reading with no rating yet: no signal
            }

            signals.add(new WeightedBook(book, weight));
        }

        for (Review review : reviewRepository.findByUserOrderByCreatedAtDesc(user)) {
            // A review proves meaningful engagement, but does not imply sentiment.
            signals.add(new WeightedBook(review.getBook(), 0.15));
        }

        for (ReadingHistoryEvent event : latestHistoryByBook.values()) {
            double weight = switch (event.getEventType()) {
                case FINISHED_READING -> 0.4;
                case MARKED_DNF -> -0.5;
                case STARTED_READING -> 0.1;
                case ADDED_TO_LIBRARY -> 0.1;
                case STATUS_CHANGED -> 0.0;
                case REMOVED_FROM_LIBRARY -> -0.1;
            };
            if (weight != 0.0) {
                signals.add(new WeightedBook(event.getBook(), weight));
            }
        }

        for (RecommendationFeedback feedback : recommendationFeedbackRepository.findByUser(user)) {
            Book book = feedback.getRecommendation().getBook();
            double weight = switch (feedback.getFeedbackType()) {
                case INTERESTED -> 0.3;
                case NOT_FOR_ME -> -0.4;
                case ADDED_TO_WANT_TO_READ -> 0.0; // already captured via the library signal above
            };
            if (weight != 0.0) {
                signals.add(new WeightedBook(book, weight));
            }
        }

        return signals;
    }

    /** Squashes an unbounded accumulated score into an interpretable 0..1 affinity. */
    private double sigmoid(double raw) {
        return 1.0 / (1.0 + Math.exp(-raw));
    }
}
