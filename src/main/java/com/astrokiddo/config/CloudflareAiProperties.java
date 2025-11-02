package com.astrokiddo.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.cloudflare")
@Getter
@Setter
public class CloudflareAiProperties {
    private String baseUrl;
    private String accountId;
    private String cf_ai_provider;
    private String cf_ai_vendor;
    private String cf_ai_model;
    private String apiToken;
    private boolean enabled = true;
    private int maxVocabulary = 3;
    private double temperature = 0.6;

    public String getModel() {
        return cf_ai_provider + "/" + cf_ai_vendor + "/" + cf_ai_model;
    }

    public boolean isConfigured() {
        return hasText(accountId) && hasText(cf_ai_provider) && hasText(cf_ai_vendor) && hasText(cf_ai_model) && hasText(apiToken);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}