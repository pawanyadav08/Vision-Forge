package com.visionforge.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a JWT token has passed its expiration date.
 * Maps to HTTP 401 Unauthorized.
 * The frontend should redirect the user to the login page on receiving this.
 */
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class TokenExpiredException extends RuntimeException {
    public TokenExpiredException() {
        super("Your session has expired. Please log in again.");
    }
}
