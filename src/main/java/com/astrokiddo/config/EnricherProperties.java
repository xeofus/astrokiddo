package com.astrokiddo.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.enricher")
@Getter
@Setter
public class EnricherProperties {
    private String baseUrl = "http://enricher:8090";
    private boolean enabled = true;
    private int maxVocabulary = 3;
    private double temperature = 0.6;
}
