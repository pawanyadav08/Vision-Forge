package com.visionforge.exception;

/**
 * ImageGenerationException — Thrown when the AI provider fails to generate
 * an image (API error, timeout, model loading, rate limit, etc.).
 *
 * Mapped to HTTP 502 Bad Gateway by GlobalExceptionHandler.
 * HTTP 502 is semantically correct: our server received an invalid response
 * from the upstream AI provider (HuggingFace, OpenAI, etc.).
 *
 * This exception is thrown by:
 *  - HuggingFaceImageProvider on 4xx/5xx HF API responses
 *  - HuggingFaceImageProvider on response timeout
 *  - HuggingFaceImageProvider when image bytes are empty
 */
public class ImageGenerationException extends RuntimeException {

    public ImageGenerationException(String message) {
        super(message);
    }

    public ImageGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
