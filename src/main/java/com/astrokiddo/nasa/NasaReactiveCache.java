package com.astrokiddo.nasa;

import com.astrokiddo.dto.ApodResponseDto;
import com.astrokiddo.dto.ImageSearchResponseDto;
import com.github.benmanes.caffeine.cache.Cache;
import io.netty.handler.timeout.ReadTimeoutException;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.PrematureCloseException;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.LocalDate;
import java.util.concurrent.TimeoutException;

@Component
public class NasaReactiveCache {
    private final Cache<String, Mono<ApodResponseDto>> apodCache;
    private final Cache<String, Mono<ImageSearchResponseDto>> imageSearchCache;
    private final ApodClient apodClient;
    private final NasaImageClient imageClient;

    public NasaReactiveCache(Cache<String, Mono<ApodResponseDto>> apodCache,
                             Cache<String, Mono<ImageSearchResponseDto>> imageSearchCache,
                             ApodClient apodClient,
                             NasaImageClient imageClient) {
        this.apodCache = apodCache;
        this.imageSearchCache = imageSearchCache;
        this.apodClient = apodClient;
        this.imageClient = imageClient;
    }

    public Mono<ApodResponseDto> getApod(LocalDate date) {
        final String key = date.toString();
        return apodCache.get(key, k ->
                apodClient.apod(date)
                        .timeout(Duration.ofSeconds(8))
                        .retryWhen(Retry.backoff(2, Duration.ofMillis(300))
                                .maxBackoff(Duration.ofSeconds(2))
                                .jitter(0.2)
                                .filter(this::isTransient))
                        .onErrorReturn(new ApodResponseDto())
                        .cache()
        );
    }

    public Mono<ImageSearchResponseDto> searchImages(String q, String mediaType,
                                                     Integer yearStart, Integer yearEnd) {
        final String key = buildKey(q, mediaType, yearStart, yearEnd);
        return imageSearchCache.get(key, k ->
                imageClient.searchImages(q, mediaType, yearStart, yearEnd)
                        .timeout(Duration.ofSeconds(8))
                        .retryWhen(Retry.backoff(2, Duration.ofMillis(300))
                                .maxBackoff(Duration.ofSeconds(2))
                                .jitter(0.2)
                                .filter(this::isTransient))
                        .onErrorReturn(new ImageSearchResponseDto())
                        .cache()
        );
    }

    private boolean isTransient(Throwable t) {
        return t instanceof ReadTimeoutException
                || t instanceof TimeoutException
                || t instanceof PrematureCloseException
                || (t.getCause() != null && isTransient(t.getCause()));
    }

    private String buildKey(String q, String mediaType, Integer y1, Integer y2) {
        return (q == null ? "" : q.trim().toLowerCase()) + "|mt=" + (mediaType == null ? "" : mediaType)
                + "|y1=" + (y1 == null ? "" : y1) + "|y2=" + (y2 == null ? "" : y2);
    }
}
