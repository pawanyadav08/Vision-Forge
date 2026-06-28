package com.visionforge.entity.enums;

/**
 * ImageStatus — Lifecycle states for an AI-generated image.
 *
 * Stored as EnumType.STRING in PostgreSQL so the DB column contains
 * human-readable values ("SUCCESS", "FAILED") rather than integer ordinals.
 * Ordinal-based storage is fragile — inserting a value in the middle of
 * the enum silently corrupts all existing rows.
 *
 * States:
 *  PENDING  → Generation request received, provider call in-flight.
 *             Useful if we later move to async (queue-based) generation.
 *  SUCCESS  → Provider returned valid image bytes; stored in DB.
 *  FAILED   → Provider call failed (timeout, model error, etc.).
 *             The row is still saved so the user can see failed attempts.
 */
public enum ImageStatus {

    /** Generation is queued or in-flight. */
    PENDING,

    /** Image generated and saved successfully. */
    SUCCESS,

    /** Generation failed — imageData will be null; failureReason will be set. */
    FAILED
}
