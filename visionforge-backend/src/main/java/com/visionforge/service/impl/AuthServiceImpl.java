package com.visionforge.service.impl;

import com.visionforge.dto.request.LoginRequest;
import com.visionforge.dto.request.RegisterRequest;
import com.visionforge.dto.response.AuthResponse;
import com.visionforge.dto.response.UserResponse;
import com.visionforge.entity.User;
import com.visionforge.exception.InvalidCredentialsException;
import com.visionforge.exception.UserAlreadyExistsException;
import com.visionforge.exception.UserNotFoundException;
import com.visionforge.repository.UserRepository;
import com.visionforge.security.JwtUtil;
import com.visionforge.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * AuthServiceImpl — Business logic implementation for authentication.
 *
 * Rules:
 *  - No HTTP knowledge (no HttpServletRequest / ResponseEntity here)
 *  - No direct SQL (uses repository methods only)
 *  - All cross-cutting concerns (logging, transactions) applied via annotations
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository      userRepository;
    private final PasswordEncoder     passwordEncoder;
    private final JwtUtil             jwtUtil;
    private final AuthenticationManager authenticationManager;

    // ── REGISTER ─────────────────────────────────────────────────────────

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {

        log.info("Registration attempt — email: {}", request.getEmail());

        // Duplicate email check
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException(
                "An account with email '" + request.getEmail() + "' already exists"
            );
        }

        // Duplicate username check
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException(
                "Username '" + request.getUsername() + "' is already taken"
            );
        }

        // Build user entity — BCrypt hashes the password irreversibly
        User newUser = User.builder()
                .fullName(request.getFullName())
                .username(request.getUsername().toLowerCase())
                .email(request.getEmail().toLowerCase())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();
        // role=USER, plan=FREE, credits=30 come from @Builder.Default in entity

        User savedUser = userRepository.save(newUser);
        log.info("User registered — id: {}, email: {}", savedUser.getId(), savedUser.getEmail());

        String token = generateTokenWithClaims(savedUser);
        return buildAuthResponse(token, savedUser);
    }

    // ── LOGIN ─────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {

        log.info("Login attempt — email: {}", request.getEmail());

        try {
            // Delegates to DaoAuthenticationProvider:
            //  1. Loads user via UserDetailsServiceImpl.loadUserByUsername(email)
            //  2. Calls BCrypt.matches(raw, hash)
            //  3. Throws BadCredentialsException if either step fails
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
        } catch (BadCredentialsException e) {
            // Never reveal whether the email or password was wrong
            throw new InvalidCredentialsException();
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(UserNotFoundException::new);

        String token = generateTokenWithClaims(user);
        log.info("User logged in — id: {}", user.getId());
        return buildAuthResponse(token, user);
    }

    // ── GET CURRENT USER ──────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new UserNotFoundException("Authenticated user not found"));
        return mapToUserResponse(user);
    }

    // ── PRIVATE HELPERS ───────────────────────────────────────────────────

    /**
     * Generates a JWT with extra claims embedded in the payload.
     * These avoid extra DB lookups for common values the frontend needs.
     */
    private String generateTokenWithClaims(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("role",   user.getRole().name());
        claims.put("plan",   user.getPlan().name());
        return jwtUtil.generateToken(claims, user);
    }

    /** Builds the AuthResponse DTO — shared by register() and login(). */
    private AuthResponse buildAuthResponse(String token, User user) {
        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresAt(LocalDateTime.now().plusSeconds(86400)) // 24 h
                .user(mapToUserResponse(user))
                .build();
    }

    /** Maps User entity → UserResponse DTO. Password is never included. */
    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .username(user.getActualUsername())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .plan(user.getPlan())
                .credits(user.getCredits())
                .emailVerified(user.isEmailVerified())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
