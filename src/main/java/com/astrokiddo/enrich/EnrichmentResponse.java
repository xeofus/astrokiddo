package com.astrokiddo.enrich;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class EnrichmentResponse {

    private String hook;

    @JsonProperty("simple_explanation")
    private String simpleExplanation;

    @JsonProperty("why_it_matters")
    private String whyItMatters;

    @JsonProperty("class_question")
    private String classQuestion;

    private List<VocabularyItem> vocabulary = new ArrayList<>();

    @JsonProperty("fun_fact")
    private String funFact;

    private String attribution;

    @JsonProperty("_meta")
    private EnrichmentMeta meta;

    public static EnrichmentResponse empty() {
        return new EnrichmentResponse();
    }

    public void setVocabulary(List<VocabularyItem> vocabulary) {
        this.vocabulary = vocabulary == null ? new ArrayList<>() : vocabulary;
    }

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

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EnrichmentMeta {
        private String model;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VocabularyItem {
        private String term;
        private String definition;
    }
}
