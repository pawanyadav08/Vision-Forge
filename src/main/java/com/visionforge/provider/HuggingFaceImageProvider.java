package com.visionforge.provider;

import com.visionforge.config.HuggingFaceProperties;
import com.visionforge.dto.request.ImageGenerationRequest;
import com.visionforge.exception.ImageGenerationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * HuggingFaceImageProvider — Concrete implementation of ImageProvider.
 *
 * Calls the Hugging Face Inference API to generate images using
 * Stable Diffusion or other text-to-image models hosted on HF Hub.
 *
 * ═══════════════════════════════════════════════════════════════════════════
 * API Contract (HF Inference API):
 * ═══════════════════════════════════════════════════════════════════════════
 * POST https://api-inference.huggingface.co/models/{model}
 * Authorization: Bearer hf_xxxx
 * Content-Type: application/json
 *
 * Request body:
 * {
 *   "inputs": "a photo of a cat",
 *   "parameters": {
 *     "negative_prompt": "blurry",
 *     "width": 1024,
 *     "height": 1024,
 *     "guidance_scale": 7.5,
 *     "num_inference_steps": 30
 *   }
 * }
 *
 * Response: raw image bytes (Content-Type: image/png or image/jpeg)
 *
 * Special HF response: If the model is loading (cold start), HF returns:
 *   503 with body: {"error": "...", "estimated_time": 20.5}
 * We surface this as an ImageGenerationException with the estimate.
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * SOLID Notes:
 *  - Implements ImageProvider (DIP)
 *  - All HF-specific mapping is HERE, not in the service (SRP)
 *  - The service never imports this class directly (OCP)
 */
@Slf4j
@Component
public class HuggingFaceImageProvider implements ImageProvider {

    private final WebClient webClient;
    private final HuggingFaceProperties props;

    /**
     * @Qualifier("huggingFaceWebClient") ensures Spring injects the correctly
     * configured WebClient bean (not a generic one).
     */
    public HuggingFaceImageProvider(
            @Qualifier("huggingFaceWebClient") WebClient webClient,
            HuggingFaceProperties props) {
        this.webClient = webClient;
        this.props = props;
    }

    // ── ImageProvider contract ────────────────────────────────────────────────

    @Override
    public String getProviderName() {
        return "HuggingFace";
    }

    @Override
    public String getModelName() {
        return props.getModel();
    }

    /**
     * Generate an image by calling the HF Inference API.
     *
     * Flow:
     *  1. Build the JSON request payload (HF-specific format)
     *  2. POST to /models/{model}
     *  3. Read response as byte[] (raw PNG/JPEG)
     *  4. On error: parse HF error and throw ImageGenerationException
     *
     * Uses .block() for synchronous execution — we are in a servlet thread
     * and don't need reactive pipelines here.
     *
     * @param prompt  the text prompt from the user
     * @param request full generation parameters (width, height, etc.)
     * @return raw image bytes
     * @throws ImageGenerationException if the API returns an error
     */
    @Override
    public byte[] generateImage(String prompt, ImageGenerationRequest request) {
        log.info("[HuggingFace] Generating image for prompt: '{}' using model: {}",
                truncate(prompt, 80), props.getModel());

        // ── Build HF-specific request body ────────────────────────────────────
        Map<String, Object> requestBody = buildRequestBody(prompt, request);

        // ── Call HF API ───────────────────────────────────────────────────────
        try {
            byte[] imageBytes = webClient
                    .post()
                    .uri("/models/" + props.getModel())
                    // Accept both PNG and JPEG responses
                    .accept(MediaType.IMAGE_PNG, MediaType.IMAGE_JPEG,
                            MediaType.APPLICATION_OCTET_STREAM)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(byte[].class)
                    // Timeout safety net (in addition to the Netty-level timeout)
                    .timeout(Duration.ofSeconds(props.getTimeoutSeconds()))
                    .block();

            if (imageBytes == null || imageBytes.length == 0) {
                throw new ImageGenerationException(
                        "HuggingFace returned an empty response for the model: " + props.getModel());
            }

            log.info("[HuggingFace] Image generated successfully — {} bytes", imageBytes.length);
            return imageBytes;

        } catch (WebClientResponseException ex) {
            handleApiError(ex);
            throw new ImageGenerationException("Unexpected error after handling API error"); // unreachable

        } catch (ImageGenerationException ex) {
            throw ex; // re-throw our own exceptions as-is

        } catch (Exception ex) {
            log.error("[HuggingFace] Unexpected error during image generation", ex);
            throw new ImageGenerationException(
                    "Failed to generate image: " + ex.getMessage(), ex);
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Build the HF Inference API JSON payload.
     * Applies defaults for any null optional parameters from the request.
     */
    private Map<String, Object> buildRequestBody(String prompt,
                                                  ImageGenerationRequest request) {
        Map<String, Object> parameters = new HashMap<>();

        // Apply defaults when the client didn't specify a value
        parameters.put("width",                getOrDefault(request.getWidth(),              1024));
        parameters.put("height",               getOrDefault(request.getHeight(),             1024));
        parameters.put("guidance_scale",       getOrDefault(request.getGuidanceScale(),       7.5));
        parameters.put("num_inference_steps",  getOrDefault(request.getNumInferenceSteps(),    25));

        if (request.getNegativePrompt() != null && !request.getNegativePrompt().isBlank()) {
            parameters.put("negative_prompt", request.getNegativePrompt());
        }

        Map<String, Object> body = new HashMap<>();
        body.put("inputs", prompt);
        body.put("parameters", parameters);

        return body;
    }

    /**
     * Parse HF error responses and throw appropriate exceptions.
     *
     * HF returns:
     *  - 503 → model is loading (cold start); body contains "estimated_time"
     *  - 400 → bad request (invalid model or parameters)
     *  - 401 → invalid/missing API key
     *  - 429 → rate limit exceeded
     */
    private void handleApiError(WebClientResponseException ex) {
        int statusCode = ex.getStatusCode().value();
        String responseBody = ex.getResponseBodyAsString();

        log.error("[HuggingFace] API error — HTTP {}: {}", statusCode, responseBody);

        String message = switch (statusCode) {
            case 401 -> "Invalid or missing HuggingFace API key. " +
                        "Ensure HF_API_KEY environment variable is set correctly.";
            case 403 -> "Access denied to model '" + props.getModel() + "'. " +
                        "The model may require agreeing to its terms on HuggingFace.";
            case 429 -> "HuggingFace API rate limit exceeded. Please try again later.";
            case 503 -> {
                // Extract estimated_time if present in the HF 503 body
                String estimate = extractEstimatedTime(responseBody);
                yield "The model is currently loading on HuggingFace. " +
                      (estimate != null ? "Estimated wait: " + estimate + " seconds. " : "") +
                      "Please try again in a moment.";
            }
            default -> "HuggingFace API returned HTTP " + statusCode +
                       ". Details: " + truncate(responseBody, 200);
        };

        throw new ImageGenerationException(message);
    }

    /** Extract "estimated_time" value from HF 503 JSON body, or null. */
    private String extractEstimatedTime(String responseBody) {
        if (responseBody == null) return null;
        try {
            // Simple string parsing to avoid pulling in Jackson manually here
            // (Jackson is already on the classpath via Spring Boot)
            int idx = responseBody.indexOf("estimated_time");
            if (idx < 0) return null;
            String after = responseBody.substring(idx + "estimated_time".length() + 2);
            int end = after.indexOf(',');
            if (end < 0) end = after.indexOf('}');
            if (end < 0) return null;
            String value = after.substring(0, end).trim();
            // Round to nearest second for readability
            double seconds = Double.parseDouble(value);
            return String.valueOf((int) Math.ceil(seconds));
        } catch (Exception e) {
            return null; // non-critical — just for UX message
        }
    }

    /** Return value if non-null, otherwise fallback. Generic helper. */
    private <T> T getOrDefault(T value, T defaultValue) {
        return value != null ? value : defaultValue;
    }

    /** Truncate a string for log output. */
    private String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...";
    }
}
