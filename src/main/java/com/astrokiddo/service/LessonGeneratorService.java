package com.astrokiddo.service;

import com.astrokiddo.dto.ApodResponseDto;
import com.astrokiddo.dto.GenerateDeckRequestDto;
import com.astrokiddo.dto.ImageSearchResponseDto;
import com.astrokiddo.model.LessonDeck;
import com.astrokiddo.model.Slide;
import com.astrokiddo.nasa.ApodClient;
import com.astrokiddo.nasa.NasaImageClient;
import com.astrokiddo.nasa.NasaReactiveCache;
import com.astrokiddo.templates.ContentTemplateEngine;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class LessonGeneratorService {

    private final NasaImageClient imageClient;
    private final ApodClient apodClient;
    private final ContentTemplateEngine engine = new ContentTemplateEngine();
    private final NasaReactiveCache cache;

    public LessonGeneratorService(NasaImageClient imageClient, ApodClient apodClient, NasaReactiveCache cache) {
        this.imageClient = imageClient;
        this.apodClient = apodClient;
        this.cache = cache;
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
                .map(tuple -> {
                    ImageSearchResponseDto imgDto = tuple.getT1();
                    ApodResponseDto apodDto = tuple.getT2();

                    List<ImageSearchResponseDto.Item> items = extractImageItems(imgDto);
                    ImageSearchResponseDto.Item topImage = items.isEmpty() ? null : items.get(0);

                    LessonDeck deck = new LessonDeck(topic);
                    List<Slide> slides = new ArrayList<>();

                    slides.add(engine.keyVisualFromImageItem(topImage));
                    slides.add(engine.explanation(topic, apodDto, topImage));
                    slides.add(engine.whyItMatters(topic));
                    slides.add(engine.questionForClass(topic, req.getGradeLevel()));
                    slides.add(engine.furtherReading(topic, topImage));

                    slides.forEach(deck::addSlide);
                    return deck;
                });
    }

    private List<ImageSearchResponseDto.Item> extractImageItems(ImageSearchResponseDto resp) {
        if (resp == null || resp.getCollection() == null || resp.getCollection().getItems() == null) {
            return List.of();
        }
        return resp.getCollection().getItems();
    }
}
