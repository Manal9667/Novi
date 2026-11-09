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
 * Produces semantic embeddings with Google's {@code gemini-embedding-001} model
 * through Gemini's OpenAI-compatible {@code /embeddings} endpoint. The output
 * dimensionality is truncated (via the OpenAI-style {@code dimensions} field) to
 * match the {@code vector(768)} pgvector column.
 *
 * <p>Never throws: on any failure it returns {@link Optional#empty()} so callers
 * fall back to lexical retrieval.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiEmbeddingService implements EmbeddingService {

    private final RestClient geminiRestClient;
    private final AiProperties aiProperties;

    @Override
    public boolean isAvailable() {
        return aiProperties.getGemini().isConfigured();
    }

    @Override
    public Optional<float[]> embed(String text) {
        if (!isAvailable() || text == null || text.isBlank()) {
            return Optional.empty();
        }

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("model", aiProperties.getEmbedding().getModel());
            body.put("input", text);
            body.put("dimensions", aiProperties.getEmbedding().getDimensions());

            JsonNode response = geminiRestClient.post()
                    .uri("/embeddings")
                    .header("Authorization", "Bearer " + aiProperties.getGemini().getApiKey())
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

            if (response == null) {
                return Optional.empty();
            }
            JsonNode data = response.path("data");
            if (!data.isArray() || data.isEmpty()) {
                return Optional.empty();
            }
            JsonNode embeddingNode = data.get(0).path("embedding");
            if (!embeddingNode.isArray() || embeddingNode.isEmpty()) {
                return Optional.empty();
            }

            float[] vector = new float[embeddingNode.size()];
            for (int i = 0; i < vector.length; i++) {
                vector[i] = (float) embeddingNode.get(i).asDouble();
            }
            return Optional.of(vector);
        } catch (Exception e) {
            log.warn("Gemini embedding call failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /** Embeds several texts, skipping any that fail. Order is not guaranteed to be preserved. */
    public List<float[]> embedAll(List<String> texts) {
        return texts.stream().map(this::embed).filter(Optional::isPresent).map(Optional::get).toList();
    }
}
