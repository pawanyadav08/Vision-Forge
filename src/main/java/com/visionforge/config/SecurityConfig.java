package com.visionforge.config;

import com.visionforge.security.JwtAuthenticationFilter;
import com.visionforge.security.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * SecurityConfig — Central Spring Security configuration.
 *
 * Defines:
 *  - Public vs protected endpoints
 *  - Stateless JWT session (no HttpSession)
 *  - BCrypt password encoding
 *  - JWT filter insertion point
 *  - DaoAuthenticationProvider wiring
 *
 * @EnableMethodSecurity: Enables @PreAuthorize on individual controller methods.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserDetailsServiceImpl  userDetailsService;

    /** Endpoints accessible without a JWT token. */
    private static final String[] PUBLIC_PATHS = {
            "/api/auth/**",       // Register, login, health
            "/swagger-ui/**",     // Swagger UI assets
            "/swagger-ui.html",
            "/v3/api-docs/**",    // OpenAPI JSON spec
            "/actuator/health",   // Health check
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CSRF disabled — stateless JWT auth doesn't need it
            .csrf(AbstractHttpConfigurer::disable)

            // Apply CORS rules from CorsConfig bean
            .cors(cors -> cors.configure(http))

            // Authorization rules (first match wins)
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(PUBLIC_PATHS).permitAll()
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .anyRequest().authenticated()
            )

            // Stateless: Spring creates no HTTP sessions
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // Wire our DaoAuthenticationProvider
            .authenticationProvider(authenticationProvider())

            // JWT filter runs BEFORE the standard username/password filter
            .addFilterBefore(jwtAuthenticationFilter,
                    UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * BCryptPasswordEncoder with strength 10.
     * Slow by design — makes brute-force attacks expensive.
     * Every password gets a unique random salt automatically.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    /**
     * DaoAuthenticationProvider:
     *  1. Loads user via UserDetailsService.loadUserByUsername(email)
     *  2. Verifies password with passwordEncoder.matches(raw, hashed)
     *  3. Throws BadCredentialsException on failure
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * Exposes Spring Boot's auto-configured AuthenticationManager as a bean
     * so AuthServiceImpl can inject and call it for login.
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
