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
    public RestClient anthropicRestClient() {
        return RestClient.builder()
                .baseUrl(aiProperties.getAnthropic().getBaseUrl())
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("anthropic-version", "2023-06-01")
                .build();
    }
}
