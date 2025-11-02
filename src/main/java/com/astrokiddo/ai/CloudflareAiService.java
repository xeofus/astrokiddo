package com.astrokiddo.ai;

import com.astrokiddo.config.CloudflareAiProperties;
import com.astrokiddo.dto.ApodResponseDto;
import com.astrokiddo.enrich.EnrichmentResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class CloudflareAiService {

    private static final Logger log = LoggerFactory.getLogger(CloudflareAiService.class);
    private static final Pattern FIRST_NUMBER = Pattern.compile("(\\d+)");

    private final WebClient client;
    private final CloudflareAiProperties properties;
    private final ObjectMapper objectMapper;

    public CloudflareAiService(WebClient cloudflareAiWebClient,
                               CloudflareAiProperties properties,
                               ObjectMapper objectMapper) {
        this.client = cloudflareAiWebClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public Mono<EnrichmentResponse> enrich(ApodResponseDto apod, String gradeLevel) {
        if (!properties.isEnabled() || apod == null || !properties.isConfigured()) {
            return Mono.empty();
        }
        if (!StringUtils.hasText(apod.getTitle()) || !StringUtils.hasText(apod.getExplanation())) {
            return Mono.empty();
        }

        int grade = parseGrade(gradeLevel);
        CloudflareAiRequest request = buildRequest(apod, gradeLevel, grade);
        log.info("Request: {}", request);
        return client.post()
                .uri(b -> b.path("/client/v4/accounts/{accountId}/ai/run/")
                        .pathSegment("{cf_ai_provider}", "{cf_ai_vendor}", "{cf_ai_model}")
                        .build(
                                properties.getAccountId(),
                                properties.getCf_ai_provider(),
                                properties.getCf_ai_vendor(),
                                properties.getCf_ai_model()
                        )
                )
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request.toString())
                .retrieve()
                .bodyToMono(CloudflareAiResponse.class)
                .timeout(Duration.ofSeconds(60))
                .retryWhen(Retry.backoff(1, Duration.ofMillis(250))
                        .maxBackoff(Duration.ofSeconds(1))
                        .jitter(0.2)
                        .filter(this::isTransient))
                .flatMap(response -> Mono.justOrEmpty(convert(response)))
                .doOnError(ex -> log.warn("Cloudflare AI call failed: {}", ex.getMessage()))
                .onErrorResume(ex -> Mono.empty());
    }

    private boolean isTransient(Throwable throwable) {
        return throwable instanceof java.io.IOException
                || (throwable.getCause() != null && throwable.getCause() instanceof java.io.IOException);
    }

    private CloudflareAiRequest buildRequest(ApodResponseDto apod, String gradeLevel, int grade) {
        String systemPrompt = buildSystemPrompt();
        String userPrompt = buildUserPrompt(apod, gradeLevel, grade);

        return new CloudflareAiRequest(systemPrompt + userPrompt);
    }

    private String buildSystemPrompt() {
        return "You are an assistant that creates concise lesson enrichment JSON for educators. " +
                "Always respond with a single JSON object. Keys: hook, simple_explanation, " +
                "why_it_matters, class_question, vocabulary (array of {\"term\", \"definition\"}), fun_fact, " +
                "attribution, and _meta. The _meta object must include a \"model\" field. " +
                "When a field is not applicable, set it to an empty string or an empty array. " +
                "Never wrap the JSON in markdown fences or add commentary. " +
                "Keep vocabulary entries to at most " + Math.max(0, properties.getMaxVocabulary()) + " items.";
    }

    private String buildUserPrompt(ApodResponseDto apod, String gradeLevel, int grade) {
        return "Create enrichment material for a classroom lesson about NASA's Astronomy Picture of the Day." +
                "Focus on keeping explanations accessible for grade " + grade + " students." +
                "Title: " + apod.getTitle() + "\n" +
                "Provide up to " + Math.max(0, properties.getMaxVocabulary()) + " vocabulary items." +
                "Make the class_question actionable for classroom discussion." +
                "Include a fun_fact when possible and cite attribution if a source is obvious." +
                "Return only the JSON object.";
    }

    private EnrichmentResponse convert(CloudflareAiResponse response) {
        if (response == null) {
            return null;
        }
        String rawText = response.text();
        if (!StringUtils.hasText(rawText)) {
            return null;
        }
        String sanitized = sanitize(rawText);
        try {
            EnrichmentResponse enrichment = objectMapper.readValue(sanitized, EnrichmentResponse.class);
            if (enrichment.getMeta() == null) {
                EnrichmentResponse.EnrichmentMeta meta = new EnrichmentResponse.EnrichmentMeta();
                meta.setModel(properties.getModel());
                enrichment.setMeta(meta);
            } else if (!StringUtils.hasText(enrichment.getMeta().getModel())) {
                enrichment.getMeta().setModel(properties.getModel());
            }
            return enrichment;
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse Cloudflare AI response: {}", e.getMessage());
            return null;
        }
    }

    private String sanitize(String text) {
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            int firstLineBreak = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstLineBreak >= 0 && lastFence > firstLineBreak) {
                trimmed = trimmed.substring(firstLineBreak + 1, lastFence).trim();
            }
        }
        return trimmed;
    }

    private int parseGrade(String gradeLevel) {
        if (!StringUtils.hasText(gradeLevel)) {
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

    private record CloudflareAiRequest(String prompt) {
        @NotNull
        @Override
        public String toString() {
            ObjectMapper mapper = new ObjectMapper();
            try {
                return mapper.writeValueAsString(this);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private record CloudflareAiMessage(String role, List<CloudflareAiContent> content) {
    }

    private record CloudflareAiContent(String type, String text) {
    }

    private record CloudflareAiResponse(Result result) {
        String text() {
            if (result == null) {
                return null;
            }
            if (StringUtils.hasText(result.response)) {
                return result.response;
            }
            if (result.outputText != null && !result.outputText.isEmpty()) {
                return String.join("", result.outputText);
            }
            return null;
        }
    }

    private record Result(String response, List<String> outputText) {
    }
}