package com.astrokiddo.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

public class CloudflareAiRecords {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CfAiEnvelope(
            @NotNull @Valid @JsonProperty("result") Result result,
            @JsonProperty("success") boolean success,
            @NotNull @JsonProperty("errors") List<ApiError> errors,
            @NotNull @JsonProperty("messages") List<Map<String, Object>> messages
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Result(
            @NotNull @Valid @JsonProperty("response") EnrichmentResponse response
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EnrichmentResponse(
            @NotBlank @Size(max = 200)
            @JsonProperty("hook") String hook,

            @NotBlank @Size(max = 2000)
            @JsonProperty("simple_explanation") String simpleExplanation,

            @NotBlank @Size(max = 2000)
            @JsonProperty("why_it_matters") String whyItMatters,

            @NotBlank @Size(max = 500)
            @JsonProperty("class_question") String classQuestion,

            @NotNull @Size(min = 0, max = 20) @Valid
            @JsonProperty("vocabulary") List<VocabItem> vocabulary,

            @NotBlank @Size(max = 500)
            @JsonProperty("fun_fact") String funFact,

            @NotBlank @Size(max = 300)
            @JsonProperty("attribution") String attribution,

            @NotNull @Valid
            @JsonProperty("_meta") Meta meta
    ) {
        public boolean hasHook() {
            return !isBlank(hook);
        }

        public boolean hasSimpleExplanation() {
            return !isBlank(simpleExplanation);
        }

        public boolean hasWhyItMatters() {
            return !isBlank(whyItMatters);
        }

        public boolean hasClassQuestion() {
            return !isBlank(classQuestion);
        }

        public boolean hasFunFact() {
            return !isBlank(funFact);
        }

        public boolean hasAttribution() {
            return !isBlank(attribution);
        }

        public boolean hasVocabulary() {
            return vocabulary != null && !vocabulary.isEmpty();
        }

        public boolean isMeaningful() {
            return hasHook() || hasSimpleExplanation() || hasWhyItMatters()
                    || hasClassQuestion() || hasFunFact() || hasVocabulary() || hasAttribution();
        }

        private boolean isBlank(String value) {
            return value == null || value.trim().isEmpty();
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record VocabItem(
            @NotBlank @Size(max = 64)
            @JsonProperty("term") String term,

            @NotBlank @Size(max = 300)
            @JsonProperty("definition") String definition
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Meta(
            @NotBlank
            @JsonProperty("model") String model
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ApiError(
            @JsonProperty("code") String code,
            @JsonProperty("message") String message
    ) {
    }
}
