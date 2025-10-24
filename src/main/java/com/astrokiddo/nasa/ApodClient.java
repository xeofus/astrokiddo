
package com.astrokiddo.nasa;

import com.astrokiddo.config.NasaProperties;
import com.astrokiddo.dto.ApodResponseDto;
import io.netty.handler.timeout.TimeoutException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.PrematureCloseException;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.LocalDate;

@Component
public class ApodClient {
    private final WebClient client;
    private final NasaProperties props;

    public ApodClient(WebClient apodWebClient, NasaProperties props) {
        this.client = apodWebClient;
        this.props = props;
    }

    public Mono<ApodResponseDto> apod(LocalDate date) {
        return client.get()
                .uri(uri -> uri
                        .queryParam("api_key", props.getApiKey())
                        .queryParam("date", date.toString())
                        .queryParam("thumbs", "true")
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(ApodResponseDto.class)
                .timeout(Duration.ofSeconds(8))
                .retryWhen(Retry.backoff(2, Duration.ofMillis(300))
                        .maxBackoff(Duration.ofSeconds(2))
                        .jitter(0.2)
                        .filter(this::isTransient))
                .onErrorResume(ex -> Mono.just(new ApodResponseDto()));
    }

    private boolean isTransient(Throwable t) {
        return t instanceof TimeoutException
                || t instanceof PrematureCloseException
                || (t.getCause() != null && isTransient(t.getCause()));
    }
}
