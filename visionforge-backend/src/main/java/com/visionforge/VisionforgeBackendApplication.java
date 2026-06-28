package com.visionforge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * VisionforgeBackendApplication — Main entry point.
 *
 * @SpringBootApplication is shorthand for:
 *   @Configuration       — marks this as a config class
 *   @EnableAutoConfiguration — lets Spring Boot auto-wire dependencies
 *   @ComponentScan       — scans com.visionforge.** for all @Component beans
 */
@SpringBootApplication
public class VisionforgeBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(VisionforgeBackendApplication.class, args);
    }
}
