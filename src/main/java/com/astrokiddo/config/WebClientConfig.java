package com.astrokiddo.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;

@Configuration
public class WebClientConfig {
    @Bean
    public ConnectionProvider nasaConnProvider() {
        return ConnectionProvider.builder("nasa")
                .maxConnections(50)
                .pendingAcquireTimeout(Duration.ofSeconds(5))
                .maxIdleTime(Duration.ofSeconds(30))
                .maxLifeTime(Duration.ofMinutes(2))
                .build();
    }

    @Bean
    public WebClient apodWebClient(NasaProperties props, ConnectionProvider nasaConnProvider) {
        return base(props.getApodBaseUrl(), nasaConnProvider);
    }

    @Bean
    public WebClient imagesWebClient(NasaProperties props, ConnectionProvider nasaConnProvider) {
        return base(props.getImagesBaseUrl(), nasaConnProvider);
    }

    @Bean
    public ConnectionProvider enricherConnProvider() {
        return ConnectionProvider.builder("enricher")
                .maxConnections(20)
                .pendingAcquireTimeout(Duration.ofSeconds(5))
                .maxIdleTime(Duration.ofSeconds(30))
                .maxLifeTime(Duration.ofMinutes(2))
                .build();
    }

    @Bean
    public WebClient enricherWebClient(EnricherProperties props, ConnectionProvider enricherConnProvider) {
        return base(props.getBaseUrl(), enricherConnProvider);
    }

    private WebClient base(String baseUrl, ConnectionProvider provider) {
        HttpClient httpClient = HttpClient.create(provider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .responseTimeout(Duration.ofSeconds(10))
                .compress(true)
                .followRedirect(true)
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(10))
                        .addHandlerLast(new WriteTimeoutHandler(10)));

        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.USER_AGENT, "AstroKiddo/0.1 (dev@astrokiddo.com)")
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(c -> c.defaultCodecs().maxInMemorySize(8 * 1024 * 1024))
                        .build())
                .build();
    }
}
