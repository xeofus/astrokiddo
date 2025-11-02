package com.astrokiddo.model;

import com.astrokiddo.ai.CloudflareAiRecords;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class LessonDeck {
    private String id;
    private String topic;
    private Instant createdAt;
    private List<Slide> slides = new ArrayList<>();
    private CloudflareAiRecords.EnrichmentResponse enrichment;

    public LessonDeck(String topic) {
        this.id = "deck-" + UUID.randomUUID();
        this.topic = topic;
        this.createdAt = Instant.now();
    }

    public void addSlide(Slide s) {
        this.slides.add(s);
    }
}
