package com.novi.review;

import com.novi.entity.Book;
import com.novi.entity.Review;
import com.novi.entity.User;
import com.novi.exception.DuplicateResourceException;
import com.novi.exception.ForbiddenException;
import com.novi.repository.ReviewRepository;
import com.novi.service.BookService;
import com.novi.service.ReviewService;
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
class ReviewServiceTest {

    @Mock private ReviewRepository reviewRepository;
    @Mock private BookService bookService;
    @Mock private com.novi.service.TasteProfileService tasteProfileService;

    @InjectMocks
    private ReviewService reviewService;

    private User owner;
    private User otherUser;
    private Book book;

    @BeforeEach
    void setUp() {
        owner = User.builder().id(1L).username("owner").displayName("Owner").build();
        otherUser = User.builder().id(2L).username("other").displayName("Other").build();
        book = Book.builder().id(10L).title("Dune").build();
    }

    @Test
    void create_whenNoExistingReview_savesReview() {
        when(bookService.getEntityById(10L)).thenReturn(book);
        when(reviewRepository.findByUserAndBook(owner, book)).thenReturn(Optional.empty());
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> {
            Review r = inv.getArgument(0);
            r.setId(50L);
            return r;
        });

        var response = reviewService.create(owner, 10L, "Loved it");

        assertThat(response.content()).isEqualTo("Loved it");
    }

    @Test
    void create_whenAlreadyReviewed_throwsDuplicate() {
        when(bookService.getEntityById(10L)).thenReturn(book);
        when(reviewRepository.findByUserAndBook(owner, book)).thenReturn(Optional.of(new Review()));

        assertThatThrownBy(() -> reviewService.create(owner, 10L, "Another review"))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void update_byNonOwner_throwsForbidden() {
        Review review = Review.builder().id(50L).user(owner).book(book).content("Original").build();
        when(reviewRepository.findById(50L)).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.update(otherUser, 50L, "Hacked"))
                .isInstanceOf(ForbiddenException.class);
    }
}
