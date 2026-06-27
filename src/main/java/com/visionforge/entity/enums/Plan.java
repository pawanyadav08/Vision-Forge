package com.visionforge.entity.enums;

/**
 * Subscription tiers for VisionForge AI.
 * Determines credit limits, image quality caps, and feature access.
 */
public enum Plan {
    FREE,        //  30 credits/month, 512px max
    PRO,         // 500 credits/month, 1024px, commercial use
    ENTERPRISE   // Unlimited, API access, priority generation
}
