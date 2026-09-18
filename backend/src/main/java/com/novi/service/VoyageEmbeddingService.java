package com.novi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.novi.config.AiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class VoyageEmbeddingService implements EmbeddingService {

    private final RestClient voyageRestClient;
    private final AiProperties aiProperties;

    @Override
    public boolean isAvailable() {
        return aiProperties.getVoyage().isConfigured();
    }

    @Override
    public Optional<float[]> embed(String text) {
        if (!isAvailable() || text == null || text.isBlank()) {
            return Optional.empty();
        }

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("input", java.util.List.of(text));
            body.put("model", aiProperties.getVoyage().getModel());

            JsonNode response = voyageRestClient.post()
                    .uri("/embeddings")
                    .header("Authorization", "Bearer " + aiProperties.getVoyage().getApiKey())
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

            if (response == null || !response.has("data") || response.get("data").isEmpty()) {
                return Optional.empty();
            }

            JsonNode embeddingNode = response.get("data").get(0).get("embedding");
            float[] vector = new float[embeddingNode.size()];
            for (int i = 0; i < vector.length; i++) {
                vector[i] = (float) embeddingNode.get(i).asDouble();
            }
            return Optional.of(vector);
        } catch (Exception e) {
            log.warn("Voyage embedding call failed: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
