package com.visionforge.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * CorsConfig — Defines which frontend origins can call our backend.
 *
 * Without CORS config the browser blocks all cross-origin requests
 * from the React frontend (different port = different origin).
 * Spring Security reads the CorsConfigurationSource bean automatically
 * because we do .cors(cors -> cors.configure(http)) in SecurityConfig.
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();

        // Allowed frontend origins (add your production domain here later)
        configuration.setAllowedOrigins(List.of(
                "http://localhost:3000",      // React CRA dev server
                "http://localhost:5173",      // Vite dev server
                "https://visionforge.ai",     // Production (future)
                "https://www.visionforge.ai"
        ));

        // Allowed HTTP methods
        configuration.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));

        // Allowed request headers
        configuration.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Accept",
                "Origin",
                "X-Requested-With",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers"
        ));

        // Response headers accessible to the browser's JS code
        configuration.setExposedHeaders(List.of(
                "Authorization",
                "Content-Disposition"
        ));

        // Required when sending Authorization header from browser
        configuration.setAllowCredentials(true);

        // Browser caches the CORS preflight response for 1 hour (fewer OPTIONS calls)
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
