package com.visionforge.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * OpenApiConfig — Configures Swagger UI / OpenAPI 3 documentation.
 *
 * Access points after startup:
 *   Swagger UI  → http://localhost:8080/swagger-ui.html
 *   API JSON    → http://localhost:8080/v3/api-docs
 *
 * The @SecurityScheme adds an "Authorize 🔒" button in Swagger UI
 * so testers can paste their JWT and call protected endpoints directly.
 */
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title       = "VisionForge AI — Backend API",
        version     = "1.0.0",
        description = """
            **VisionForge AI** — Text-to-image AI SaaS platform.

            ## Phase 2: Authentication & User Management

            ### How to authenticate in Swagger UI
            1. `POST /api/auth/register` — create an account.
            2. Copy the `accessToken` from the response.
            3. Click the **Authorize 🔒** button above.
            4. Enter: `Bearer <your_token>`
            5. Protected endpoints will now work.
            """,
        contact = @Contact(
            name  = "VisionForge Team",
            email = "support@visionforge.ai",
            url   = "https://visionforge.ai"
        ),
        license = @License(
            name = "MIT License",
            url  = "https://opensource.org/licenses/MIT"
        )
    ),
    servers = {
        @Server(url = "http://localhost:8080",       description = "Local Development"),
        @Server(url = "https://api.visionforge.ai",  description = "Production")
    }
)
@SecurityScheme(
    name          = "bearerAuth",
    type          = SecuritySchemeType.HTTP,
    scheme        = "bearer",
    bearerFormat  = "JWT",
    description   = "Paste your JWT token. Format: Bearer <token>"
)
public class OpenApiConfig {
    // All configuration is via class-level annotations — no @Bean methods needed.
}
