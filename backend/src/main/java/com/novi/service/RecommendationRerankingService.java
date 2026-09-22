package com.novi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novi.config.AiProperties;
import com.novi.entity.*;
import com.novi.service.CandidateRetrievalService.ScoredCandidate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Takes the narrowed candidate set from {@code CandidateRetrievalService} and
 * asks Claude to rank and explain the top matches against the user's taste
 * profile - the step that turns a similarity score into something a person
 * can actually understand ("why am I seeing this?"). Falls back to a
 * deterministic, non-AI ranking (by baseline similarity score) if the
 * Anthropic API isn't configured or the call fails, so the feature degrades
 * rather than breaks.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationRerankingService {

    public record RankedResult(Book book, double matchScore, List<String> reasons, String potentialDownside) {}

    private static final String SYSTEM_PROMPT = """
            You are Novi's book recommendation reranker. You will be given a
            reader's taste profile and a list of candidate books. Select and
            rank the best matches for THIS reader specifically, considering
            genre/theme affinity, historical ratings, tone, pacing and any
            stated preferences or natural-language request.

            Respond with ONLY a JSON array (no prose, no markdown fences),
            ordered best match first, of objects shaped exactly like:
            [{"bookId": 123, "matchPercent": 94, "reasons": ["short reason 1", "short reason 2"], "potentialDownside": "one honest sentence or null"}]

            Rules:
            - matchPercent is an integer 0-100 reflecting genuine confidence, not always high numbers.
            - reasons must reference the reader's ACTUAL profile/history given below - never generic filler.
            - potentialDownside must be an honest, specific caveat, or null if there truly isn't one.
            - Include at most as many results as candidates given, and only include books you'd genuinely recommend.
            """;

    private final AnthropicClient anthropicClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AiProperties aiProperties;

    public List<RankedResult> rerank(User user, List<ScoredCandidate> candidates,
                                      List<UserGenreAffinity> genreAffinities,
                                      List<UserThemeAffinity> themeAffinities,
                                      String naturalLanguageQuery) {
        if (candidates.isEmpty()) {
            return List.of();
        }

        if (anthropicClient.isAvailable()) {
            Optional<List<RankedResult>> aiResult = tryAiRerank(candidates, genreAffinities, themeAffinities, naturalLanguageQuery);
            if (aiResult.isPresent() && !aiResult.get().isEmpty()) {
                return aiResult.get();
            }
        }

        return fallbackRanking(candidates, naturalLanguageQuery);
    }

    private Optional<List<RankedResult>> tryAiRerank(List<ScoredCandidate> candidates,
                                                       List<UserGenreAffinity> genreAffinities,
                                                       List<UserThemeAffinity> themeAffinities,
                                                       String naturalLanguageQuery) {
        String prompt = buildPrompt(candidates, genreAffinities, themeAffinities, naturalLanguageQuery);

        return anthropicClient.complete(SYSTEM_PROMPT, prompt, 2000).flatMap(raw -> {
            try {
                JsonNode array = objectMapper.readTree(AnthropicClient.stripJsonFences(raw));
                if (!array.isArray()) return Optional.empty();

                Map<Long, Book> booksById = candidates.stream()
                        .collect(Collectors.toMap(c -> c.book().getId(), ScoredCandidate::book));

                int limit = aiProperties.getRecommendationCount();
                List<RankedResult> results = new ArrayList<>();
                for (JsonNode node : array) {
                    // The model is asked for the top N ordered best-first; cap
                    // defensively so a chatty response can't return (and persist)
                    // the entire candidate pool.
                    if (results.size() >= limit) break;

                    Long bookId = node.path("bookId").asLong();
                    Book book = booksById.get(bookId);
                    if (book == null) continue; // ignore hallucinated ids defensively

                    List<String> reasons = new ArrayList<>();
                    if (node.has("reasons")) {
                        node.get("reasons").forEach(r -> reasons.add(r.asText()));
                    }
                    String downside = node.path("potentialDownside").isNull() ? null : node.path("potentialDownside").asText(null);

                    // Clamp to [0,100]: a model can return an out-of-range value
                    // (or omit the field, defaulting to 0), which would otherwise
                    // surface to the user as e.g. a "150% match".
                    double matchPercent = node.path("matchPercent").asDouble(0);
                    double clampedPercent = Math.max(0.0, Math.min(100.0, matchPercent));

                    results.add(new RankedResult(book, clampedPercent / 100.0, reasons, downside));
                }
                return results.isEmpty() ? Optional.empty() : Optional.of(results);
            } catch (Exception e) {
                log.warn("Failed to parse AI reranking response: {}", e.getMessage());
                return Optional.empty();
            }
        });
    }

    private String buildPrompt(List<ScoredCandidate> candidates,
                                List<UserGenreAffinity> genreAffinities,
                                List<UserThemeAffinity> themeAffinities,
                                String naturalLanguageQuery) {
        StringBuilder sb = new StringBuilder();

        if (naturalLanguageQuery != null && !naturalLanguageQuery.isBlank()) {
            sb.append("The reader's specific request: \"").append(naturalLanguageQuery).append("\"\n\n");
        }

        sb.append("Reader's genre affinities (0=dislikes, 1=loves):\n");
        genreAffinities.stream().limit(10).forEach(a ->
                sb.append("- ").append(a.getGenre().getName()).append(": ").append(String.format("%.2f", a.getScore())).append("\n"));

        sb.append("\nReader's theme affinities (0=dislikes, 1=loves):\n");
        themeAffinities.stream().limit(10).forEach(a ->
                sb.append("- ").append(a.getTheme().getName()).append(": ").append(String.format("%.2f", a.getScore())).append("\n"));

        sb.append("\nCandidate books (already filtered to exclude books the reader owns):\n");
        for (ScoredCandidate c : candidates) {
            Book book = c.book();
            sb.append("- id=").append(book.getId())
                    .append(", title=\"").append(book.getTitle()).append("\"")
                    .append(", authors=").append(book.getAuthors().stream().map(Author::getName).collect(Collectors.joining(", ")))
                    .append(", genres=").append(book.getGenres().stream().map(Genre::getName).collect(Collectors.joining(", ")))
                    .append(", themes=").append(book.getThemes().stream().map(Theme::getName).collect(Collectors.joining(", ")))
                    .append(", description=\"").append(truncate(book.getDescription(), 300)).append("\"")
                    .append("\n");
        }

        sb.append("\nReturn the top ").append(aiProperties.getRecommendationCount()).append(" best matches as JSON.");
        return sb.toString();
    }

    private List<RankedResult> fallbackRanking(List<ScoredCandidate> candidates, String naturalLanguageQuery) {
        String[] queryTerms = naturalLanguageQuery == null
                ? new String[0]
                : naturalLanguageQuery.toLowerCase(Locale.ROOT).split("\\W+");

        return candidates.stream()
            .sorted(Comparator.comparingDouble((ScoredCandidate candidate) -> fallbackScore(candidate, queryTerms)).reversed())
                .limit(aiProperties.getRecommendationCount())
                .map(c -> new RankedResult(
                        c.book(),
                        c.baselineScore(),
                        List.of("Similar genres and themes to books you've enjoyed"),
                        "AI-generated explanations are unavailable right now - this is a similarity-based match."
                ))
                .toList();
    }

            private double fallbackScore(ScoredCandidate candidate, String[] queryTerms) {
            if (queryTerms.length == 0) return candidate.baselineScore();

            Book book = candidate.book();
            String searchableText = (book.getTitle() + " "
                + book.getDescription() + " "
                + book.getAuthors().stream().map(Author::getName).collect(Collectors.joining(" ")) + " "
                + book.getGenres().stream().map(Genre::getName).collect(Collectors.joining(" ")))
                .toLowerCase(Locale.ROOT);
            long matchingTerms = Arrays.stream(queryTerms)
                .filter(term -> term.length() > 2 && searchableText.contains(term))
                .count();
            return candidate.baselineScore() + matchingTerms * 0.1;
            }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
    }
}
