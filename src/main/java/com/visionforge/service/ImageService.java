package com.visionforge.service;

import com.visionforge.dto.request.ImageGenerationRequest;
import com.visionforge.dto.response.ImageResponse;
import com.visionforge.entity.User;

import java.util.List;

/**
 * ImageService — Contract for all image generation and management operations.
 *
 * Declared as an interface following the Dependency Inversion Principle:
 *  - ImageController depends on this abstraction, not on ImageServiceImpl
 *  - Easy to mock in unit tests via @MockBean / Mockito
 *  - The implementation can be swapped or decorated without touching the controller
 *
 * All methods receive the authenticated User entity (extracted from the
 * SecurityContext by @AuthenticationPrincipal in the controller).
 * The service never calls SecurityContextHolder directly — that's the controller's job.
 */
public interface ImageService {

    /**
     * Generate a new image using the configured AI provider.
     *
     * Responsibilities:
     *  1. Validate the user has sufficient credits
     *  2. Delegate to the ImageProvider to generate the image
     *  3. Convert raw bytes to Base64
     *  4. Persist the Image entity to PostgreSQL
     *  5. Deduct one credit from the user
     *
     * @param request   the validated generation parameters (prompt, size, etc.)
     * @param currentUser the authenticated user making the request
     * @return ImageResponse with the generated image data URL and metadata
     * @throws com.visionforge.exception.InsufficientCreditsException
     *         if the user has 0 credits remaining
     * @throws com.visionforge.exception.ImageGenerationException
     *         if the AI provider fails to generate the image
     */
    ImageResponse generateImage(ImageGenerationRequest request, User currentUser);

    /**
     * Retrieve all images belonging to the authenticated user.
     * Returns newest-first. Excludes no records (includes FAILED attempts).
     *
     * @param currentUser the authenticated user
     * @return list of ImageResponse DTOs, newest first; empty list if none
     */
    List<ImageResponse> getMyImages(User currentUser);

    /**
     * Delete an image by ID.
     * Only the owner of the image can delete it.
     *
     * @param imageId     the ID of the image to delete
     * @param currentUser the authenticated user attempting the deletion
     * @throws com.visionforge.exception.ImageNotFoundException
     *         if no image with the given ID exists in the database
     * @throws com.visionforge.exception.ImageAccessDeniedException
     *         if the image exists but belongs to a different user
     */
    void deleteImage(Long imageId, User currentUser);
}
