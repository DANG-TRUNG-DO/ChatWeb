package com.chatweb.auth.service;

import com.chatweb.auth.config.JwtProperties;
import com.chatweb.auth.exception.InvalidTokenException;
import com.chatweb.auth.exception.TokenExpiredException;
import com.chatweb.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private JwtProperties jwtProperties;

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties();
        jwtProperties.setSecret("test-secret-key-that-is-at-least-256-bits-long-for-testing-purposes");
        jwtProperties.setAccessTokenExpiration(60000); // 1 minute
        jwtProperties.setRefreshTokenExpiration(120000); // 2 minutes

        jwtTokenProvider = new JwtTokenProvider(jwtProperties);
        jwtTokenProvider.init();
    }

    @Test
    @DisplayName("Should generate valid access token and extract claims correctly")
    void shouldGenerateAndExtractClaims() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .email("test@example.com")
                .username("testuser")
                .build();

        String token = jwtTokenProvider.generateAccessToken(user);

        assertNotNull(token);
        assertTrue(jwtTokenProvider.isTokenValid(token));
        assertEquals(userId, jwtTokenProvider.extractUserId(token));
        assertEquals("testuser", jwtTokenProvider.extractUsername(token));
        assertEquals("test@example.com", jwtTokenProvider.extractEmail(token));
    }

    @Test
    @DisplayName("Should generate non-empty unique refresh tokens")
    void shouldGenerateRefreshToken() {
        String token1 = jwtTokenProvider.generateRefreshToken();
        String token2 = jwtTokenProvider.generateRefreshToken();

        assertNotNull(token1);
        assertNotNull(token2);
        assertFalse(token1.isBlank());
        assertFalse(token1.equals(token2));
    }

    @Test
    @DisplayName("Should throw TokenExpiredException when token is expired")
    void shouldThrowWhenTokenIsExpired() {
        jwtProperties.setAccessTokenExpiration(-1000); // already expired
        jwtTokenProvider.init();

        User user = User.builder()
                .id(UUID.randomUUID())
                .email("expired@example.com")
                .username("expireduser")
                .build();

        String token = jwtTokenProvider.generateAccessToken(user);

        assertThrows(TokenExpiredException.class, () -> jwtTokenProvider.validateToken(token));
        assertFalse(jwtTokenProvider.isTokenValid(token));
    }

    @Test
    @DisplayName("Should throw InvalidTokenException when token is tampered or invalid")
    void shouldThrowWhenTokenIsInvalid() {
        String invalidToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.invalidpayload.invalidsignature";

        assertThrows(InvalidTokenException.class, () -> jwtTokenProvider.validateToken(invalidToken));
        assertFalse(jwtTokenProvider.isTokenValid(invalidToken));
    }
}
