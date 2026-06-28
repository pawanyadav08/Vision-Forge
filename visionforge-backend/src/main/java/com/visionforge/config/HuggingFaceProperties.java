package com.visionforge.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * HuggingFaceProperties — Type-safe binding for the 'huggingface' YAML block.
 *
 * Bound from application.yml:
 * <pre>{@code
 * huggingface:
 *   api-key: ${HF_API_KEY}
 *   base-url: https://api-inference.huggingface.co
 *   model: stabilityai/stable-diffusion-xl-base-1.0
 *   timeout-seconds: 120
 * }</pre>
 *
 * Why @ConfigurationProperties over @Value?
 *  - All config for this provider is in ONE class — no scattered @Value fields
 *  - @Validated causes Spring to throw a clear error at startup if the
 *    API key env var is not set (fast-fail > runtime NPE)
 *  - Type-safe: timeoutSeconds is an int, not a String to parse
 *  - Easily testable: just instantiate this class in a unit test
 *
 * The @EnableConfigurationProperties(HuggingFaceProperties.class) annotation
 * is placed on WebClientConfig to register this bean.
 */
@Validated
@ConfigurationProperties(prefix = "huggingface")
public class HuggingFaceProperties {

    /**
     * Hugging Face API key.
     * Sourced from HF_API_KEY environment variable.
     * Required — Spring fails fast at startup if missing.
     */
    @NotBlank(message = "HuggingFace API key (HF_API_KEY env var) must not be blank")
    private String apiKey;

    /**
     * Base URL for the Hugging Face Inference API.
     * Default: https://api-inference.huggingface.co
     * Configurable to support HF Inference Endpoints (dedicated instances).
     */
    @NotBlank(message = "HuggingFace base URL must not be blank")
    private String baseUrl;

    /**
     * Model identifier on Hugging Face Hub.
     * E.g.: "stabilityai/stable-diffusion-xl-base-1.0"
     *       "runwayml/stable-diffusion-v1-5"
     *       "CompVis/stable-diffusion-v1-4"
     */
    @NotBlank(message = "HuggingFace model must not be blank")
    private String model;

    /**
     * HTTP request timeout in seconds.
     * Default: 120 seconds.
     * HF cold-starts (model loading) can take 20–90 seconds.
     * Minimum 10 seconds to prevent premature failures.
     */
    @Min(value = 10, message = "Timeout must be at least 10 seconds")
    private int timeoutSeconds = 120;

    // ── Getters and Setters (Spring needs setters for property binding) ──────
    // Note: not using @Getter/@Setter to keep the binding mechanism explicit

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }
}
