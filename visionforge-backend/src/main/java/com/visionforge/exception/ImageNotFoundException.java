package com.visionforge.exception;

/**
 * ImageNotFoundException — Thrown when an image ID doesn't exist in the DB.
 *
 * Mapped to HTTP 404 Not Found by GlobalExceptionHandler.
 *
 * Thrown by: ImageServiceImpl.deleteImage() when the image ID is not found.
 */
public class ImageNotFoundException extends RuntimeException {

    public ImageNotFoundException(String message) {
        super(message);
    }

    public ImageNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
