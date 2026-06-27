package com.visionforge;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test — verifies the Spring ApplicationContext loads without errors.
 * This catches misconfigured beans, missing properties, and wiring issues.
 *
 * NOTE: Requires a running PostgreSQL instance with the credentials
 * defined in application.yml. For CI, use @DataJpaTest with H2 instead.
 */
@SpringBootTest
@ActiveProfiles("test")
class VisionforgeBackendApplicationTests {

    @Test
    void contextLoads() {
        // If the Spring context starts successfully this test passes.
        // No assertions needed — a failed context startup throws an exception.
    }
}
