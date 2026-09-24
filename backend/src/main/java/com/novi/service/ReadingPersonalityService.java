package com.novi.service;

import com.novi.dto.recommendation.AffinityEntry;
import com.novi.dto.recommendation.ReadingPersonalityResponse;
import com.novi.entity.User;
import com.novi.entity.UserGenreAffinity;
import com.novi.entity.UserThemeAffinity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Surfaces what Novi has learned about a reader: the top genre/theme
 * affinities for a bar-chart style UI, plus a short natural-language summary.
 * The summary is written by Claude when available; otherwise a simple
 * template keeps the feature functional without an AI provider configured.
 */
@Service
@RequiredArgsConstructor
public class ReadingPersonalityService {

    private static final String SYSTEM_PROMPT = """
            You are Novi, a book recommendation platform. Given a reader's
            genre and theme affinity scores (0 = dislikes, 1 = loves), write a
            warm, specific, 2-3 sentence natural-language summary of their
            reading personality. Reference the actual top genres/themes by
            name. Do not invent facts not implied by the scores. Respond with
            plain text only - no markdown, no JSON.
            """;

    private final TasteProfileService tasteProfileService;
    private final AnthropicClient anthropicClient;

    @Transactional
    public ReadingPersonalityResponse getReadingPersonality(User user) {
        // Only recompute when a taste signal has changed since the last build.
        tasteProfileService.recomputeIfStale(user);

        List<UserGenreAffinity> genreAffinities = tasteProfileService.getGenreAffinities(user);
        List<UserThemeAffinity> themeAffinities = tasteProfileService.getThemeAffinities(user);

        List<AffinityEntry> genres = genreAffinities.stream()
                .limit(8)
                .map(a -> new AffinityEntry(a.getGenre().getName(), a.getScore()))
                .toList();

        List<AffinityEntry> themes = themeAffinities.stream()
                .limit(8)
                .map(a -> new AffinityEntry(a.getTheme().getName(), a.getScore()))
                .toList();

        String summary = generateSummary(genres, themes);

        return new ReadingPersonalityResponse(genres, themes, summary);
    }

    private String generateSummary(List<AffinityEntry> genres, List<AffinityEntry> themes) {
        if (genres.isEmpty() && themes.isEmpty()) {
            return "Rate a few books to start building your reading personality.";
        }

        if (anthropicClient.isAvailable()) {
            String prompt = "Genre affinities: " + describe(genres) + "\nTheme affinities: " + describe(themes);
            var aiSummary = anthropicClient.complete(SYSTEM_PROMPT, prompt, 200);
            if (aiSummary.isPresent()) {
                return aiSummary.get().trim();
            }
        }

        // Deterministic fallback if the AI provider isn't configured.
        String topGenre = genres.isEmpty() ? null : genres.get(0).name();
        String topTheme = themes.isEmpty() ? null : themes.get(0).name();
        if (topGenre != null && topTheme != null) {
            return "You tend to gravitate toward " + topGenre + " with strong interest in " + topTheme.toLowerCase() + " themes.";
        } else if (topGenre != null) {
            return "You tend to gravitate toward " + topGenre + ".";
        }
        return "Your reading personality is still taking shape - keep rating books to refine it.";
    }

    private String describe(List<AffinityEntry> entries) {
        return entries.stream()
                .map(e -> e.name() + ": " + String.format("%.2f", e.score()))
                .collect(Collectors.joining(", "));
    }
}
