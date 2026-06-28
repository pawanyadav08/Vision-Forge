package com.visionforge.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * WebClientConfig — Configures the WebClient bean for Hugging Face API calls.
 *
 * Why a dedicated bean instead of WebClient.create() inline?
 *  1. Centralized timeout, codec, and header configuration
 *  2. Testable — can mock/replace this bean in tests
 *  3. Single source of truth for HF API connection settings
 *
 * Key settings:
 *  - Connect timeout : 10 seconds (fail fast if HF unreachable)
 *  - Read timeout    : configurable via huggingface.timeout-seconds (default 120s)
 *                      HF cold-starts can take 20-90 seconds
 *  - Buffer size     : 20 MB — HF returns raw PNG bytes (SDXL ~2-4MB per image)
 *                      Default codec buffer is only 256KB — too small!
 *  - Authorization   : Bearer token injected from HuggingFaceProperties
 *
 * @EnableConfigurationProperties: Registers HuggingFaceProperties as a Spring bean
 * so the @ConfigurationProperties binding takes effect.
 */
@Configuration
@EnableConfigurationProperties(HuggingFaceProperties.class)
public class WebClientConfig {

    /**
     * The WebClient bean used by HuggingFaceImageProvider.
     *
     * Named "huggingFaceWebClient" to allow future providers to define their
     * own WebClient beans without ambiguity.
     *
     * @param props the bound HuggingFace configuration properties
     * @return a fully configured, ready-to-use WebClient
     */
    @Bean("huggingFaceWebClient")
    public WebClient huggingFaceWebClient(HuggingFaceProperties props) {

        // ── Netty HTTP client with explicit timeouts ──────────────────────────
        HttpClient httpClient = HttpClient.create()
                // Connection timeout: fail fast if HF endpoint unreachable
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000)
                // Response timeout: overall end-to-end deadline (handles cold-starts)
                .responseTimeout(Duration.ofSeconds(props.getTimeoutSeconds()))
                .doOnConnected(conn -> conn
                        // Read timeout: how long to wait for the next data chunk
                        .addHandlerLast(new ReadTimeoutHandler(
                                props.getTimeoutSeconds(), TimeUnit.SECONDS))
                        // Write timeout: how long to wait when sending the request body
                        .addHandlerLast(new WriteTimeoutHandler(30, TimeUnit.SECONDS))
                );

        // ── Codec: increase buffer size to handle large image responses ───────
        // SDXL PNG responses are typically 2–5 MB.
        // Default Spring WebClient buffer limit is 256 KB — way too small.
        ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
                .codecs(configurer ->
                        configurer.defaultCodecs().maxInMemorySize(20 * 1024 * 1024) // 20 MB
                )
                .build();

        // ── Assemble WebClient ────────────────────────────────────────────────
        return WebClient.builder()
                .baseUrl(props.getBaseUrl())
                // Hugging Face requires: "Authorization: Bearer hf_xxxx"
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + props.getApiKey())
                // We send JSON and accept raw binary (octet-stream or image/*)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(exchangeStrategies)
                .build();
    }
}
