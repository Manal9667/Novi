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
 * LLM (Gemini) isn't configured or the call fails, so the feature degrades
 * rather than breaks.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationRerankingService {

    public record RankedResult(Book book, double matchScore, List<String> reasons, String potentialDownside) {}

    /** Which signal dominates ranking: the reader's profile, or their explicit request. */
    public enum Mode { PROFILE, QUERY }

    private static final String PROFILE_SYSTEM_PROMPT = """
            You are Novi's book recommendation reranker. You will be given a
            reader's taste profile and a list of candidate books. Select and
            rank the best matches for THIS reader specifically, considering
            genre/theme affinity, historical ratings, tone and pacing.

            Respond with ONLY a JSON array (no prose, no markdown fences),
            ordered best match first, of objects shaped exactly like:
            [{"bookId": 123, "matchPercent": 94, "reasons": ["short reason 1", "short reason 2"], "potentialDownside": "one honest sentence or null"}]

            Rules:
            - matchPercent is an integer 0-100 reflecting genuine confidence, not always high numbers.
            - reasons must reference the reader's ACTUAL profile/history given below - never generic filler.
            - potentialDownside must be an honest, specific caveat, or null if there truly isn't one.
            - Include at most as many results as candidates given, and only include books you'd genuinely recommend.
            """;

    private static final String QUERY_SYSTEM_PROMPT = """
            You are Novi's book recommendation reranker for an EXPLICIT reader
            request ("Find Your Next Read"). You will be given the reader's
            request, a secondary personalization signal, and a list of candidate
            books. Rank the candidates by how well they SATISFY THE REQUEST.

            Priority order:
            1. Relevance to the request (genre, cultural context, theme, mood, "similar to", author, length) - this dominates.
            2. Explicit constraints in the request must be respected.
            3. Overall book quality/metadata fit.
            4. Personalization - use ONLY to break ties between books that are equally relevant to the request.

            Respond with ONLY a JSON array (no prose, no markdown fences),
            ordered best match first, of objects shaped exactly like:
            [{"bookId": 123, "matchPercent": 94, "reasons": ["short reason 1", "short reason 2"], "potentialDownside": "one honest sentence or null"}]

            Rules:
            - matchPercent is an integer 0-100 reflecting how well the book fits THE REQUEST, not always high numbers.
            - Each book's reasons must explain how it matches the REQUEST specifically (e.g. "Chinese historical fiction centered on family"), not generic filler.
            - Do NOT include books that clearly do not match the request, even if they fit the reader's taste. Return fewer results rather than padding with irrelevant books.
            - potentialDownside must be an honest, specific caveat, or null if there truly isn't one.
            """;

    private final LlmClient llmClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AiProperties aiProperties;

    /**
     * Personalized ("Recommended for You") rerank: the reader's profile is the
     * primary signal. Also used by unit tests. {@code naturalLanguageQuery} is
     * normally null here.
     */
    public List<RankedResult> rerank(User user, List<ScoredCandidate> candidates,
                                      List<UserGenreAffinity> genreAffinities,
                                      List<UserThemeAffinity> themeAffinities,
                                      String naturalLanguageQuery) {
        return doRerank(Mode.PROFILE, candidates, genreAffinities, themeAffinities, naturalLanguageQuery);
    }

    /**
     * Explicit-query ("Find Your Next Read") rerank: the request is the primary
     * signal and personalization is only a tie-breaker.
     */
    public List<RankedResult> rerankForQuery(User user, List<ScoredCandidate> candidates,
                                             List<UserGenreAffinity> genreAffinities,
                                             List<UserThemeAffinity> themeAffinities,
                                             String naturalLanguageQuery) {
        return doRerank(Mode.QUERY, candidates, genreAffinities, themeAffinities, naturalLanguageQuery);
    }

    private List<RankedResult> doRerank(Mode mode, List<ScoredCandidate> candidates,
                                        List<UserGenreAffinity> genreAffinities,
                                        List<UserThemeAffinity> themeAffinities,
                                        String naturalLanguageQuery) {
        if (candidates.isEmpty()) {
            return List.of();
        }

        if (llmClient.isAvailable()) {
            Optional<List<RankedResult>> aiResult = tryAiRerank(mode, candidates, genreAffinities, themeAffinities, naturalLanguageQuery);
            if (aiResult.isPresent() && !aiResult.get().isEmpty()) {
                log.info("Rerank[{}]: using Gemini AI ranking ({} candidates -> {} results)",
                        mode, candidates.size(), aiResult.get().size());
                return aiResult.get();
            }
            log.info("Rerank[{}]: Gemini unavailable or returned nothing - using deterministic fallback ({} candidates)",
                    mode, candidates.size());
        } else {
            log.info("Rerank[{}]: no LLM configured - using deterministic fallback ({} candidates)",
                    mode, candidates.size());
        }

        return fallbackRanking(mode, candidates, naturalLanguageQuery);
    }

    private Optional<List<RankedResult>> tryAiRerank(Mode mode, List<ScoredCandidate> candidates,
                                                       List<UserGenreAffinity> genreAffinities,
                                                       List<UserThemeAffinity> themeAffinities,
                                                       String naturalLanguageQuery) {
        String systemPrompt = mode == Mode.QUERY ? QUERY_SYSTEM_PROMPT : PROFILE_SYSTEM_PROMPT;
        String prompt = buildPrompt(mode, candidates, genreAffinities, themeAffinities, naturalLanguageQuery);

        return llmClient.complete(systemPrompt, prompt, 2000).flatMap(raw -> {
            try {
                JsonNode array = objectMapper.readTree(LlmClient.stripJsonFences(raw));
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

    private String buildPrompt(Mode mode, List<ScoredCandidate> candidates,
                                List<UserGenreAffinity> genreAffinities,
                                List<UserThemeAffinity> themeAffinities,
                                String naturalLanguageQuery) {
        StringBuilder sb = new StringBuilder();
        boolean queryMode = mode == Mode.QUERY && naturalLanguageQuery != null && !naturalLanguageQuery.isBlank();

        if (queryMode) {
            sb.append("The reader's request: \"").append(naturalLanguageQuery).append("\"\n");
            sb.append("Rank the candidates below by how well they satisfy THIS request.\n\n");
            sb.append("Secondary personalization signal (use ONLY to break ties between equally relevant books):\n");
        } else {
            if (naturalLanguageQuery != null && !naturalLanguageQuery.isBlank()) {
                sb.append("The reader's specific request: \"").append(naturalLanguageQuery).append("\"\n\n");
            }
            sb.append("Reader's genre affinities (0=dislikes, 1=loves):\n");
        }

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

        if (queryMode) {
            sb.append("\nReturn the best matches for the request as JSON (at most ")
              .append(aiProperties.getRecommendationCount())
              .append("). Each book's reasons must explain how it matches the REQUEST.");
        } else {
            sb.append("\nReturn the top ").append(aiProperties.getRecommendationCount()).append(" best matches as JSON.");
        }
        return sb.toString();
    }

    /**
     * Deterministic ranking used only when the LLM is unavailable or fails. The
     * books are still real retrieval results; the explanation is kept honest and
     * mode-appropriate rather than a generic (and, in query mode, misleading)
     * personalization claim.
     */
    private List<RankedResult> fallbackRanking(Mode mode, List<ScoredCandidate> candidates, String naturalLanguageQuery) {
        boolean hasQuery = naturalLanguageQuery != null && !naturalLanguageQuery.isBlank();
        String[] queryTerms = hasQuery
                ? naturalLanguageQuery.toLowerCase(Locale.ROOT).split("\\W+")
                : new String[0];

        // Reason grounded in the actual input: in query mode the pool was
        // retrieved by matching the request, so we say exactly that; in profile
        // mode it was retrieved by taste similarity.
        List<String> reasons = (mode == Mode.QUERY && hasQuery)
                ? List.of("Matches your request for \"" + naturalLanguageQuery.trim() + "\"")
                : List.of("Similar to the genres and themes in your library");
        // Don't fabricate a specific caveat when the AI explainer is offline.
        String downside = null;

        return candidates.stream()
            .sorted(Comparator.comparingDouble((ScoredCandidate candidate) -> fallbackScore(candidate, queryTerms)).reversed())
                .limit(aiProperties.getRecommendationCount())
                .map(c -> new RankedResult(c.book(), c.baselineScore(), reasons, downside))
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
