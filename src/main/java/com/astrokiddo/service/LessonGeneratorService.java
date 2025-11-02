package com.astrokiddo.service;

import com.astrokiddo.ai.CloudflareAiRecords;
import com.astrokiddo.ai.CloudflareAiService;
import com.astrokiddo.dto.ApodResponseDto;
import com.astrokiddo.dto.GenerateDeckRequestDto;
import com.astrokiddo.dto.ImageSearchResponseDto;
import com.astrokiddo.model.LessonDeck;
import com.astrokiddo.model.Slide;
import com.astrokiddo.nasa.NasaReactiveCache;
import com.astrokiddo.templates.ContentTemplateEngine;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class LessonGeneratorService {

    private final ContentTemplateEngine engine = new ContentTemplateEngine();
    private final NasaReactiveCache cache;
    private final CloudflareAiService aiService;

    public LessonGeneratorService(NasaReactiveCache cache, CloudflareAiService aiService) {
        this.cache = cache;
        this.aiService = aiService;
    }

    // TODO: turn off minusMonth after shutdown
    public Mono<LessonDeck> generate(GenerateDeckRequestDto req) {
        String topic = req.getTopic().trim();
        Mono<ImageSearchResponseDto> images =
                cache.searchImages(topic, "image", null, null)
                .switchIfEmpty(Mono.just(new ImageSearchResponseDto()))
                .onErrorReturn(new ImageSearchResponseDto());
        Mono<ApodResponseDto> apod =
                cache.getApod(LocalDate.now().minusMonths(2))
                .switchIfEmpty(Mono.just(new ApodResponseDto()))
                .onErrorResume(ex -> Mono.just(new ApodResponseDto()));

        return Mono.zip(images, apod)
                .flatMap(tuple -> {
                    ImageSearchResponseDto imgDto = tuple.getT1();
                    ApodResponseDto apodDto = tuple.getT2();
                    Mono<CloudflareAiRecords.EnrichmentResponse> enrichmentMono = aiService.enrich(apodDto, req.getGradeLevel());
                    return enrichmentMono.map(enrichment -> buildDeck(topic, req.getGradeLevel(), imgDto, apodDto, enrichment));
                });
    }

    private List<ImageSearchResponseDto.Item> extractImageItems(ImageSearchResponseDto resp) {
        if (resp == null || resp.getCollection() == null || resp.getCollection().getItems() == null) {
            return List.of();
        }
        return resp.getCollection().getItems();
    }

    private LessonDeck buildDeck(String topic, String gradeLevel, ImageSearchResponseDto imgDto,
                                 ApodResponseDto apodDto, CloudflareAiRecords.EnrichmentResponse enrichment) {
        List<ImageSearchResponseDto.Item> items = new ArrayList<>(extractImageItems(imgDto));
        items.removeIf(Objects::isNull);

        ImageSearchResponseDto.Item keyVisualItem = !items.isEmpty() ? items.get(0) : null;
        ImageSearchResponseDto.Item explanationItem = items.size() > 1 ? items.get(1) : keyVisualItem;
        ImageSearchResponseDto.Item furtherReadingItem = items.size() > 2 ? items.get(2)
                : (items.size() > 1 ? items.get(items.size() - 1) : keyVisualItem);

        LessonDeck deck = new LessonDeck(topic);
        List<Slide> slides = new ArrayList<>();

        Slide key = engine.keyVisualFromImageItem(keyVisualItem);
        Slide explanation = engine.explanation(topic, apodDto, explanationItem);
        Slide why = engine.whyItMatters(topic);
        Slide question = engine.questionForClass(topic, gradeLevel);
        Slide further = engine.furtherReading(topic, furtherReadingItem);

        applyEnrichment(enrichment, key, explanation, why, question, further, gradeLevel);

        slides.add(key);
        slides.add(explanation);
        slides.add(why);
        slides.add(question);
        slides.add(further);

        slides.forEach(deck::addSlide);
        if (enrichment != null && enrichment.isMeaningful()) {
            deck.setEnrichment(enrichment);
        }
        return deck;
    }

    private void applyEnrichment(CloudflareAiRecords.EnrichmentResponse enrichment, Slide key, Slide explanation,
                                 Slide why, Slide question, Slide further, String gradeLevel) {
        if (enrichment == null) {
            return;
        }
        if (enrichment.hasHook()) {
            key.setText(enrichment.hook());
        }
        if (enrichment.hasAttribution()) {
            key.setAttribution(enrichment.attribution());
        }
        if (enrichment.hasSimpleExplanation()) {
            explanation.setText(enrichment.simpleExplanation());
            if (enrichment.hasAttribution()) {
                explanation.setAttribution(enrichment.attribution());
            }
        }
        if (enrichment.hasWhyItMatters()) {
            why.setText(enrichment.whyItMatters());
        }
        if (enrichment.hasClassQuestion()) {
            String enrichedQuestion = enrichment.classQuestion().trim();
            String normalizedGrade = normalizeGradeLevel(gradeLevel);
            if (normalizedGrade != null && !enrichedQuestion.toLowerCase().contains("grade")) {
                enrichedQuestion = enrichedQuestion + " (Align difficulty for grade " + normalizedGrade + ")";
            }
            question.setText(enrichedQuestion);
        }
        if (enrichment.hasFunFact()) {
            String base = defaultString(further.getText());
            String funFact = "Fun fact: " + enrichment.funFact();
            if (base.isEmpty()) {
                further.setText(funFact);
            } else if (!base.contains(funFact)) {
                further.setText(base + "\n\n" + funFact);
            }
        }
    }

    private String defaultString(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeGradeLevel(String gradeLevel) {
        if (gradeLevel == null) {
            return null;
        }
        String normalized = gradeLevel.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
