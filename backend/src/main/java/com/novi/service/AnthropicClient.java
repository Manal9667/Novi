package com.novi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.novi.config.AiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Thin wrapper around the Anthropic Messages API. Every AI feature in Novi
 * that needs reasoning (reranking, explanations, natural-language query
 * interpretation, theme tagging, reading-personality summaries) goes through
 * this one class, so the model/version/auth logic lives in exactly one place.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnthropicClient {

    private final RestClient anthropicRestClient;
    private final AiProperties aiProperties;

    public boolean isAvailable() {
        return aiProperties.getAnthropic().isConfigured();
    }

    /**
     * Sends a single user-turn message (with an optional system prompt) and
     * returns the concatenated text of the response, or empty on any failure.
     * Never throws - callers should always have a non-AI fallback.
     */
    public Optional<String> complete(String systemPrompt, String userMessage, int maxTokens) {
        if (!isAvailable()) {
            return Optional.empty();
        }

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("model", aiProperties.getAnthropic().getModel());
            body.put("max_tokens", maxTokens);
            if (systemPrompt != null) {
                body.put("system", systemPrompt);
            }
            body.put("messages", List.of(Map.of("role", "user", "content", userMessage)));

            return send(body);
        } catch (Exception e) {
            log.warn("Anthropic API call failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Same as {@link #complete} but includes an image in the user turn, using
     * the Anthropic Messages API vision content blocks. Backs the Phase 3
     * book-scanner (title/author extraction from a photo). Never throws.
     *
     * @param base64Image the raw image bytes, base64-encoded (no data: prefix)
     * @param mediaType   e.g. "image/jpeg", "image/png", "image/webp"
     */
    public Optional<String> completeWithImage(String systemPrompt, String userText,
                                              String base64Image, String mediaType, int maxTokens) {
        if (!isAvailable()) {
            return Optional.empty();
        }

        try {
            Map<String, Object> imageBlock = Map.of(
                    "type", "image",
                    "source", Map.of(
                            "type", "base64",
                            "media_type", mediaType,
                            "data", base64Image));
            Map<String, Object> textBlock = Map.of("type", "text", "text", userText);

            Map<String, Object> body = new HashMap<>();
            body.put("model", aiProperties.getAnthropic().getModel());
            body.put("max_tokens", maxTokens);
            if (systemPrompt != null) {
                body.put("system", systemPrompt);
            }
            body.put("messages", List.of(Map.of(
                    "role", "user",
                    "content", List.of(imageBlock, textBlock))));

            return send(body);
        } catch (Exception e) {
            log.warn("Anthropic vision API call failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<String> send(Map<String, Object> body) {
        JsonNode response = anthropicRestClient.post()
                .uri("/messages")
                .header("x-api-key", aiProperties.getAnthropic().getApiKey())
                .body(body)
                .retrieve()
                .body(JsonNode.class);

        if (response == null || !response.has("content")) {
            return Optional.empty();
        }

        StringBuilder text = new StringBuilder();
        for (JsonNode block : response.get("content")) {
            if ("text".equals(block.path("type").asText())) {
                text.append(block.path("text").asText());
            }
        }
        return text.isEmpty() ? Optional.empty() : Optional.of(text.toString());
    }

    /**
     * Strips a fenced ```json code block if the model wrapped its output in
     * one, since we always ask for raw JSON but models sometimes add fences
     * anyway.
     */
    public static String stripJsonFences(String text) {
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```(json)?", "");
            int lastFence = trimmed.lastIndexOf("```");
            if (lastFence >= 0) {
                trimmed = trimmed.substring(0, lastFence);
            }
        }
        return trimmed.trim();
    }
}
