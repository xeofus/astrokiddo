package com.astrokiddo.enrich;

import com.astrokiddo.config.EnricherProperties;
import com.astrokiddo.dto.ApodResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class EnricherClient {

    private static final Logger log = LoggerFactory.getLogger(EnricherClient.class);
    private static final Pattern FIRST_NUMBER = Pattern.compile("(\\d+)");

    private final WebClient client;
    private final EnricherProperties properties;

    public EnricherClient(WebClient enricherWebClient, EnricherProperties properties) {
        this.client = enricherWebClient;
        this.properties = properties;
    }

    public Mono<EnrichmentResponse> enrich(ApodResponseDto apod, String gradeLevel) {
        if (!properties.isEnabled() || apod == null) {
            return Mono.empty();
        }
        String title = apod.getTitle();
        String explanation = apod.getExplanation();
        if (isBlank(title) || isBlank(explanation)) {
            return Mono.empty();
        }

        EnrichmentRequest request = new EnrichmentRequest(
                title,
                explanation,
                parseGrade(gradeLevel),
                Math.max(0, properties.getMaxVocabulary()),
                properties.getTemperature()
        );

        return client.post()
                .uri("/enrich")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(EnrichmentResponse.class)
                .timeout(Duration.ofSeconds(8))
                .retryWhen(Retry.backoff(1, Duration.ofMillis(250))
                        .maxBackoff(Duration.ofSeconds(1))
                        .jitter(0.2)
                        .filter(this::isTransient))
                .doOnError(ex -> log.warn("Enricher call failed: {}", ex.getMessage()))
                .onErrorResume(ex -> Mono.empty());
    }

    private boolean isTransient(Throwable throwable) {
        return throwable instanceof java.io.IOException
                || (throwable.getCause() != null && throwable.getCause() instanceof java.io.IOException);
    }

    private int parseGrade(String gradeLevel) {
        if (gradeLevel == null) {
            return 5;
        }
        Matcher matcher = FIRST_NUMBER.matcher(gradeLevel);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException ignored) {
                // fall through
            }
        }
        return 5;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}