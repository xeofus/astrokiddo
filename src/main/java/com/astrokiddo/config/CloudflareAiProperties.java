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
    private String cfAiProvider;
    private String cfAiVendor;
    private String cfAiModel;
    private String apiToken;
    private boolean enabled = true;
    private int maxVocabulary = 3;
    private double temperature = 0.6;

    public String getModel() {
        return cfAiProvider + "/" + cfAiVendor + "/" + cfAiModel;
    }

    public boolean isConfigured() {
        return hasText(accountId) && hasText(cfAiProvider) && hasText(cfAiVendor) && hasText(cfAiModel) && hasText(apiToken);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}