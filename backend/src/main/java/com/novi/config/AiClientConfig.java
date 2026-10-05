package com.novi.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@RequiredArgsConstructor
public class AiClientConfig {

    private final AiProperties aiProperties;

    @Bean
    public RestClient voyageRestClient() {
        return RestClient.builder()
                .baseUrl(aiProperties.getVoyage().getBaseUrl())
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Bean
    public RestClient geminiRestClient() {
        return RestClient.builder()
                .baseUrl(aiProperties.getGemini().getBaseUrl())
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
