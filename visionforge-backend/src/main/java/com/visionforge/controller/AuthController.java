package com.visionforge.controller;

import com.visionforge.dto.request.LoginRequest;
import com.visionforge.dto.request.RegisterRequest;
import com.visionforge.dto.response.AuthResponse;
import com.visionforge.dto.response.UserResponse;
import com.visionforge.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * AuthController — HTTP entry point for all authentication endpoints.
 *
 * Responsibilities:
 *   ✅ Accept HTTP requests and parse request bodies
 *   ✅ Trigger @Valid bean validation
 *   ✅ Delegate ALL logic to AuthService
 *   ✅ Return the correct HTTP status codes
 *   ✅ Document endpoints via Swagger annotations
 *
 * NOT responsible for:
 *   ❌ Business logic (AuthService handles that)
 *   ❌ Database access (Repository handles that)
 *   ❌ Password hashing (SecurityConfig handles that)
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register, login, and current-user profile APIs")
public class AuthController {

    private final AuthService authService;

    // ── POST /api/auth/register ───────────────────────────────────────────

    @Operation(
        summary     = "Register a new user",
        description = "Creates a new account and returns a JWT for immediate use."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "User registered successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error — invalid input"),
        @ApiResponse(responseCode = "409", description = "Email or username already exists")
    })
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request) {

        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ── POST /api/auth/login ──────────────────────────────────────────────

    @Operation(
        summary     = "Login with email and password",
        description = "Authenticates the user and returns a JWT token."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login successful"),
        @ApiResponse(responseCode = "400", description = "Validation error — missing fields"),
        @ApiResponse(responseCode = "401", description = "Invalid email or password")
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {

        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    // ── GET /api/auth/me ──────────────────────────────────────────────────

    @Operation(
        summary     = "Get current user profile",
        description = "Returns the profile of the authenticated user. Requires Bearer token.",
        security    = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Profile returned successfully"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token")
    })
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(
            @AuthenticationPrincipal UserDetails userDetails) {

        UserResponse response = authService.getCurrentUser(userDetails);
        return ResponseEntity.ok(response);
    }

    // ── GET /api/auth/health ──────────────────────────────────────────────

    @Operation(summary = "Health check", description = "Verify the service is running.")
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("VisionForge AI Backend is running \uD83D\uDE80");
    }
}
