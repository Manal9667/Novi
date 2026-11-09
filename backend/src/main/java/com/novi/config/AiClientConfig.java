package com.novi.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
@RequiredArgsConstructor
public class AiClientConfig {

    private final AiProperties aiProperties;

    /**
     * RestClient for Gemini's OpenAI-compatible endpoint (chat + embeddings),
     * with explicit connect/read timeouts so a slow or unresponsive provider
     * can never hang a recommendation request indefinitely.
     */
    @Bean
    public RestClient geminiRestClient() {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofMillis(aiProperties.getTimeout().getConnectMs()))
                .withReadTimeout(Duration.ofMillis(aiProperties.getTimeout().getReadMs()));
        ClientHttpRequestFactory factory = ClientHttpRequestFactories.get(settings);

        return RestClient.builder()
                .baseUrl(aiProperties.getGemini().getBaseUrl())
                .requestFactory(factory)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
