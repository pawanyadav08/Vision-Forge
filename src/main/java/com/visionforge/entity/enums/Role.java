package com.visionforge.entity.enums;

/**
 * User roles for authorization.
 * Stored as String in DB (not numeric) for readability.
 * Used in Spring Security via: "ROLE_" + role.name()
 */
public enum Role {
    USER,   // Regular user — can generate images, manage their gallery
    ADMIN   // Platform admin — can manage all users, view analytics
}
