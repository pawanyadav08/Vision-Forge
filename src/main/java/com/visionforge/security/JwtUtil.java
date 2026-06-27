package com.visionforge.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JwtUtil — All JWT operations: generation, validation, and claim extraction.
 *
 * Single Responsibility: only handles JWT — nothing else.
 * Reads secret key and expiry from application.yml via @Value.
 */
@Slf4j
@Component
public class JwtUtil {

    @Value("${application.security.jwt.secret-key}")
    private String secretKey;

    @Value("${application.security.jwt.expiration}")
    private long jwtExpiration; // milliseconds

    // ── TOKEN GENERATION ──────────────────────────────────────────────────

    /** Generate a token with no extra claims (standard use case). */
    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    /**
     * Generate a signed JWT with additional payload claims.
     *
     * @param extraClaims Extra data to embed (userId, role, plan)
     * @param userDetails The authenticated user
     * @return Signed JWT string: Header.Payload.Signature
     */
    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())          // "sub" = email
                .issuedAt(new Date())                        // "iat" = now
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration)) // "exp"
                .signWith(getSigningKey())                   // HMAC-SHA256
                .compact();
    }

    // ── TOKEN VALIDATION ──────────────────────────────────────────────────

    /**
     * Returns true if the token is valid for the given user:
     *   - Signature is correct
     *   - Token is not expired
     *   - Subject matches the user's email
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (JwtException e) {
            log.warn("JWT validation error: {}", e.getMessage());
            return false;
        }
    }

    // ── CLAIM EXTRACTION ──────────────────────────────────────────────────

    /** Extract the subject claim (email) from the token. */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /** Extract the expiration date from the token. */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Generic claim extractor.
     * @param claimsResolver A function like Claims::getSubject
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // ── PRIVATE HELPERS ───────────────────────────────────────────────────

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Parse and verify the token signature.
     * Throws a JwtException subtype on any failure:
     *   ExpiredJwtException, MalformedJwtException, SignatureException, etc.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Convert the plain-text secret key from application.yml into a
     * cryptographic SecretKey object for HMAC-SHA256 signing.
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = secretKey.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
