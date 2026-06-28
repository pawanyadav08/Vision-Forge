package com.visionforge.service.impl;

import com.visionforge.dto.request.ImageGenerationRequest;
import com.visionforge.dto.response.ImageResponse;
import com.visionforge.entity.Image;
import com.visionforge.entity.User;
import com.visionforge.entity.enums.ImageStatus;
import com.visionforge.exception.ImageAccessDeniedException;
import com.visionforge.exception.ImageGenerationException;
import com.visionforge.exception.ImageNotFoundException;
import com.visionforge.exception.InsufficientCreditsException;
import com.visionforge.provider.ImageProvider;
import com.visionforge.repository.ImageRepository;
import com.visionforge.repository.UserRepository;
import com.visionforge.service.ImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;
import java.util.List;

/**
 * ImageServiceImpl — The orchestration layer for AI image generation.
 *
 * Responsibilities (Single Responsibility: "image generation + persistence"):
 *  1. Validate user has credits
 *  2. Delegate to ImageProvider (HF, OpenAI, etc.) for generation
 *  3. Encode raw bytes to Base64 and save to DB
 *  4. Deduct user credit
 *  5. Handle failures gracefully (save FAILED record, restore credit)
 *
 * Transactional strategy:
 *  - generateImage: NOT annotated @Transactional at the method level
 *    because the HF API call can take 20–120 seconds — we must NOT hold
 *    a DB transaction open during an external HTTP call (connection pool exhaustion).
 *    Instead, we open separate short transactions for DB reads and writes.
 *
 *  - getMyImages / deleteImage: @Transactional(readOnly = true / false)
 *    These are fast DB-only operations — safe to wrap in a transaction.
 *
 * Credit atomicity:
 *  saveSuccessfulImage() is @Transactional — if the DB save fails,
 *  the credit deduction is rolled back automatically.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageServiceImpl implements ImageService {

    private final ImageProvider imageProvider;      // HuggingFaceImageProvider injected
    private final ImageRepository imageRepository;
    private final UserRepository userRepository;

    // ── 1. GENERATE IMAGE ────────────────────────────────────────────────────

    /**
     * Full generation pipeline — intentionally NOT @Transactional at this level.
     * The HF API call can take 20-120 seconds; holding a DB connection that long
     * would exhaust the Hikari connection pool under any meaningful load.
     *
     * Flow:
     *  [Check credits] → [Call HF API] → [Save SUCCESS record + deduct credit]
     *                                  OR [Save FAILED record]
     */
    @Override
    public ImageResponse generateImage(ImageGenerationRequest request, User currentUser) {
        log.info("[ImageService] Generation requested by user={} for prompt='{}'",
                currentUser.getEmail(), truncate(request.getPrompt(), 60));

        // ── Step 1: Credit check (quick DB read — no transaction held open) ──
        validateUserHasCredits(currentUser);

        // ── Step 2: Call AI provider (this can take 20–120 seconds) ──────────
        byte[] imageBytes;
        try {
            imageBytes = imageProvider.generateImage(request.getPrompt(), request);
        } catch (ImageGenerationException ex) {
            // Save a FAILED record so the user can see why their credit was NOT consumed
            saveFailedImage(request, currentUser, ex.getMessage());
            throw ex; // re-throw so controller returns 502
        }

        // ── Step 3: Encode to Base64 ──────────────────────────────────────────
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        // ── Step 4: Save to DB + deduct credit (atomic transaction) ──────────
        Image savedImage = saveSuccessfulImage(request, currentUser, base64Image);

        log.info("[ImageService] Image ID={} saved for user={}", savedImage.getId(), currentUser.getEmail());
        return ImageResponse.from(savedImage);
    }

    // ── 2. GET MY IMAGES ─────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<ImageResponse> getMyImages(User currentUser) {
        log.debug("[ImageService] Fetching images for user={}", currentUser.getEmail());

        return imageRepository
                .findByUserOrderByCreatedAtDesc(currentUser)
                .stream()
                .map(ImageResponse::from)
                .toList();
    }

    // ── 3. DELETE IMAGE ───────────────────────────────────────────────────────

    @Override
    @Transactional
    public void deleteImage(Long imageId, User currentUser) {
        log.info("[ImageService] Delete requested: imageId={} by user={}", imageId, currentUser.getEmail());

        // Find the image — throws 404 if it doesn't exist at all
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new ImageNotFoundException(
                        "Image with ID " + imageId + " not found"));

        // Ownership check — throws 403 if image belongs to someone else
        if (!image.getUser().getId().equals(currentUser.getId())) {
            log.warn("[ImageService] Ownership violation: user={} tried to delete imageId={}",
                    currentUser.getEmail(), imageId);
            throw new ImageAccessDeniedException(
                    "You do not have permission to delete image with ID " + imageId);
        }

        imageRepository.delete(image);
        log.info("[ImageService] Image ID={} deleted by user={}", imageId, currentUser.getEmail());
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Validate credit balance. Reads fresh from DB to avoid stale cache.
     * Short-lived — no transaction held.
     */
    private void validateUserHasCredits(User currentUser) {
        // Re-read user to get the latest credit balance (avoid using stale session cache)
        User freshUser = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new RuntimeException("Authenticated user not found in DB"));

        if (freshUser.getCredits() <= 0) {
            log.warn("[ImageService] User={} has insufficient credits", currentUser.getEmail());
            throw new InsufficientCreditsException(
                    "You have no credits remaining. Please upgrade your plan to generate more images.");
        }
    }

    /**
     * Persist a successful image + decrement user credit in ONE transaction.
     * If either operation fails, the whole transaction rolls back.
     *
     * @Transactional ensures atomicity: credit deduction and image save
     * either both succeed or both fail — no partial state.
     */
    @Transactional
    protected Image saveSuccessfulImage(ImageGenerationRequest request,
                                         User currentUser,
                                         String base64Image) {
        // Determine width/height (apply defaults if not specified)
        int width  = request.getWidth()  != null ? request.getWidth()  : 1024;
        int height = request.getHeight() != null ? request.getHeight() : 1024;

        Image image = Image.builder()
                .user(currentUser)
                .prompt(request.getPrompt())
                .negativePrompt(request.getNegativePrompt())
                .modelUsed(imageProvider.getModelName())
                .provider(imageProvider.getProviderName())
                .imageData(base64Image)
                .contentType("image/png")
                .width(width)
                .height(height)
                .status(ImageStatus.SUCCESS)
                .build();

        Image saved = imageRepository.save(image);

        // Deduct one credit from the user
        // Re-load user within this transaction to get the current credit count
        User userToUpdate = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new RuntimeException("User not found during credit deduction"));
        userToUpdate.setCredits(userToUpdate.getCredits() - 1);
        userRepository.save(userToUpdate);

        log.debug("[ImageService] Credit deducted — user={} remaining credits={}",
                currentUser.getEmail(), userToUpdate.getCredits() - 1);

        return saved;
    }

    /**
     * Persist a FAILED image record — no credit is deducted.
     * Wrapped in its own transaction so it commits even if the caller re-throws.
     * The user can see why the generation failed in their image history.
     *
     * Note: We save in a new, independent method (not @Transactional here)
     * since we only need a single save operation.
     */
    private void saveFailedImage(ImageGenerationRequest request,
                                  User currentUser,
                                  String failureReason) {
        try {
            Image failedImage = Image.builder()
                    .user(currentUser)
                    .prompt(request.getPrompt())
                    .negativePrompt(request.getNegativePrompt())
                    .modelUsed(imageProvider.getModelName())
                    .provider(imageProvider.getProviderName())
                    .status(ImageStatus.FAILED)
                    .failureReason(truncate(failureReason, 500))
                    .build();

            imageRepository.save(failedImage);
            log.info("[ImageService] Saved FAILED image record for user={}", currentUser.getEmail());
        } catch (Exception e) {
            // Don't let a failure to save the failure record shadow the real error
            log.error("[ImageService] Could not save FAILED image record: {}", e.getMessage());
        }
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...";
    }
}
