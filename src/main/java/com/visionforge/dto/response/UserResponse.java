package com.visionforge.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.visionforge.entity.enums.Plan;
import com.visionforge.entity.enums.Role;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * UserResponse — Public-safe user profile data.
 *
 * IMPORTANT: No 'password' field here.
 * DTOs are the gate — we control exactly what leaves the server.
 */
@Data
@Builder
public class UserResponse {

    private Long id;
    private String fullName;
    private String username;
    private String email;
    private String avatarUrl;
    private Role role;
    private Plan plan;
    private Integer credits;
    private boolean emailVerified;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
}
