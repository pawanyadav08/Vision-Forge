package com.visionforge.service;

import com.visionforge.dto.request.LoginRequest;
import com.visionforge.dto.request.RegisterRequest;
import com.visionforge.dto.response.AuthResponse;
import com.visionforge.dto.response.UserResponse;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * AuthService — Contract for all authentication operations.
 *
 * Declaring an interface follows the Dependency Inversion Principle:
 *  - The controller depends on this abstraction, not the concrete class.
 *  - Easy to mock in unit tests.
 *  - The implementation can be swapped without touching the controller.
 */
public interface AuthService {

    /**
     * Register a new user account.
     * @throws com.visionforge.exception.UserAlreadyExistsException
     *         if email or username is already taken.
     */
    AuthResponse register(RegisterRequest request);

    /**
     * Authenticate an existing user.
     * @throws com.visionforge.exception.InvalidCredentialsException
     *         if email or password is incorrect.
     */
    AuthResponse login(LoginRequest request);

    /**
     * Return the profile of the currently authenticated user.
     * The userDetails parameter is injected from the SecurityContext
     * by Spring via @AuthenticationPrincipal in the controller.
     */
    UserResponse getCurrentUser(UserDetails userDetails);
}
