package com.visionforge.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * ImageGenerationRequest — Validated DTO for POST /api/images/generate.
 *
 * The client sends this JSON body:
 * <pre>{@code
 * {
 *   "prompt": "a majestic dragon soaring over snowy mountains at sunset",
 *   "negativePrompt": "blurry, low quality, watermark",
 *   "width": 1024,
 *   "height": 1024,
 *   "guidanceScale": 7.5,
 *   "numInferenceSteps": 30
 * }
 * }</pre>
 *
 * Only 'prompt' is required. All other fields have sensible defaults
 * applied in ImageServiceImpl when null — we don't force the client
 * to specify every parameter.
 *
 * Validation annotations:
 *  @NotBlank  — rejects null, empty, and whitespace-only strings
 *  @Size      — enforces reasonable prompt length limits
 *  @Min/@Max  — guards against absurdly large images or step counts
 *               that would cause HF API timeouts / errors
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageGenerationRequest {

    /**
     * The main text prompt describing the desired image.
     * Required. Must be between 3 and 1000 characters.
     */
    @NotBlank(message = "Prompt must not be blank")
    @Size(min = 3, message = "Prompt must be at least 3 characters")
    @Size(max = 1000, message = "Prompt must not exceed 1000 characters")
    private String prompt;

    /**
     * Optional negative prompt — things to exclude from the image.
     * E.g. "blurry, low quality, ugly, deformed"
     * Not all providers / models support negative prompts.
     */
    @Size(max = 500, message = "Negative prompt must not exceed 500 characters")
    private String negativePrompt;

    /**
     * Image width in pixels.
     * Default: 1024 (SDXL standard).
     * Must be divisible by 8 (hardware constraint for latent diffusion models).
     * Validated at provider level — we keep the DTO simple.
     */
    @Min(value = 256,  message = "Width must be at least 256 pixels")
    @Max(value = 1024, message = "Width must not exceed 1024 pixels (free tier limit)")
    private Integer width;

    /**
     * Image height in pixels.
     * Default: 1024 (SDXL standard).
     */
    @Min(value = 256,  message = "Height must be at least 256 pixels")
    @Max(value = 1024, message = "Height must not exceed 1024 pixels (free tier limit)")
    private Integer height;

    /**
     * Classifier-Free Guidance scale.
     * Higher values → image adheres more strongly to the prompt.
     * Range 1.0–20.0; industry standard is 7.0–7.5.
     * Default: 7.5
     */
    @DecimalMin(value = "1.0",  message = "Guidance scale must be at least 1.0")
    @DecimalMax(value = "20.0", message = "Guidance scale must not exceed 20.0")
    private Double guidanceScale;

    /**
     * Number of diffusion steps.
     * More steps → higher quality but slower generation.
     * Range 10–50; typical production value is 20–30.
     * Default: 25
     */
    @Min(value = 10, message = "Inference steps must be at least 10")
    @Max(value = 50, message = "Inference steps must not exceed 50 (free tier limit)")
    private Integer numInferenceSteps;
}
