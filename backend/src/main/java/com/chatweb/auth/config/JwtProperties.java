package com.chatweb.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    /**
     * Secret key for signing JWTs (must be at least 256 bits for HS256).
     */
    private String secret = "dev-secret-key-change-in-production-must-be-at-least-256-bits-long-for-hs256";

    /**
     * Access token expiration time in milliseconds (default: 15 minutes = 900,000 ms).
     */
    private long accessTokenExpiration = 900000;

    /**
     * Refresh token expiration time in milliseconds (default: 7 days = 604,800,000 ms).
     */
    private long refreshTokenExpiration = 604800000;
}
