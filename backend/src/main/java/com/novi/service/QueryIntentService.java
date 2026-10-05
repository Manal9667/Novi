package com.novi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns a free-text "Find Your Next Read" request into a structured
 * {@link QueryIntent} that drives query-relevant retrieval. Uses Gemini (via
 * {@link LlmClient}) when configured; otherwise degrades to a deterministic
 * keyword extraction so explicit-query search still works with no AI provider.
 *
 * <p>This is deliberately the ONLY place that interprets the request, so the
 * retrieval and reranking layers stay provider-agnostic.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QueryIntentService {

    private static final String SYSTEM_PROMPT = """
            You convert a reader's natural-language book request into a compact
            JSON search intent. Extract ONLY what the request actually implies -
            never invent constraints. Respond with ONLY a JSON object (no prose,
            no markdown fences) shaped exactly like:
            {
              "genres": [],            // e.g. ["historical fiction","fantasy"]
              "themes": [],            // e.g. ["family","identity","political intrigue"]
              "culturalContext": [],   // nationalities/cultures/settings, e.g. ["Chinese"]
              "authors": [],           // specific authors named in the request
              "similarTo": [],         // book or author titles the reader wants something like
              "moods": [],             // e.g. ["dark","uplifting","fast-paced"]
              "keywords": [],          // other salient nouns/terms from the request
              "length": null           // "short" | "long" | null
            }
            Use lowercase for genres/themes/moods. Keep arrays empty when nothing applies.
            """;

    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;

    public QueryIntent extract(String query) {
        if (query == null || query.isBlank()) {
            return QueryIntent.keywordFallback(query);
        }

        if (llmClient.isAvailable()) {
            try {
                var raw = llmClient.complete(SYSTEM_PROMPT, "Request: \"" + query + "\"", 400);
                if (raw.isPresent()) {
                    QueryIntent parsed = parse(raw.get());
                    // If the model returned something usable, prefer it; otherwise
                    // fall through to keywords so retrieval still has terms.
                    if (parsed != null && !parsed.isEmpty()) {
                        return parsed;
                    }
                }
            } catch (Exception e) {
                log.warn("Query intent extraction failed, using keyword fallback: {}", e.getMessage());
            }
        }

        return QueryIntent.keywordFallback(query);
    }

    private QueryIntent parse(String raw) {
        try {
            JsonNode node = objectMapper.readTree(LlmClient.stripJsonFences(raw));
            String length = node.path("length").isNull() ? null : node.path("length").asText(null);
            return new QueryIntent(
                    strings(node, "genres"),
                    strings(node, "themes"),
                    strings(node, "culturalContext"),
                    strings(node, "authors"),
                    strings(node, "similarTo"),
                    strings(node, "moods"),
                    strings(node, "keywords"),
                    length
            );
        } catch (Exception e) {
            log.warn("Could not parse query intent JSON: {}", e.getMessage());
            return null;
        }
    }

    private List<String> strings(JsonNode node, String field) {
        JsonNode arr = node.path(field);
        if (!arr.isArray()) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        arr.forEach(n -> {
            String v = n.asText(null);
            if (v != null && !v.isBlank()) {
                out.add(v.trim());
            }
        });
        return out;
    }
}
