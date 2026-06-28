package com.visionforge.exception;

/**
 * InsufficientCreditsException — Thrown when a user attempts to generate
 * an image but has 0 credits remaining.
 *
 * Mapped to HTTP 402 Payment Required by GlobalExceptionHandler.
 * HTTP 402 is the semantic standard for quota/billing limits.
 */
public class InsufficientCreditsException extends RuntimeException {

    public InsufficientCreditsException(String message) {
        super(message);
    }

    public InsufficientCreditsException(String message, Throwable cause) {
        super(message, cause);
    }
}
