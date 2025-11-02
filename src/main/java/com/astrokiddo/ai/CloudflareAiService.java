package com.astrokiddo.ai;

import com.astrokiddo.config.CloudflareAiProperties;
import com.astrokiddo.dto.ApodResponseDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class CloudflareAiService {

    private static final Logger log = LoggerFactory.getLogger(CloudflareAiService.class);

    private final WebClient client;
    private final CloudflareAiProperties properties;

    public CloudflareAiService(WebClient cloudflareAiWebClient,
                               CloudflareAiProperties properties) {
        this.client = cloudflareAiWebClient;
        this.properties = properties;
    }

    public Mono<CloudflareAiRecords.EnrichmentResponse> enrich(ApodResponseDto apod, String gradeLevel) {
        if (!properties.isEnabled() || apod == null || !properties.isConfigured()) {
            return Mono.empty();
        }
        if (!StringUtils.hasText(apod.getTitle()) || !StringUtils.hasText(apod.getExplanation())) {
            return Mono.empty();
        }
        CloudflareAiRequest request = buildRequest(apod, gradeLevel);
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
                .bodyValue(request)
                .retrieve()
                .bodyToMono(CloudflareAiRecords.CfAiEnvelope.class)
                .map(CloudflareAiValidationUtil::validateOrThrow)
                .timeout(Duration.ofSeconds(60))
                .retryWhen(Retry.backoff(1, Duration.ofMillis(250))
                        .maxBackoff(Duration.ofSeconds(1))
                        .jitter(0.2)
                        .filter(this::isTransient))
                .map(env -> env.result().response())
                .doOnError(ex -> log.warn("Cloudflare AI call failed: {}", ex.getMessage()));
    }

    private boolean isTransient(Throwable throwable) {
        return throwable instanceof java.io.IOException
                || (throwable.getCause() != null && throwable.getCause() instanceof java.io.IOException);
    }

    private CloudflareAiRequest buildRequest(ApodResponseDto apod, String gradeLevel) {
        String systemPrompt = buildSystemPrompt();
        String userPrompt = buildUserPrompt(apod, gradeLevel);

        CloudflareAiMessage systemMessage = new CloudflareAiMessage("system", systemPrompt);

        CloudflareAiMessage userMessage = new CloudflareAiMessage("user", userPrompt);

        return new CloudflareAiRequest(
                List.of(systemMessage, userMessage),
                buildResponseFormat()
        );
    }

    private String buildSystemPrompt() {
        return "You are an assistant that creates concise lesson enrichment JSON for educators. " +
                "Keep vocabulary entries to at most " + Math.max(0, properties.getMaxVocabulary()) + " items.";
    }

    private String buildUserPrompt(ApodResponseDto apod, String gradeLevel) {
        return "Create enrichment material for a classroom lesson about NASA's Astronomy Picture of the Day." +
                "Focus on keeping explanations accessible for grade " + gradeLevel + " students." +
                "Title: " + apod.getTitle() +
                "Provide up to " + Math.max(0, properties.getMaxVocabulary()) + " vocabulary items." +
                "Make the class_question actionable for classroom discussion." +
                "Include a fun_fact when possible and cite attribution if a source is obvious." +
                "Return only the JSON object.";
    }

    private CloudflareAiResponseFormat buildResponseFormat() {
        CloudflareAiProperty hook = CloudflareAiProperty.string("Engaging hook to start the lesson");
        CloudflareAiProperty simpleExplanation = CloudflareAiProperty.string("Plain-language summary of the concept");
        CloudflareAiProperty whyItMatters = CloudflareAiProperty.string("Reason the content matters to students");
        CloudflareAiProperty classQuestion = CloudflareAiProperty.string("Question to spark classroom discussion");

        CloudflareAiProperty vocabularyItem = CloudflareAiProperty.object(
                "Vocabulary entry",
                Map.of(
                        "term", CloudflareAiProperty.string("Vocabulary term"),
                        "definition", CloudflareAiProperty.string("Short definition appropriate for students")
                ),
                List.of("term", "definition")
        );

        CloudflareAiProperty vocabulary = CloudflareAiProperty.array(
                vocabularyItem
        );

        CloudflareAiProperty funFact = CloudflareAiProperty.string("Interesting fact related to the topic");
        CloudflareAiProperty attribution = CloudflareAiProperty.string("Source attribution when applicable");

        CloudflareAiProperty meta = CloudflareAiProperty.object(
                "Metadata about the response",
                Map.of("model", CloudflareAiProperty.string("Model identifier")),
                List.of("model")
        );

        Map<String, CloudflareAiProperty> propertiesMap = new LinkedHashMap<>();
        propertiesMap.put("hook", hook);
        propertiesMap.put("simple_explanation", simpleExplanation);
        propertiesMap.put("why_it_matters", whyItMatters);
        propertiesMap.put("class_question", classQuestion);
        propertiesMap.put("vocabulary", vocabulary);
        propertiesMap.put("fun_fact", funFact);
        propertiesMap.put("attribution", attribution);
        propertiesMap.put("_meta", meta);

        CloudflareAiJsonSchema jsonSchema = getCloudflareAiJsonSchema(propertiesMap);

        return new CloudflareAiResponseFormat("json_schema", jsonSchema);
    }

    private static CloudflareAiJsonSchema getCloudflareAiJsonSchema(Map<String, CloudflareAiProperty> propertiesMap) {
        CloudflareAiSchema schema = new CloudflareAiSchema(
                "object",
                false,
                propertiesMap,
                List.of(
                        "hook",
                        "simple_explanation",
                        "why_it_matters",
                        "class_question",
                        "vocabulary",
                        "fun_fact",
                        "attribution",
                        "_meta"
                )
        );

        return new CloudflareAiJsonSchema("object", schema);
    }

    private record CloudflareAiRequest(List<CloudflareAiMessage> messages,
                                       CloudflareAiResponseFormat response_format) {
        public String toString() {
            ObjectMapper mapper = new ObjectMapper();
            try {
                return mapper.writeValueAsString(this);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private record CloudflareAiResponseFormat(String type, CloudflareAiJsonSchema json_schema) {
    }

    private record CloudflareAiJsonSchema(String type, CloudflareAiSchema schema) {
    }

    private record CloudflareAiSchema(String type,
                                      boolean additionalProperties,
                                      Map<String, CloudflareAiProperty> properties,
                                      List<String> required) {
    }

    private record CloudflareAiProperty(String type,
                                        String description,
                                        CloudflareAiProperty items,
                                        Map<String, CloudflareAiProperty> properties,
                                        Boolean additionalProperties,
                                        List<String> required) {

        private static CloudflareAiProperty string(String description) {
            return new CloudflareAiProperty("string", description, null, null, null, null);
        }

        private static CloudflareAiProperty array(CloudflareAiProperty items) {
            return new CloudflareAiProperty("array", "Vocabulary terms with definitions", items, null, null, null);
        }

        private static CloudflareAiProperty object(String description,
                                                   Map<String, CloudflareAiProperty> properties,
                                                   List<String> required) {
            return new CloudflareAiProperty("object", description, null, properties, false, required);
        }
    }

    private record CloudflareAiMessage(String role, String content) {
    }
}