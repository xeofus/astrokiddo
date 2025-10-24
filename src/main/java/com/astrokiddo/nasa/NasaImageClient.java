
package com.astrokiddo.nasa;

import com.astrokiddo.dto.ImageSearchResponseDto;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class NasaImageClient {
    private final WebClient client;

    public NasaImageClient(WebClient imagesWebClient) {
        this.client = imagesWebClient;
    }

    public Mono<ImageSearchResponseDto> searchImages(String query, String mediaType, Integer yearStart, Integer yearEnd) {
        return client.get().uri(uri -> {
            var b = uri.path("/search").queryParam("q", query);
            if (mediaType != null) b.queryParam("media_type", mediaType);
            if (yearStart != null) b.queryParam("year_start", yearStart);
            if (yearEnd != null) b.queryParam("year_end", yearEnd);
            return b.build();
        }).accept(MediaType.APPLICATION_JSON).retrieve().bodyToMono(ImageSearchResponseDto.class);
    }
}
