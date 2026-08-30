package com.chatweb.auth.service;

import com.chatweb.auth.config.JwtProperties;
import com.chatweb.auth.exception.InvalidTokenException;
import com.chatweb.auth.exception.TokenExpiredException;
import com.chatweb.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;
    private final SecureRandom secureRandom = new SecureRandom();
    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        byte[] keyBytes = this.jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException("JWT secret key must be at least 256 bits (32 bytes) long for HS256 algorithm");
        }
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Generates a signed JWT Access Token for the given user.
     */
    public String generateAccessToken(User user) {
        return generateAccessToken(user.getId(), user.getEmail(), user.getUsername());
    }

    /**
     * Generates a signed JWT Access Token with specific claims.
     */
    public String generateAccessToken(UUID userId, String email, String username) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(jwtProperties.getAccessTokenExpiration());

        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("username", username)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(this.secretKey)
                .compact();
    }

    /**
     * Generates a cryptographically secure random Refresh Token string (64 hex characters).
     */
    public String generateRefreshToken() {
        byte[] randomBytes = new byte[32];
        this.secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    /**
     * Extracts the User ID (UUID) from the JWT subject.
     */
    public UUID extractUserId(String token) {
        String subject = extractAllClaims(token).getSubject();
        return UUID.fromString(subject);
    }

    /**
     * Extracts the username claim from the JWT token.
     */
    public String extractUsername(String token) {
        return extractAllClaims(token).get("username", String.class);
    }

    /**
     * Extracts the email claim from the JWT token.
     */
    public String extractEmail(String token) {
        return extractAllClaims(token).get("email", String.class);
    }

    /**
     * Extracts the token expiration date.
     */
    public Date extractExpiration(String token) {
        return extractAllClaims(token).getExpiration();
    }

    /**
     * Validates the JWT token structure and signature.
     *
     * @param token JWT token string
     * @return true if valid
     * @throws TokenExpiredException if the token is expired
     * @throws InvalidTokenException if the token is malformed, has invalid signature, or is invalid
     */
    public boolean validateToken(String token) {
        try {
            extractAllClaims(token);
            return true;
        } catch (ExpiredJwtException ex) {
            log.warn("JWT token has expired: {}", ex.getMessage());
            throw new TokenExpiredException("JWT token has expired");
        } catch (SignatureException ex) {
            log.warn("Invalid JWT signature: {}", ex.getMessage());
            throw new InvalidTokenException("Invalid JWT signature");
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("Invalid JWT token: {}", ex.getMessage());
            throw new InvalidTokenException("Invalid JWT token");
        }
    }

    /**
     * Safely checks if a token is valid without throwing exceptions.
     */
    public boolean isTokenValid(String token) {
        try {
            extractAllClaims(token);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * Parses and returns all claims stored inside the JWT token.
     */
    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(this.secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getAccessTokenExpiration() {
        return jwtProperties.getAccessTokenExpiration();
    }

    public long getRefreshTokenExpiration() {
        return jwtProperties.getRefreshTokenExpiration();
    }
}
