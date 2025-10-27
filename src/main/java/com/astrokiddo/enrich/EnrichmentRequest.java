package com.astrokiddo.enrich;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class EnrichmentRequest {
    private final String title;
    private final String explanation;
    private final int grade;
    @JsonProperty("max_vocab")
    private final int maxVocabulary;
    private final double temperature;
}
