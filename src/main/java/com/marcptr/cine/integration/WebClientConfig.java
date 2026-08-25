package com.marcptr.cine.integration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

        @Bean
        public WebClient tmdbWebClient(
                        @Value("${tmdb.base-url}") String baseUrl,
                        @Value("${tmdb.api-key}") String apiKey) {

                return WebClient.builder()
                                .baseUrl(baseUrl).exchangeStrategies(
                                                ExchangeStrategies.builder()
                                                                .codecs(configurer -> configurer.defaultCodecs()
                                                                                .maxInMemorySize(16 * 1024 * 1024))
                                                                .build())
                                .build();
        }
}