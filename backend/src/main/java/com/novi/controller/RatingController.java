package com.novi.controller;

import com.novi.dto.rating.RatingRequest;
import com.novi.dto.rating.RatingResponse;
import com.novi.security.CurrentUserProvider;
import com.novi.service.RatingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/books/{bookId}/ratings")
@RequiredArgsConstructor
public class RatingController {

    private final RatingService ratingService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping("/mine")
    public ResponseEntity<RatingResponse> getMine(@PathVariable Long bookId) {
        RatingResponse response = ratingService.getMyRating(currentUserProvider.getCurrentUser(), bookId);
        return response != null ? ResponseEntity.ok(response) : ResponseEntity.noContent().build();
    }

    @PostMapping
    public RatingResponse rate(@PathVariable Long bookId, @Valid @RequestBody RatingRequest request) {
        return ratingService.rate(currentUserProvider.getCurrentUser(), bookId, request.stars());
    }

    @PutMapping
    public RatingResponse update(@PathVariable Long bookId, @Valid @RequestBody RatingRequest request) {
        return ratingService.rate(currentUserProvider.getCurrentUser(), bookId, request.stars());
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(@PathVariable Long bookId) {
        ratingService.deleteRating(currentUserProvider.getCurrentUser(), bookId);
        return ResponseEntity.noContent().build();
    }
}
