package com.astrokiddo.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.nasa")
@Getter
@Setter
public class NasaProperties {
    private String apiKey;
    private String apodBaseUrl;
    private String imagesBaseUrl;
}
