package com.novi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novi.entity.Author;
import com.novi.entity.Book;
import com.novi.entity.Genre;
import com.novi.entity.Theme;
import com.novi.service.RankingService.RankedBook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Step 4 of the RAG pipeline: generate a short, grounded explanation for why
 * each ranked book matches the reader's request.
 *
 * <p>The explanations are produced by Gemini but strictly grounded: the prompt
 * supplies only the retrieved books' real metadata (title, authors, genres,
 * themes, description) and forbids inventing any facts. If Gemini is
 * unconfigured, times out, or returns malformed/partial output, each affected
 * book falls back to a deterministic sentence built from its own real metadata,
 * so a recommendation always has an explanation and never a fabricated one.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExplanationService {

    private static final int MAX_DESCRIPTION_CHARS = 320;
    private static final int MAX_TOKENS = 1200;

    private static final String SYSTEM_PROMPT = """
            You are Novi, a knowledgeable book curator. For each candidate book you are
            given the reader's request and the book's REAL metadata, write one or two
            sentences explaining why the book fits the request.

            Respond with ONLY a JSON array (no prose, no markdown fences), shaped exactly:
            [{"bookId": 123, "explanation": "..."}]

            Strict rules:
            - Ground every explanation ONLY in the metadata provided for that book. NEVER
              invent plot points, characters, authors, awards, dates, or any fact not given.
            - Refer to how the book matches the reader's request (genre, mood, theme, style).
            - Keep each explanation to 1-2 sentences, warm and specific, no spoilers.
            - Include exactly one entry per bookId provided.
            """;

    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;

    /**
     * @return a map of bookId -> explanation covering every ranked book (AI
     *         result where available, deterministic fallback otherwise).
     */
    public Map<Long, String> explain(String query, List<RankedBook> ranked) {
        Map<Long, String> out = new HashMap<>();
        if (ranked == null || ranked.isEmpty()) {
            return out;
        }

        if (llmClient.isAvailable()) {
            try {
                tryAiExplain(query, ranked, out);
            } catch (Exception e) {
                log.warn("Explanation generation failed, using deterministic fallback: {}", e.getMessage());
            }
        }

        // Fill any book the model didn't cover (or all of them, if AI is off/failed).
        for (RankedBook rb : ranked) {
            out.putIfAbsent(rb.book().getId(), fallbackExplanation(rb.book()));
        }
        return out;
    }

    private void tryAiExplain(String query, List<RankedBook> ranked, Map<Long, String> out) {
        String prompt = buildPrompt(query, ranked);
        llmClient.complete(SYSTEM_PROMPT, prompt, MAX_TOKENS).ifPresent(raw -> {
            try {
                JsonNode array = objectMapper.readTree(LlmClient.stripJsonFences(raw));
                if (!array.isArray()) {
                    return;
                }
                // Only accept ids we actually asked about (guards against hallucinated ids).
                java.util.Set<Long> validIds = new java.util.HashSet<>();
                for (RankedBook rb : ranked) validIds.add(rb.book().getId());

                for (JsonNode node : array) {
                    long bookId = node.path("bookId").asLong(-1);
                    String explanation = node.path("explanation").asText("").trim();
                    if (validIds.contains(bookId) && !explanation.isBlank()) {
                        out.put(bookId, explanation);
                    }
                }
            } catch (Exception e) {
                log.warn("Could not parse explanation JSON: {}", e.getMessage());
            }
        });
    }

    private String buildPrompt(String query, List<RankedBook> ranked) {
        StringBuilder sb = new StringBuilder();
        sb.append("Reader's request: \"").append(query == null ? "" : query.trim()).append("\"\n\n");
        sb.append("Candidate books:\n");
        for (RankedBook rb : ranked) {
            Book b = rb.book();
            sb.append("- bookId=").append(b.getId())
                    .append("; title=\"").append(b.getTitle()).append("\"")
                    .append("; authors=").append(join(b.getAuthors().stream().map(Author::getName).toList()))
                    .append("; genres=").append(join(b.getGenres().stream().map(Genre::getName).toList()))
                    .append("; themes=").append(join(b.getThemes().stream().map(Theme::getName).toList()))
                    .append("; description=\"").append(truncate(b.getDescription())).append("\"\n");
        }
        sb.append("\nReturn the JSON array now.");
        return sb.toString();
    }

    /** Honest, metadata-grounded explanation when the AI explainer is unavailable. */
    String fallbackExplanation(Book book) {
        List<String> genres = book.getGenres().stream().map(Genre::getName).limit(2).toList();
        List<String> themes = book.getThemes().stream().map(Theme::getName).limit(2).toList();

        if (!genres.isEmpty() && !themes.isEmpty()) {
            return "A " + String.join(" / ", genres).toLowerCase(java.util.Locale.ROOT)
                    + " pick, with themes of " + String.join(" and ", themes)
                    + " that line up with what you described.";
        }
        if (!genres.isEmpty()) {
            return "A " + String.join(" / ", genres).toLowerCase(java.util.Locale.ROOT)
                    + " pick that fits what you're looking for.";
        }
        if (!themes.isEmpty()) {
            return "Explores " + String.join(" and ", themes) + ", matching what you described.";
        }
        return "A close match for what you described.";
    }

    private static String join(List<String> values) {
        return values.isEmpty() ? "(none)" : String.join(", ", values);
    }

    private static String truncate(String s) {
        if (s == null) return "";
        String t = s.strip();
        return t.length() <= MAX_DESCRIPTION_CHARS ? t : t.substring(0, MAX_DESCRIPTION_CHARS) + "...";
    }
}
