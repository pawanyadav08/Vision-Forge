package com.visionforge.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.visionforge.entity.Image;
import com.visionforge.entity.enums.ImageStatus;
import lombok.*;

import java.time.LocalDateTime;

/**
 * ImageResponse — DTO returned to the client for every image operation.
 *
 * This class is the single, stable contract between the backend and frontend.
 * The internal Image entity and User entity are NEVER serialized directly —
 * doing so would leak password hashes, internal IDs, and Hibernate proxies.
 *
 * Key design decision — imageDataUrl:
 *   The DB stores raw Base64 bytes. This DTO assembles the browser-ready
 *   Data URL: "data:image/png;base64,<base64string>"
 *   The frontend can set this directly as <img src={imageDataUrl} /> with
 *   zero additional processing.
 *
 * @JsonInclude(NON_NULL): Fields that are null (e.g. failureReason on success,
 *   imageDataUrl on failure) are omitted from the JSON response entirely,
 *   keeping the payload clean.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ImageResponse {

    /** Database primary key — used by the frontend for DELETE calls. */
    private Long id;

    /** The text prompt that generated this image. */
    private String prompt;

    /** Optional negative prompt, if provided. */
    private String negativePrompt;

    /**
     * Browser-ready Data URL: "data:image/png;base64,<base64string>"
     * Null when status = FAILED (no image was produced).
     */
    private String imageDataUrl;

    /** Content type (e.g. "image/png", "image/jpeg"). */
    private String contentType;

    /** Image width in pixels. */
    private Integer width;

    /** Image height in pixels. */
    private Integer height;

    /** Generation status: PENDING, SUCCESS, or FAILED. */
    private ImageStatus status;

    /** Why generation failed. Null on success. */
    private String failureReason;

    /** Which AI model was used (e.g. "stabilityai/stable-diffusion-xl-base-1.0"). */
    private String modelUsed;

    /** Which provider was used (e.g. "HuggingFace", "OpenAI"). */
    private String provider;

    /** When the image was created. */
    private LocalDateTime createdAt;

    // ── Static factory method ─────────────────────────────────────────────────

    /**
     * Convert an Image entity to an ImageResponse DTO.
     *
     * This static factory centralises the mapping logic in one place.
     * Using a static factory on the DTO (rather than a separate Mapper class)
     * is appropriate here since the mapping is simple and we don't use MapStruct.
     *
     * @param image the Image entity from the database
     * @return a fully populated ImageResponse (imageDataUrl assembled here)
     */
    public static ImageResponse from(Image image) {
        // Assemble the data URL only when imageData is present (SUCCESS case)
        String dataUrl = null;
        if (image.getImageData() != null && !image.getImageData().isBlank()) {
            dataUrl = "data:" + image.getContentType() + ";base64," + image.getImageData();
        }

        return ImageResponse.builder()
                .id(image.getId())
                .prompt(image.getPrompt())
                .negativePrompt(image.getNegativePrompt())
                .imageDataUrl(dataUrl)
                .contentType(image.getContentType())
                .width(image.getWidth())
                .height(image.getHeight())
                .status(image.getStatus())
                .failureReason(image.getFailureReason())
                .modelUsed(image.getModelUsed())
                .provider(image.getProvider())
                .createdAt(image.getCreatedAt())
                .build();
    }
}
