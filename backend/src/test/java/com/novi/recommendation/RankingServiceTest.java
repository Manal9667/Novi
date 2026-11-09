package com.novi.recommendation;

import com.novi.config.AiProperties;
import com.novi.entity.Book;
import com.novi.entity.User;
import com.novi.entity.UserBook;
import com.novi.repository.UserBookRepository;
import com.novi.service.BookEmbeddingService;
import com.novi.service.CandidateRetrievalService.ScoredCandidate;
import com.novi.service.QueryIntent;
import com.novi.service.RankingService;
import com.novi.service.RankingService.RankedBook;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.novi.support.TestBooks.book;
import static com.novi.support.TestBooks.genre;
import static com.novi.support.TestBooks.set;
import static com.novi.support.TestBooks.theme;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RankingServiceTest {

    private final UserBookRepository userBookRepository = mock(UserBookRepository.class);
    private final BookEmbeddingService bookEmbeddingService = mock(BookEmbeddingService.class);
    private final AiProperties props = new AiProperties();
    private final RankingService service = new RankingService(userBookRepository, bookEmbeddingService, props);

    private static QueryIntent intentWithKeywords(String... keywords) {
        return new QueryIntent(List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(keywords), null);
    }

    @Test
    void metadataMatchIsFractionOfTermsPresent() {
        Book b = book(1, "The Silent Patient", set(genre(1, "Psychological Thriller")), set(theme(1, "unreliable narrator")));

        double match = service.metadataMatch(b, List.of("psychological thriller", "unreliable narrator", "romance"));

        assertThat(match).isCloseTo(2.0 / 3.0, within(1e-9));
    }

    @Test
    void semanticSimilarityIsZeroWithoutQueryVector() {
        Book b = book(1, "Any", set(genre(1, "Mystery")), set());
        assertThat(service.semanticSimilarity(b, Optional.empty())).isZero();
    }

    @Test
    void ranksByRelevanceDescendingAndTrimsToRecommendationCount() {
        props.setRecommendationCount(2);
        QueryIntent intent = intentWithKeywords("psychological", "thriller");

        Book strong = book(1, "Strong", set(genre(1, "Psychological Thriller")), set(theme(1, "tense"))); // both terms
        Book weak = book(2, "Weak", set(genre(2, "Thriller")), set(theme(2, "tense")));                    // one term
        Book none = book(3, "None", set(genre(3, "Romance")), set(theme(3, "love")));                      // zero terms

        List<ScoredCandidate> candidates = List.of(
                new ScoredCandidate(none, 0.1),
                new ScoredCandidate(strong, 0.1),
                new ScoredCandidate(weak, 0.1));

        List<RankedBook> ranked = service.rank(candidates, intent, Optional.empty(), null);

        assertThat(ranked).hasSize(2);
        assertThat(ranked.get(0).book().getTitle()).isEqualTo("Strong");
        assertThat(ranked.get(1).book().getTitle()).isEqualTo("Weak");
        assertThat(ranked.get(0).score()).isGreaterThan(ranked.get(1).score());
    }

    @Test
    void dedupesRepeatedBooks() {
        QueryIntent intent = intentWithKeywords("mystery");
        Book b = book(1, "Dup", set(genre(1, "Mystery")), set());
        List<ScoredCandidate> candidates = List.of(new ScoredCandidate(b, 0.5), new ScoredCandidate(b, 0.5));

        List<RankedBook> ranked = service.rank(candidates, intent, Optional.empty(), null);

        assertThat(ranked).hasSize(1);
    }

    @Test
    void preferenceBonusLiftsBooksMatchingSavedGenres() {
        QueryIntent intent = intentWithKeywords("adventure"); // a shared theme, so metadata ties

        var fantasy = genre(10, "Fantasy");
        var mystery = genre(20, "Mystery");
        var adventure = theme(30, "adventure");

        Book preferred = book(1, "Preferred", set(fantasy), set(adventure)); // shares Fantasy with saved
        Book other = book(2, "Other", set(mystery), set(adventure));

        User user = User.builder().id(1L).username("u").build();
        Book savedBook = book(99, "Saved", set(fantasy), set());
        UserBook saved = UserBook.builder().user(user).book(savedBook).build();
        when(userBookRepository.findByUser(user)).thenReturn(new ArrayList<>(List.of(saved)));

        List<ScoredCandidate> candidates = List.of(
                new ScoredCandidate(other, 0.1),
                new ScoredCandidate(preferred, 0.1));

        List<RankedBook> ranked = service.rank(candidates, intent, Optional.empty(), user);

        assertThat(ranked.get(0).book().getTitle()).isEqualTo("Preferred");
        assertThat(ranked.get(0).preference()).isGreaterThan(ranked.get(1).preference());
    }
}
