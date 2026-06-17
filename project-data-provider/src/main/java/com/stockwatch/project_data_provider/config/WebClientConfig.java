package com.stockwatch.project_data_provider.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .codecs(config->config.defaultCodecs()
                    .maxInMemorySize(10 * 1024 * 1024))
                .build();
                    // Set max in-memory size to 16MB
    }


    
}
