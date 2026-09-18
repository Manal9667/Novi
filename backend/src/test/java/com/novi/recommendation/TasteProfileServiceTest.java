package com.novi.recommendation;

import com.novi.entity.*;
import com.novi.entity.enums.ReadingStatus;
import com.novi.repository.*;
import com.novi.service.BookEmbeddingService;
import com.novi.service.TasteProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TasteProfileServiceTest {

    @Mock private UserBookRepository userBookRepository;
    @Mock private RatingRepository ratingRepository;
    @Mock private ReviewRepository reviewRepository;
    @Mock private ReadingHistoryRepository readingHistoryRepository;
    @Mock private RecommendationFeedbackRepository recommendationFeedbackRepository;
    @Mock private UserGenreAffinityRepository userGenreAffinityRepository;
    @Mock private UserThemeAffinityRepository userThemeAffinityRepository;
    @Mock private UserRepository userRepository;
    @Mock private BookEmbeddingService bookEmbeddingService;

    @InjectMocks
    private TasteProfileService tasteProfileService;

    private User user;
    private Genre sciFi;
    private Book fiveStarBook;
    private Book dnfBook;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).username("reader").build();
        sciFi = Genre.builder().id(1L).name("Science Fiction").build();

        fiveStarBook = Book.builder().id(10L).title("Dune").build();
        fiveStarBook.getGenres().add(sciFi);

        dnfBook = Book.builder().id(11L).title("Foundation").build();
        dnfBook.getGenres().add(sciFi);

        when(recommendationFeedbackRepository.findByUser(user)).thenReturn(List.of());
        when(reviewRepository.findByUserOrderByCreatedAtDesc(user)).thenReturn(List.of());
        when(readingHistoryRepository.findByUserOrderByOccurredAtDesc(user)).thenReturn(List.of());
    }

    @Test
    void recompute_ratedFiveStarBook_producesHighGenreAffinity() {
        UserBook ub = UserBook.builder().id(100L).user(user).book(fiveStarBook).status(ReadingStatus.READ).build();
        when(userBookRepository.findByUser(user)).thenReturn(List.of(ub));
        when(ratingRepository.findByUserAndBook(user, fiveStarBook))
                .thenReturn(Optional.of(Rating.builder().stars(5).build()));
        when(bookEmbeddingService.getVector(any())).thenReturn(Optional.empty());

        tasteProfileService.recompute(user);

        ArgumentCaptor<UserGenreAffinity> captor = ArgumentCaptor.forClass(UserGenreAffinity.class);
        verify(userGenreAffinityRepository).save(captor.capture());

        UserGenreAffinity saved = captor.getValue();
        assertThat(saved.getGenre()).isEqualTo(sciFi);
        assertThat(saved.getScore()).isGreaterThan(0.5); // strong positive signal -> above-neutral affinity
    }

    @Test
    void recompute_dnfBook_producesBelowNeutralGenreAffinity() {
        UserBook ub = UserBook.builder().id(101L).user(user).book(dnfBook).status(ReadingStatus.DNF).build();
        when(userBookRepository.findByUser(user)).thenReturn(List.of(ub));
        when(ratingRepository.findByUserAndBook(user, dnfBook)).thenReturn(Optional.empty());

        tasteProfileService.recompute(user);

        ArgumentCaptor<UserGenreAffinity> captor = ArgumentCaptor.forClass(UserGenreAffinity.class);
        verify(userGenreAffinityRepository).save(captor.capture());

        assertThat(captor.getValue().getScore()).isLessThan(0.5);
    }

    @Test
    void recompute_alwaysClearsExistingAffinitiesFirst() {
        when(userBookRepository.findByUser(user)).thenReturn(List.of());

        tasteProfileService.recompute(user);

        verify(userGenreAffinityRepository).deleteByUser(user);
        verify(userThemeAffinityRepository).deleteByUser(user);
    }
}
