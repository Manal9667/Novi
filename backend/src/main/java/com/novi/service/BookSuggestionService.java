package com.novi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Uses Gemini as a book "generator": given a natural-language request, it names
 * REAL, published books that satisfy it (title + author), drawing on the
 * model's world knowledge. This is what lets Novi answer requests that can't be
 * satisfied by matching local metadata - e.g. "a best seller romance" (a
 * popularity concept Novi stores no data for) surfaces actual bestselling
 * titles like <i>The Love Hypothesis</i>.
 *
 * <p>Crucially, the suggestions are only candidate TITLES. They are grounded
 * against the real metadata provider (Open Library) before anything is shown,
 * so a hallucinated title that can't be verified is simply dropped - no
 * fabricated books ever reach the user.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BookSuggestionService {

    public record Suggestion(String title, String author) {}

    private static final String SYSTEM_PROMPT = """
            You are Novi, a well-read book concierge. Given a reader's request,
            name REAL, published books that genuinely satisfy it - use your
            knowledge of actual books, authors, popularity, awards and series.

            Respond with ONLY a JSON array (no prose, no markdown fences) of up
            to %d objects shaped exactly like:
            [{"title": "The Love Hypothesis", "author": "Ali Hazelwood"}]

            Rules:
            - Only real books that actually exist. Never invent titles or authors.
            - Prioritize books that match the request (genre, mood, theme, popularity,
              "similar to", author, setting, length). For "best seller"/"popular"
              requests, name genuinely well-known, widely-read titles.
            - Prefer the most relevant/representative books; variety is good.
            - If the reader's tastes are given, use them only as a gentle tie-breaker
              when the request itself is open-ended.
            """;

    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;

    /**
     * @param readerTopGenres a few of the reader's strongest genres, used only
     *                        as a soft tie-breaker (may be empty)
     * @param limit           maximum number of titles to request
     */
    public List<Suggestion> suggest(String query, List<String> readerTopGenres, int limit) {
        if (query == null || query.isBlank() || !llmClient.isAvailable()) {
            return List.of();
        }

        StringBuilder prompt = new StringBuilder("Reader's request: \"").append(query).append("\"");
        if (readerTopGenres != null && !readerTopGenres.isEmpty()) {
            prompt.append("\nReader also tends to enjoy: ").append(String.join(", ", readerTopGenres));
        }
        prompt.append("\n\nName the real books that best satisfy this request.");

        try {
            var raw = llmClient.complete(String.format(SYSTEM_PROMPT, limit), prompt.toString(), 700);
            if (raw.isEmpty()) {
                return List.of();
            }
            return parse(raw.get(), limit);
        } catch (Exception e) {
            log.warn("Book suggestion generation failed: {}", e.getMessage());
            return List.of();
        }
    }

    private List<Suggestion> parse(String raw, int limit) {
        List<Suggestion> out = new ArrayList<>();
        try {
            JsonNode arr = objectMapper.readTree(LlmClient.stripJsonFences(raw));
            if (!arr.isArray()) {
                return out;
            }
            for (JsonNode node : arr) {
                if (out.size() >= limit) break;
                String title = text(node.get("title"));
                if (title == null || title.isBlank()) continue;
                String author = text(node.get("author"));
                out.add(new Suggestion(title.trim(), author == null ? "" : author.trim()));
            }
        } catch (Exception e) {
            log.warn("Could not parse book suggestions JSON: {}", e.getMessage());
        }
        return out;
    }

    private String text(JsonNode node) {
        return node == null || node.isNull() ? null : node.asText();
    }
}
