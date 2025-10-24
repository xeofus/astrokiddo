package com.astrokiddo.config;

import org.springframework.context.annotation.Configuration;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.astrokiddo.dto.ApodResponseDto;
import com.astrokiddo.dto.ImageSearchResponseDto;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Configuration
public class CacheConfig {
    @Bean
    public Cache<String, Mono<ApodResponseDto>> apodCache() {
        return Caffeine.newBuilder()
                .maximumSize(365)
                .expireAfterWrite(Duration.ofHours(12))
                .recordStats()
                .build();
    }

    @Bean
    public Cache<String, Mono<ImageSearchResponseDto>> imageSearchCache() {
        return Caffeine.newBuilder()
                .maximumSize(2000)
                .expireAfterWrite(Duration.ofMinutes(20))
                .recordStats()
                .build();
    }
}
