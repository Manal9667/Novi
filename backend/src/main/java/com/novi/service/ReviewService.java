package com.novi.service;

import com.novi.dto.review.ReviewResponse;
import com.novi.entity.Book;
import com.novi.entity.Review;
import com.novi.entity.User;
import com.novi.exception.ForbiddenException;
import com.novi.exception.ResourceNotFoundException;
import com.novi.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookService bookService;

    @Transactional
    public ReviewResponse create(User user, Long bookId, String content) {
        Book book = bookService.getEntityById(bookId);

        reviewRepository.findByUserAndBook(user, book).ifPresent(r -> {
            throw new com.novi.exception.DuplicateResourceException("You have already reviewed this book. Edit your existing review instead.");
        });

        Review review = Review.builder().user(user).book(book).content(content).build();
        return toResponse(reviewRepository.save(review));
    }

    @Transactional
    public ReviewResponse update(User user, Long reviewId, String content) {
        Review review = getOwnedReview(user, reviewId);
        review.setContent(content);
        return toResponse(reviewRepository.save(review));
    }

    @Transactional
    public void delete(User user, Long reviewId) {
        Review review = getOwnedReview(user, reviewId);
        reviewRepository.delete(review);
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> getForBook(Long bookId) {
        Book book = bookService.getEntityById(bookId);
        return reviewRepository.findByBookOrderByCreatedAtDesc(book).stream().map(this::toResponse).toList();
    }

    private Review getOwnedReview(User user, Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review " + reviewId + " not found"));
        if (!review.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("You can only edit or delete your own reviews");
        }
        return review;
    }

    private ReviewResponse toResponse(Review r) {
        return new ReviewResponse(
                r.getId(),
                r.getBook().getId(),
                r.getUser().getId(),
                r.getUser().getUsername(),
                r.getUser().getDisplayName(),
                r.getContent(),
                r.getCreatedAt(),
                r.getUpdatedAt()
        );
    }
}
