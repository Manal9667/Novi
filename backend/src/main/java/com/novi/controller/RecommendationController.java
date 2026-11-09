package com.novi.controller;

import com.novi.dto.recommendation.NaturalLanguageRequest;
import com.novi.dto.recommendation.RecommendationResponse;
import com.novi.entity.User;
import com.novi.security.CurrentUserProvider;
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
    private final CurrentUserProvider currentUserProvider;

    /**
     * Natural-language recommendation request, e.g.
     * "a psychological thriller with an unreliable narrator, like The Silent Patient".
     * Public: works signed-out. When a user is signed in, books already in their
     * library are excluded.
     */
    @PostMapping("/ask")
    public List<RecommendationResponse> ask(@Valid @RequestBody NaturalLanguageRequest request) {
        User user = currentUserProvider.getCurrentUserOrNull();
        return recommendationService.recommend(user, request.query());
    }
}
