package com.novi.service;

import com.novi.dto.common.PageResponse;
import com.novi.dto.review.ReviewResponse;
import com.novi.entity.Book;
import com.novi.entity.Review;
import com.novi.entity.User;
import com.novi.exception.ForbiddenException;
import com.novi.exception.ResourceNotFoundException;
import com.novi.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookService bookService;
    private final TasteProfileService tasteProfileService;

    @Transactional
    public ReviewResponse create(User user, Long bookId, String content) {
        Book book = bookService.getEntityById(bookId);

        reviewRepository.findByUserAndBook(user, book).ifPresent(r -> {
            throw new com.novi.exception.DuplicateResourceException("You have already reviewed this book. Edit your existing review instead.");
        });

        Review review = Review.builder().user(user).book(book).content(content).build();
        ReviewResponse response = toResponse(reviewRepository.save(review));
        tasteProfileService.markStale(user);
        return response;
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
        tasteProfileService.markStale(user);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getForBook(Long bookId, Pageable pageable) {
        Book book = bookService.getEntityById(bookId);
        Page<Review> reviews = reviewRepository.findByBook(book, pageable);
        return PageResponse.from(reviews.map(this::toResponse));
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
