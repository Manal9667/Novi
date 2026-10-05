package com.novi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.novi.config.AiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Thin wrapper around Google Gemini via its OpenAI-compatible endpoint
 * ({@code https://generativelanguage.googleapis.com/v1beta/openai/chat/completions}).
 * Every AI feature in Novi that needs reasoning (reranking, explanations,
 * natural-language query interpretation, theme tagging, reading-personality
 * summaries) or vision (the book scanner) goes through this one class, so the
 * model/auth logic lives in exactly one place and the provider can be swapped
 * without touching callers.
 *
 * <p>The endpoint is OpenAI-shaped: Bearer auth, a {@code messages} array, and
 * multimodal {@code image_url} parts with inline {@code data:} URLs - the format
 * Google documents for this compatibility layer.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LlmClient {

    private final RestClient geminiRestClient;
    private final AiProperties aiProperties;

    public boolean isAvailable() {
        return aiProperties.getGemini().isConfigured();
    }

    /**
     * Sends a single user-turn message (with an optional system prompt) and
     * returns the text of the response, or empty on any failure. Never throws -
     * callers should always have a non-AI fallback.
     */
    public Optional<String> complete(String systemPrompt, String userMessage, int maxTokens) {
        if (!isAvailable()) {
            return Optional.empty();
        }

        try {
            List<Map<String, Object>> messages = new ArrayList<>();
            if (systemPrompt != null) {
                messages.add(Map.of("role", "system", "content", systemPrompt));
            }
            messages.add(Map.of("role", "user", "content", userMessage));

            return send(buildBody(messages, maxTokens));
        } catch (Exception e) {
            log.warn("Gemini API call failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Same as {@link #complete} but includes an image in the user turn, using
     * the OpenAI-style multimodal content parts (an {@code image_url} part with
     * an inline {@code data:} URL) - the format Google documents for its
     * OpenAI-compatible endpoint. Backs the Phase 3 book-scanner. Never throws.
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
            Map<String, Object> textPart = Map.of("type", "text", "text", userText);
            Map<String, Object> imagePart = Map.of(
                    "type", "image_url",
                    "image_url", Map.of("url", "data:" + mediaType + ";base64," + base64Image));

            List<Map<String, Object>> messages = new ArrayList<>();
            if (systemPrompt != null) {
                messages.add(Map.of("role", "system", "content", systemPrompt));
            }
            messages.add(Map.of("role", "user", "content", List.of(textPart, imagePart)));

            return send(buildBody(messages, maxTokens));
        } catch (Exception e) {
            log.warn("Gemini vision API call failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private Map<String, Object> buildBody(List<Map<String, Object>> messages, int maxTokens) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", aiProperties.getGemini().getModel());
        body.put("max_tokens", maxTokens);
        body.put("messages", messages);
        return body;
    }

    private Optional<String> send(Map<String, Object> body) {
        JsonNode response = geminiRestClient.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + aiProperties.getGemini().getApiKey())
                .body(body)
                .retrieve()
                .body(JsonNode.class);

        if (response == null) {
            return Optional.empty();
        }

        JsonNode content = response.path("choices").path(0).path("message").path("content");
        if (content.isMissingNode() || content.isNull()) {
            return Optional.empty();
        }

        String text = content.asText("").trim();
        return text.isEmpty() ? Optional.empty() : Optional.of(text);
    }

    /**
     * Multi-turn, tool-calling chat used by the recommendation agent. Sends the
     * full running {@code messages} transcript plus the available {@code tools}
     * (OpenAI function-tool specs) and returns the assistant's reply message
     * node - which contains either {@code tool_calls} (the model wants to run a
     * tool) or {@code content} (a final answer). The caller owns the loop:
     * execute the tools, append their results as {@code role:"tool"} messages,
     * and call again. Returns empty on any failure so the caller can fall back.
     *
     * @param messages the running OpenAI-format transcript (system/user/assistant/tool)
     * @param tools    OpenAI tool specs ({@code [{type:"function", function:{...}}]})
     */
    public Optional<JsonNode> chat(List<Map<String, Object>> messages, List<Map<String, Object>> tools, int maxTokens) {
        if (!isAvailable()) {
            return Optional.empty();
        }

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("model", aiProperties.getGemini().getModel());
            body.put("max_tokens", maxTokens);
            body.put("messages", messages);
            if (tools != null && !tools.isEmpty()) {
                body.put("tools", tools);
                body.put("tool_choice", "auto");
            }

            JsonNode response = geminiRestClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + aiProperties.getGemini().getApiKey())
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

            if (response == null) {
                return Optional.empty();
            }
            JsonNode message = response.path("choices").path(0).path("message");
            return message.isMissingNode() || message.isNull() ? Optional.empty() : Optional.of(message);
        } catch (Exception e) {
            log.warn("Gemini tool-chat call failed: {}", e.getMessage());
            return Optional.empty();
        }
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
