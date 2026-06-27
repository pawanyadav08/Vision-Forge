package com.visionforge.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when login fails due to wrong email or password.
 * Maps to HTTP 401 Unauthorized.
 *
 * SECURITY NOTE: The message is intentionally generic.
 * We never reveal whether the email doesn't exist OR the
 * password is wrong — this prevents user-enumeration attacks.
 */
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() {
        super("Invalid email or password");
    }
}
