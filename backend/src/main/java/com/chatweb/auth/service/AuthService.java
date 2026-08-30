package com.chatweb.auth.service;

import com.chatweb.auth.dto.AuthResponse;
import com.chatweb.auth.dto.LoginRequest;
import com.chatweb.auth.dto.RefreshTokenRequest;
import com.chatweb.auth.dto.RegisterRequest;

public interface AuthService {

    /**
     * Registers a new user with email, username, and password.
     *
     * @param request the registration details
     * @return AuthResponse containing tokens and user details
     */
    AuthResponse register(RegisterRequest request);

    /**
     * Authenticates a user by email/username and password.
     *
     * @param request the login credentials
     * @return AuthResponse containing tokens and user details
     */
    AuthResponse login(LoginRequest request);

    /**
     * Refreshes the access token using a valid, non-revoked refresh token.
     * Implements refresh token rotation.
     *
     * @param request the refresh token request
     * @return AuthResponse containing a new access token, new refresh token, and user details
     */
    AuthResponse refreshToken(RefreshTokenRequest request);

    /**
     * Revokes the provided refresh token and invalidates the session.
     *
     * @param request the refresh token to revoke
     */
    void logout(RefreshTokenRequest request);
}
