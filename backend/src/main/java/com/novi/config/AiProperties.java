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
}
