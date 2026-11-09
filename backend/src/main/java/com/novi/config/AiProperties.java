package com.novi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Novi's single AI provider, Google Gemini, used for both
 * reasoning (query understanding, grounded explanations) and embeddings, via
 * Gemini's OpenAI-compatible endpoint. Left unconfigured (no API key), every AI
 * feature degrades gracefully: retrieval falls back to lexical search and
 * explanations fall back to a deterministic sentence.
 */
@Configuration
@ConfigurationProperties(prefix = "novi.ai")
public class AiProperties {

    private Gemini gemini = new Gemini();
    private Embedding embedding = new Embedding();
    private Timeout timeout = new Timeout();
    private Ranking ranking = new Ranking();

    /** How many candidates retrieval over-fetches before ranking trims to the final list. */
    private int candidatePoolSize = 60;
    /** How many recommendations are ultimately returned to the user. */
    private int recommendationCount = 8;

    public static class Gemini {
        private String apiKey;
        private String model;
        private String baseUrl;

        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public boolean isConfigured() { return apiKey != null && !apiKey.isBlank(); }
    }

    /**
     * Embedding model settings. gemini-embedding-001 defaults to 3072 dimensions
     * but is Matryoshka-truncatable; Novi uses 768 (a tier Google recommends) to
     * match the {@code vector(768)} pgvector column. Changing dimensions requires
     * a matching change to the V2 migration and re-embedding the catalog.
     */
    public static class Embedding {
        private String model = "gemini-embedding-001";
        private int dimensions = 768;

        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public int getDimensions() { return dimensions; }
        public void setDimensions(int dimensions) { this.dimensions = dimensions; }
    }

    /** HTTP timeouts for outbound Gemini calls, so a slow provider can't hang requests. */
    public static class Timeout {
        private int connectMs = 5000;
        private int readMs = 30000;

        public int getConnectMs() { return connectMs; }
        public void setConnectMs(int connectMs) { this.connectMs = connectMs; }
        public int getReadMs() { return readMs; }
        public void setReadMs(int readMs) { this.readMs = readMs; }
    }

    /**
     * Weights for the transparent hybrid ranking score. Semantic + metadata form
     * the request-relevance (normalized to sum to 1); preference is a bonus
     * applied only when the reader is signed in and has saved books.
     */
    public static class Ranking {
        private double semanticWeight = 0.6;
        private double metadataWeight = 0.25;
        private double preferenceWeight = 0.15;

        public double getSemanticWeight() { return semanticWeight; }
        public void setSemanticWeight(double semanticWeight) { this.semanticWeight = semanticWeight; }
        public double getMetadataWeight() { return metadataWeight; }
        public void setMetadataWeight(double metadataWeight) { this.metadataWeight = metadataWeight; }
        public double getPreferenceWeight() { return preferenceWeight; }
        public void setPreferenceWeight(double preferenceWeight) { this.preferenceWeight = preferenceWeight; }
    }

    public Gemini getGemini() { return gemini; }
    public void setGemini(Gemini gemini) { this.gemini = gemini; }
    public Ranking getRanking() { return ranking; }
    public void setRanking(Ranking ranking) { this.ranking = ranking; }
    public Embedding getEmbedding() { return embedding; }
    public void setEmbedding(Embedding embedding) { this.embedding = embedding; }
    public Timeout getTimeout() { return timeout; }
    public void setTimeout(Timeout timeout) { this.timeout = timeout; }
    public int getCandidatePoolSize() { return candidatePoolSize; }
    public void setCandidatePoolSize(int candidatePoolSize) { this.candidatePoolSize = candidatePoolSize; }
    public int getRecommendationCount() { return recommendationCount; }
    public void setRecommendationCount(int recommendationCount) { this.recommendationCount = recommendationCount; }
}
