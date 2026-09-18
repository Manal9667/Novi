package com.novi.rating;

import com.novi.entity.Book;
import com.novi.entity.Rating;
import com.novi.entity.User;
import com.novi.exception.ResourceNotFoundException;
import com.novi.repository.RatingRepository;
import com.novi.service.BookService;
import com.novi.service.RatingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RatingServiceTest {

    @Mock private RatingRepository ratingRepository;
    @Mock private BookService bookService;

    @InjectMocks
    private RatingService ratingService;

    private User user;
    private Book book;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).username("reader").build();
        book = Book.builder().id(10L).title("Dune").build();
    }

    @Test
    void rate_withValidStars_createsNewRating() {
        when(bookService.getEntityById(10L)).thenReturn(book);
        when(ratingRepository.findByUserAndBook(user, book)).thenReturn(Optional.empty());
        when(ratingRepository.save(any(Rating.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = ratingService.rate(user, 10L, 5);

        assertThat(response.stars()).isEqualTo(5);
    }

    @Test
    void rate_whenAlreadyRated_updatesExistingRating() {
        Rating existing = Rating.builder().id(1L).user(user).book(book).stars(3).build();
        when(bookService.getEntityById(10L)).thenReturn(book);
        when(ratingRepository.findByUserAndBook(user, book)).thenReturn(Optional.of(existing));
        when(ratingRepository.save(any(Rating.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = ratingService.rate(user, 10L, 4);

        assertThat(response.stars()).isEqualTo(4);
        verify(ratingRepository, never()).save(argThat(r -> r != existing));
    }

    @Test
    void deleteRating_whenNoneExists_throwsNotFound() {
        when(bookService.getEntityById(10L)).thenReturn(book);
        when(ratingRepository.findByUserAndBook(user, book)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ratingService.deleteRating(user, 10L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
