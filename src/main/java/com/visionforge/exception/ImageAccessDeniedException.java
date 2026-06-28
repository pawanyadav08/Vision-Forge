package com.visionforge.exception;

/**
 * ImageAccessDeniedException — Thrown when a user tries to delete an image
 * that belongs to a different user.
 *
 * Mapped to HTTP 403 Forbidden by GlobalExceptionHandler.
 *
 * Note: We use a custom exception rather than Spring's AccessDeniedException
 * because we want to provide a specific, user-friendly message that includes
 * context (e.g. "You do not have permission to delete image with ID 42").
 * Spring's AccessDeniedException is caught before reaching our handler.
 */
public class ImageAccessDeniedException extends RuntimeException {

    public ImageAccessDeniedException(String message) {
        super(message);
    }

    public ImageAccessDeniedException(String message, Throwable cause) {
        super(message, cause);
    }
}
