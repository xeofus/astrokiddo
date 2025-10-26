package com.astrokiddo.controller;

import com.astrokiddo.dto.GenerateDeckRequestDto;
import com.astrokiddo.model.LessonDeck;
import com.astrokiddo.service.LessonGeneratorService;
import com.astrokiddo.store.DeckStore;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping(path = "/api/decks", produces = MediaType.APPLICATION_JSON_VALUE)
public class DeckController {

    private final LessonGeneratorService service;
    private final DeckStore store;

    public DeckController(LessonGeneratorService service, DeckStore store) {
        this.service = service;
        this.store = store;
    }

    @PostMapping(path = "/generate", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<LessonDeck> generate(@Valid @RequestBody GenerateDeckRequestDto req) {
        return service.generate(req).doOnNext(store::save);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LessonDeck> get(@PathVariable String id) {
        LessonDeck deck = store.get(id).orElseThrow(() -> new NoSuchElementException("Deck not found: " + id));
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(10, TimeUnit.MINUTES).cachePublic())
                .body(deck);
    }
}
