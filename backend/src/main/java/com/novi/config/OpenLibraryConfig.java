package com.novi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class OpenLibraryConfig {

    @Bean
    public RestClient openLibraryRestClient() {
        // Open Library's public API requires no API key, which keeps the
        // metadata provider swappable and free to run for a portfolio project.
        return RestClient.builder()
                .baseUrl("https://openlibrary.org")
                .build();
    }
}
