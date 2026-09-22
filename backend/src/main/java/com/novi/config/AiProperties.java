package com.novi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "novi.ai")
public class AiProperties {

    private Voyage voyage = new Voyage();
    private Anthropic anthropic = new Anthropic();
    private int candidatePoolSize = 100;
    private int recommendationCount = 10;
    /**
     * Upper bound on how many books are loaded into memory and scored per
     * recommendation request. A guardrail against loading an arbitrarily large
     * catalog; for a modest, import-on-demand catalog this is never reached.
     * The proper long-term fix for very large catalogs is a vector index
     * (pgvector/ANN) that narrows candidates in the database - see README.
     */
    private int maxScanBooks = 5000;

    public static class Voyage {
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

    public static class Anthropic {
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

    public Voyage getVoyage() { return voyage; }
    public void setVoyage(Voyage voyage) { this.voyage = voyage; }
    public Anthropic getAnthropic() { return anthropic; }
    public void setAnthropic(Anthropic anthropic) { this.anthropic = anthropic; }
    public int getCandidatePoolSize() { return candidatePoolSize; }
    public void setCandidatePoolSize(int candidatePoolSize) { this.candidatePoolSize = candidatePoolSize; }
    public int getRecommendationCount() { return recommendationCount; }
    public void setRecommendationCount(int recommendationCount) { this.recommendationCount = recommendationCount; }
    public int getMaxScanBooks() { return maxScanBooks; }
    public void setMaxScanBooks(int maxScanBooks) { this.maxScanBooks = maxScanBooks; }
}
