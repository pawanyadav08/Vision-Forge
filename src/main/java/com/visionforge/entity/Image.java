package com.visionforge.entity;

import com.visionforge.entity.enums.ImageStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Image Entity — represents one AI-generated image in the system.
 *
 * Maps to the 'images' table in PostgreSQL.
 *
 * Design notes:
 *  - Linked to User via a ManyToOne FK (one user → many images).
 *  - imageData stores the raw image as a Base64-encoded string (TEXT column).
 *    This is intentional for Phase 3 (no S3/object-storage configured yet).
 *    To migrate to S3 later: replace imageData with imageUrl (VARCHAR), add
 *    an S3Service, and update HuggingFaceImageProvider → zero business logic changes.
 *  - modelUsed records exactly which AI model generated the image, enabling
 *    future per-model analytics and billing.
 *  - failureReason is nullable — only populated when status = FAILED,
 *    so we never lose track of why a credit was consumed.
 *  - width / height are optional metadata; HF API doesn't always return them
 *    but we default sensible values in the service.
 */
@Entity
@Table(
    name = "images",
    indexes = {
        // Fast lookup: "all images for user X, newest first"
        @Index(name = "idx_images_user_id",    columnList = "user_id"),
        // Fast lookup: "all images with status X"
        @Index(name = "idx_images_status",     columnList = "status"),
        // Composite: user's images by date (common query pattern)
        @Index(name = "idx_images_user_created", columnList = "user_id, created_at DESC")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Image {

    // ── PRIMARY KEY ──────────────────────────────────────────────────────────
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── OWNER ────────────────────────────────────────────────────────────────
    /**
     * The user who requested this image.
     * FetchType.LAZY: don't JOIN to users table unless explicitly accessed.
     * nullable = false: an image must always belong to a user.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_images_user"))
    private User user;

    // ── GENERATION INPUTS ────────────────────────────────────────────────────
    /**
     * The text prompt submitted by the user.
     * columnDefinition = "TEXT" allows prompts longer than 255 chars.
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String prompt;

    /**
     * Negative prompt — things to exclude from the image.
     * Optional: not all models / providers support it.
     */
    @Column(columnDefinition = "TEXT")
    private String negativePrompt;

    /**
     * The exact model identifier used (e.g. "stabilityai/stable-diffusion-xl-base-1.0").
     * Stored so we can support multiple models and track usage per model.
     */
    @Column(nullable = false, length = 200)
    private String modelUsed;

    // ── GENERATION OUTPUT ────────────────────────────────────────────────────
    /**
     * Base64-encoded image bytes.
     * @Lob maps to PostgreSQL TEXT (unbounded).
     * Nullable: will be null if status = FAILED.
     */
    @Lob
    @Column(name = "image_data", columnDefinition = "TEXT")
    private String imageData;

    /**
     * MIME type of the image (e.g. "image/png", "image/jpeg").
     * HuggingFace Inference API typically returns image/jpeg or image/png.
     */
    @Column(length = 50)
    @Builder.Default
    private String contentType = "image/png";

    /** Image width in pixels. Default 1024 for SDXL. */
    @Column
    @Builder.Default
    private Integer width = 1024;

    /** Image height in pixels. Default 1024 for SDXL. */
    @Column
    @Builder.Default
    private Integer height = 1024;

    // ── STATUS & METADATA ────────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ImageStatus status = ImageStatus.PENDING;

    /**
     * Populated only when status = FAILED.
     * Stores the provider error message so we can debug and inform the user.
     */
    @Column(columnDefinition = "TEXT")
    private String failureReason;

    /**
     * Human-readable name for the provider (e.g. "HuggingFace", "OpenAI").
     * Allows multi-provider analytics without joining to a separate table.
     */
    @Column(nullable = false, length = 50)
    @Builder.Default
    private String provider = "HuggingFace";

    // ── AUDIT TIMESTAMPS ─────────────────────────────────────────────────────
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
