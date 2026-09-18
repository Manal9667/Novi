package com.novi.controller;

import com.novi.dto.recommendation.FeedbackRequest;
import com.novi.dto.recommendation.NaturalLanguageRequest;
import com.novi.dto.recommendation.ReadingPersonalityResponse;
import com.novi.dto.recommendation.RecommendationResponse;
import com.novi.security.CurrentUserProvider;
import com.novi.service.ReadingPersonalityService;
import com.novi.service.RecommendationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final ReadingPersonalityService readingPersonalityService;
    private final CurrentUserProvider currentUserProvider;

    /** Personalized recommendations generated from the user's full reading profile. */
    @GetMapping
    public List<RecommendationResponse> getRecommendations() {
        return recommendationService.getPersonalizedRecommendations(currentUserProvider.getCurrentUser());
    }

    /** Natural-language recommendation requests, e.g. "something like Dune but shorter". */
    @PostMapping("/ask")
    public List<RecommendationResponse> ask(@Valid @RequestBody NaturalLanguageRequest request) {
        return recommendationService.getRecommendationsForQuery(currentUserProvider.getCurrentUser(), request.query());
    }

    @PostMapping("/{id}/feedback")
    public void giveFeedback(@PathVariable Long id, @Valid @RequestBody FeedbackRequest request) {
        recommendationService.recordFeedback(currentUserProvider.getCurrentUser(), id, request);
    }

    /** "My Reading Personality": interpretable genre/theme affinities plus an AI-written summary. */
    @GetMapping("/reading-personality")
    public ReadingPersonalityResponse getReadingPersonality() {
        return readingPersonalityService.getReadingPersonality(currentUserProvider.getCurrentUser());
    }
}
