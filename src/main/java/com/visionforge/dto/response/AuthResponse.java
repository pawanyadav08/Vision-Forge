package com.visionforge.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AuthResponse — Returned by both /register and /login.
 *
 * Contains the JWT access token and basic user info so the
 * frontend does not need a second /me call right after login.
 */
@Data
@Builder
public class AuthResponse {

    private String accessToken;

    @Builder.Default
    private String tokenType = "Bearer";

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime expiresAt;

    private UserResponse user;
}
