package com.visionforge.exception;

import com.visionforge.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * GlobalExceptionHandler — Centralised error handling for ALL controllers.
 *
 * @RestControllerAdvice intercepts exceptions thrown from any @RestController
 * and maps them to structured ErrorResponse JSON — one consistent format
 * for every error the API can produce.
 *
 * Phase 3 additions: handlers for image-specific exceptions
 *   (#9)  ImageNotFoundException       → 404
 *   (#10) ImageAccessDeniedException   → 403
 *   (#11) InsufficientCreditsException → 402
 *   (#12) ImageGenerationException     → 502
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── 1. VALIDATION: @Valid on request body fails ───────────────────────
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field = ((FieldError) error).getField();
            String msg   = error.getDefaultMessage();
            fieldErrors.put(field, msg);
        });

        ErrorResponse body = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message("One or more fields have invalid values")
                .path(request.getRequestURI())
                .fieldErrors(fieldErrors)
                .build();

        log.warn("Validation failed [{}]: {}", request.getRequestURI(), fieldErrors);
        return ResponseEntity.badRequest().body(body);
    }

    // ── 2. DUPLICATE REGISTRATION ─────────────────────────────────────────
    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUserAlreadyExists(
            UserAlreadyExistsException ex,
            HttpServletRequest request) {

        ErrorResponse body = ErrorResponse.builder()
                .status(HttpStatus.CONFLICT.value())
                .error("Conflict")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        log.warn("Duplicate user attempt [{}]: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    // ── 3. USER NOT FOUND ─────────────────────────────────────────────────
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(
            UserNotFoundException ex,
            HttpServletRequest request) {

        ErrorResponse body = ErrorResponse.builder()
                .status(HttpStatus.NOT_FOUND.value())
                .error("Not Found")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    // ── 4. INVALID CREDENTIALS ────────────────────────────────────────────
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(
            InvalidCredentialsException ex,
            HttpServletRequest request) {

        ErrorResponse body = ErrorResponse.builder()
                .status(HttpStatus.UNAUTHORIZED.value())
                .error("Unauthorized")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        log.warn("Failed login attempt [{}]", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    // ── 5. TOKEN EXPIRED ──────────────────────────────────────────────────
    @ExceptionHandler(TokenExpiredException.class)
    public ResponseEntity<ErrorResponse> handleTokenExpired(
            TokenExpiredException ex,
            HttpServletRequest request) {

        ErrorResponse body = ErrorResponse.builder()
                .status(HttpStatus.UNAUTHORIZED.value())
                .error("Token Expired")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    // ── 6. ACCESS DENIED: authenticated but wrong role ───────────────────
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request) {

        ErrorResponse body = ErrorResponse.builder()
                .status(HttpStatus.FORBIDDEN.value())
                .error("Forbidden")
                .message("You do not have permission to access this resource")
                .path(request.getRequestURI())
                .build();

        log.warn("Access denied [{}]", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    // ── 7. SPRING SECURITY AUTH FAILURES ─────────────────────────────────
    @ExceptionHandler({AuthenticationException.class, BadCredentialsException.class})
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException ex,
            HttpServletRequest request) {

        ErrorResponse body = ErrorResponse.builder()
                .status(HttpStatus.UNAUTHORIZED.value())
                .error("Unauthorized")
                .message("Authentication failed")
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Phase 3 — Image-specific exception handlers
    // ═════════════════════════════════════════════════════════════════════════

    // ── 8. IMAGE NOT FOUND ────────────────────────────────────────────────
    @ExceptionHandler(ImageNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleImageNotFound(
            ImageNotFoundException ex,
            HttpServletRequest request) {

        ErrorResponse body = ErrorResponse.builder()
                .status(HttpStatus.NOT_FOUND.value())
                .error("Image Not Found")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    // ── 9. IMAGE ACCESS DENIED (non-owner tries to delete) ───────────────
    @ExceptionHandler(ImageAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleImageAccessDenied(
            ImageAccessDeniedException ex,
            HttpServletRequest request) {

        ErrorResponse body = ErrorResponse.builder()
                .status(HttpStatus.FORBIDDEN.value())
                .error("Forbidden")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        log.warn("Image ownership violation [{}]: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    // ── 10. INSUFFICIENT CREDITS ──────────────────────────────────────────
    // HTTP 402 Payment Required: the standard code for quota/billing limits
    @ExceptionHandler(InsufficientCreditsException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientCredits(
            InsufficientCreditsException ex,
            HttpServletRequest request) {

        ErrorResponse body = ErrorResponse.builder()
                .status(HttpStatus.PAYMENT_REQUIRED.value())
                .error("Insufficient Credits")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        log.warn("Insufficient credits [{}]: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(body);
    }

    // ── 11. IMAGE GENERATION FAILURE (provider error / timeout) ──────────
    // HTTP 502 Bad Gateway: our server received a bad response from upstream (HF)
    @ExceptionHandler(ImageGenerationException.class)
    public ResponseEntity<ErrorResponse> handleImageGenerationException(
            ImageGenerationException ex,
            HttpServletRequest request) {

        ErrorResponse body = ErrorResponse.builder()
                .status(HttpStatus.BAD_GATEWAY.value())
                .error("Image Generation Failed")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        log.error("Image generation failed [{}]: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(body);
    }

    // ── 12. CATCH-ALL: prevents raw stack traces leaking to client ────────
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request) {

        // Full stack trace for our logs only
        log.error("Unhandled exception at [{}]: ", request.getRequestURI(), ex);

        ErrorResponse body = ErrorResponse.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message("An unexpected error occurred. Please try again later.")
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
