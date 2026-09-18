package com.novi.controller;

import com.novi.dto.review.ReviewRequest;
import com.novi.dto.review.ReviewResponse;
import com.novi.security.CurrentUserProvider;
import com.novi.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping("/api/books/{bookId}/reviews")
    public ResponseEntity<ReviewResponse> create(@PathVariable Long bookId, @Valid @RequestBody ReviewRequest request) {
        ReviewResponse response = reviewService.create(currentUserProvider.getCurrentUser(), bookId, request.content());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/api/reviews/{id}")
    public ReviewResponse update(@PathVariable Long id, @Valid @RequestBody ReviewRequest request) {
        return reviewService.update(currentUserProvider.getCurrentUser(), id, request.content());
    }

    @DeleteMapping("/api/reviews/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        reviewService.delete(currentUserProvider.getCurrentUser(), id);
        return ResponseEntity.noContent().build();
    }
}
