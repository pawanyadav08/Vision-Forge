package com.visionforge.provider;

import com.visionforge.dto.request.ImageGenerationRequest;

/**
 * ImageProvider — Abstraction for all AI image generation providers.
 *
 * ═══════════════════════════════════════════════════════════════════════════
 * SOLID PRINCIPLE: Open/Closed Principle (OCP)
 * ═══════════════════════════════════════════════════════════════════════════
 * The system is OPEN for extension (add new providers) but CLOSED for
 * modification (ImageServiceImpl never changes when a new provider is added).
 *
 * Current implementations:
 *   └── HuggingFaceImageProvider (Phase 3)
 *
 * Future implementations (no changes to service layer required):
 *   └── OpenAiImageProvider          (DALL-E 3)
 *   └── StabilityAiImageProvider     (Stability AI API)
 *   └── GoogleVertexImageProvider    (Imagen 3)
 *   └── MockImageProvider            (testing, CI/CD)
 *
 * ═══════════════════════════════════════════════════════════════════════════
 * SOLID PRINCIPLE: Dependency Inversion Principle (DIP)
 * ═══════════════════════════════════════════════════════════════════════════
 * High-level modules (ImageServiceImpl) depend on THIS abstraction,
 * not on concrete classes (HuggingFaceImageProvider).
 *
 * Usage in ImageServiceImpl:
 * <pre>{@code
 *   // Spring injects the correct provider (e.g. HuggingFaceImageProvider)
 *   private final ImageProvider imageProvider;
 * }</pre>
 *
 * To support multiple providers simultaneously, change ImageServiceImpl
 * to accept a List<ImageProvider> and select by getProviderName().
 */
public interface ImageProvider {

    /**
     * Generate an image from a text prompt.
     *
     * The provider is responsible for:
     *  1. Building the API-specific request payload
     *  2. Calling the external API (with timeout handling)
     *  3. Returning the raw image bytes
     *
     * It is NOT responsible for:
     *  - Saving to the database  (ImageService's job)
     *  - Credit deduction        (ImageService's job)
     *  - Authentication          (Spring Security's job)
     *
     * @param prompt  the text description of the desired image
     * @param request the full generation request (width, height, steps, etc.)
     * @return raw binary image bytes (typically PNG or JPEG)
     * @throws com.visionforge.exception.ImageGenerationException
     *         if the provider API returns an error or times out
     */
    byte[] generateImage(String prompt, ImageGenerationRequest request);

    /**
     * Returns the canonical name of this provider.
     * Used for:
     *  - Logging and debugging
     *  - Storing in the Image.provider DB column
     *  - Selecting a specific provider at runtime (multi-provider support)
     *
     * @return provider name, e.g. "HuggingFace", "OpenAI", "StabilityAI"
     */
    String getProviderName();

    /**
     * Returns the model identifier currently configured for this provider.
     * Stored in Image.modelUsed for analytics and reproducibility.
     *
     * @return model ID, e.g. "stabilityai/stable-diffusion-xl-base-1.0"
     */
    String getModelName();
}
