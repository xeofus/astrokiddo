package com.astrokiddo.controller;

import com.astrokiddo.dto.ApodResponseDto;
import com.astrokiddo.nasa.NasaReactiveCache;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping(path = "/api/apod", produces = MediaType.APPLICATION_JSON_VALUE)
public class ApodController {

    private final NasaReactiveCache cache;

    public ApodController(NasaReactiveCache cache) {
        this.cache = cache;
    }

    @GetMapping
    public Mono<ResponseEntity<ApodResponseDto>> getApod(
            @RequestParam(value = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        LocalDate targetDate = date != null ? date : LocalDate.now().minusMonths(5);
        return cache.getApod(targetDate)
                .switchIfEmpty(Mono.just(new ApodResponseDto()))
                .map(apod -> ResponseEntity.ok()
                        .cacheControl(CacheControl.maxAge(24, TimeUnit.HOURS).cachePublic())
                        .body(apod));
    }
}
