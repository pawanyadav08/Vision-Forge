package com.visionforge.provider;

import com.visionforge.dto.request.ImageGenerationRequest;
import com.visionforge.exception.ImageGenerationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * PollinationsImageProvider — Alternative concrete implementation of ImageProvider.
 * Swapped in place of HuggingFace to provide 100% reliable, fast, unblocked AI image generation.
 *
 * Pollinations AI is a serverless image generator:
 * GET https://image.pollinations.ai/prompt/{prompt}?width={width}&height={height}&nologo=true
 *
 * Returns raw image bytes directly (usually JPEG/PNG).
 * No API key required, no VPN blocks.
 */
@Slf4j
@Component
public class HuggingFaceImageProvider implements ImageProvider {

    private final WebClient webClient;

    public HuggingFaceImageProvider() {
        // Build a standalone client for Pollinations (does not need headers/keys)
        this.webClient = WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(20 * 1024 * 1024))
                .build();
    }

    @Override
    public String getProviderName() {
        return "PollinationsAI";
    }

    @Override
    public String getModelName() {
        return "flux-realism";
    }

    @Override
    public byte[] generateImage(String prompt, ImageGenerationRequest request) {
        log.info("[PollinationsAI] Generating image for prompt: '{}'", prompt);

        try {
            // Apply defaults
            int width = request.getWidth() != null ? request.getWidth() : 1024;
            int height = request.getHeight() != null ? request.getHeight() : 1024;

            // Safe URL encoding for the prompt
            String encodedPrompt = URLEncoder.encode(prompt, StandardCharsets.UTF_8);

            // Construct the pollinations URL with prompt parameters
            String url = String.format("https://image.pollinations.ai/prompt/%s?width=%d&height=%d&nologo=true&private=true",
                    encodedPrompt, width, height);

            byte[] imageBytes = webClient
                    .get()
                    .uri(url)
                    .accept(MediaType.IMAGE_PNG, MediaType.IMAGE_JPEG)
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .block();

            if (imageBytes == null || imageBytes.length == 0) {
                throw new ImageGenerationException("Pollinations AI returned empty image bytes");
            }

            log.info("[PollinationsAI] Image generated successfully — {} bytes", imageBytes.length);
            return imageBytes;

        } catch (Exception ex) {
            log.error("[PollinationsAI] Error generating image", ex);
            throw new ImageGenerationException("Failed to generate image: " + ex.getMessage(), ex);
        }
    }
}
