package com.visionforge.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JwtAuthenticationFilter — Intercepts every incoming HTTP request.
 *
 * Per-request flow:
 *  1. Read the "Authorization: Bearer <token>" header
 *  2. Extract and validate the JWT
 *  3. Load the user from the database
 *  4. Set authentication into Spring's SecurityContext
 *  5. Continue the filter chain
 *
 * OncePerRequestFilter guarantees this filter runs exactly once per request.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest  request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain         filterChain
    ) throws ServletException, IOException {

        // ── 1. Read Authorization header
        final String authHeader = request.getHeader("Authorization");

        // No Bearer token present — skip JWT processing, continue chain
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // ── 2. Extract the raw JWT (drop the "Bearer " prefix)
        final String jwt = authHeader.substring(7);

        try {
            // ── 3. Extract email from token payload
            final String userEmail = jwtUtil.extractUsername(jwt);

            // Only authenticate if we have an email and no auth is set yet
            if (userEmail != null &&
                    SecurityContextHolder.getContext().getAuthentication() == null) {

                // ── 4. Load full user from DB
                UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

                // ── 5. Validate token (signature + expiry + email match)
                if (jwtUtil.isTokenValid(jwt, userDetails)) {

                    // ── 6. Build Spring Security authentication token
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,                          // no credentials needed post-auth
                                    userDetails.getAuthorities()
                            );
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );

                    // ── 7. Register authentication in SecurityContext
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.debug("Authenticated '{}' for [{}]", userEmail, request.getRequestURI());
                }
            }
        } catch (Exception e) {
            // Invalid / expired token — log and continue without authenticating.
            // SecurityConfig will block the request if the endpoint requires auth.
            log.warn("JWT processing failed for [{}]: {}", request.getRequestURI(), e.getMessage());
        }

        // ── 8. Always continue the filter chain
        filterChain.doFilter(request, response);
    }
}
