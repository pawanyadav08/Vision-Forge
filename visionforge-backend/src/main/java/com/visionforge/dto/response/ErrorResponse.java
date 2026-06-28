package com.visionforge.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * ErrorResponse — Standardised error envelope for ALL errors.
 *
 * Example JSON (validation error):
 * {
 *   "status": 400,
 *   "error": "Validation Failed",
 *   "message": "One or more fields have invalid values",
 *   "path": "/api/auth/register",
 *   "timestamp": "2026-06-27T21:30:00",
 *   "fieldErrors": {
 *     "email": "Please provide a valid email address"
 *   }
 * }
 *
 * @JsonInclude(NON_NULL): null fields are omitted from JSON output.
 * 'fieldErrors' only appears for validation errors.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private int status;
    private String error;
    private String message;
    private String path;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    /** Only present for MethodArgumentNotValidException */
    private Map<String, String> fieldErrors;
}
