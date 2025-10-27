package com.astrokiddo.service;

import com.astrokiddo.dto.ApodResponseDto;
import com.astrokiddo.dto.GenerateDeckRequestDto;
import com.astrokiddo.dto.ImageSearchResponseDto;
import com.astrokiddo.enrich.EnricherClient;
import com.astrokiddo.enrich.EnrichmentResponse;
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
    private final EnricherClient enricherClient;

    public LessonGeneratorService(NasaReactiveCache cache, EnricherClient enricherClient) {
        this.cache = cache;
        this.enricherClient = enricherClient;
    }

    public Mono<LessonDeck> generate(GenerateDeckRequestDto req) {
        String topic = req.getTopic().trim();
        Mono<ImageSearchResponseDto> images =
                cache.searchImages(topic, "image", null, null)
                .switchIfEmpty(Mono.just(new ImageSearchResponseDto()))
                .onErrorReturn(new ImageSearchResponseDto());
        Mono<ApodResponseDto> apod =
                cache.getApod(LocalDate.now())
                .switchIfEmpty(Mono.just(new ApodResponseDto()))
                .onErrorResume(ex -> Mono.just(new ApodResponseDto()));

        return Mono.zip(images, apod)
                .flatMap(tuple -> {
                    ImageSearchResponseDto imgDto = tuple.getT1();
                    ApodResponseDto apodDto = tuple.getT2();

                    Mono<EnrichmentResponse> enrichmentMono = enricherClient.enrich(apodDto, req.getGradeLevel())
                            .defaultIfEmpty(EnrichmentResponse.empty());

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
                                 ApodResponseDto apodDto, EnrichmentResponse enrichment) {
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

    private void applyEnrichment(EnrichmentResponse enrichment, Slide key, Slide explanation,
                                 Slide why, Slide question, Slide further, String gradeLevel) {
        if (enrichment == null) {
            return;
        }
        if (enrichment.hasHook()) {
            key.setText(enrichment.getHook());
        }
        if (enrichment.hasAttribution()) {
            key.setAttribution(enrichment.getAttribution());
        }
        if (enrichment.hasSimpleExplanation()) {
            explanation.setText(enrichment.getSimpleExplanation());
            if (enrichment.hasAttribution()) {
                explanation.setAttribution(enrichment.getAttribution());
            }
        }
        if (enrichment.hasWhyItMatters()) {
            why.setText(enrichment.getWhyItMatters());
        }
        if (enrichment.hasClassQuestion()) {
            String enrichedQuestion = enrichment.getClassQuestion().trim();
            String normalizedGrade = normalizeGradeLevel(gradeLevel);
            if (normalizedGrade != null && !enrichedQuestion.toLowerCase().contains("grade")) {
                enrichedQuestion = enrichedQuestion + " (Align difficulty for grade " + normalizedGrade + ")";
            }
            question.setText(enrichedQuestion);
        }
        if (enrichment.hasFunFact()) {
            String base = defaultString(further.getText());
            String funFact = "Fun fact: " + enrichment.getFunFact();
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
