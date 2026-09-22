package com.novi.service;

import com.novi.dto.rating.RatingResponse;
import com.novi.entity.Book;
import com.novi.entity.Rating;
import com.novi.entity.User;
import com.novi.exception.ResourceNotFoundException;
import com.novi.repository.RatingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RatingService {

    private final RatingRepository ratingRepository;
    private final BookService bookService;

    @Transactional
    public RatingResponse rate(User user, Long bookId, int stars) {
        Book book = bookService.getEntityById(bookId);

        Rating rating = ratingRepository.findByUserAndBook(user, book)
                .orElseGet(() -> Rating.builder().user(user).book(book).build());

        rating.setStars(stars);
        rating = ratingRepository.save(rating);

        return toResponse(rating);
    }

    @Transactional
    public void deleteRating(User user, Long bookId) {
        Book book = bookService.getEntityById(bookId);
        ratingRepository.findByUserAndBook(user, book)
                .orElseThrow(() -> new ResourceNotFoundException("You have not rated this book"));
        ratingRepository.deleteByUserAndBook(user, book);
    }

    @Transactional(readOnly = true)
    public RatingResponse getMyRating(User user, Long bookId) {
        Book book = bookService.getEntityById(bookId);
        return ratingRepository.findByUserAndBook(user, book)
                .map(this::toResponse)
                .orElse(null);
    }

    private RatingResponse toResponse(Rating rating) {
        return new RatingResponse(
                rating.getId(),
                rating.getBook().getId(),
                rating.getStars(),
                rating.getCreatedAt(),
                rating.getUpdatedAt()
        );
    }
}
