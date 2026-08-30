package com.chatweb.auth.service;

import com.chatweb.auth.dto.AuthResponse;
import com.chatweb.auth.dto.LoginRequest;
import com.chatweb.auth.dto.RefreshTokenRequest;
import com.chatweb.auth.dto.RegisterRequest;
import com.chatweb.auth.entity.RefreshToken;
import com.chatweb.auth.exception.EmailAlreadyExistsException;
import com.chatweb.auth.exception.InvalidCredentialsException;
import com.chatweb.auth.exception.InvalidTokenException;
import com.chatweb.auth.exception.TokenExpiredException;
import com.chatweb.auth.exception.UsernameAlreadyExistsException;
import com.chatweb.auth.repository.RefreshTokenRepository;
import com.chatweb.common.exception.ResourceNotFoundException;
import com.chatweb.user.dto.UserResponse;
import com.chatweb.user.entity.User;
import com.chatweb.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        String username = request.getUsername().trim();

        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(email);
        }

        if (userRepository.existsByUsername(username)) {
            throw new UsernameAlreadyExistsException(username);
        }

        String displayName = (request.getDisplayName() != null && !request.getDisplayName().isBlank())
                ? request.getDisplayName().trim()
                : username;

        User user = User.builder()
                .email(email)
                .username(username)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .displayName(displayName)
                .build();

        User savedUser = userRepository.save(user);
        log.info("Registered new user with id: {}, username: {}", savedUser.getId(), savedUser.getUsername());

        return createAuthResponse(savedUser);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        String identifier = request.getEmailOrUsername().trim();

        User user = userRepository.findByEmailOrUsername(identifier)
                .orElseThrow(() -> {
                    log.warn("Login failed: User not found with identifier '{}'", identifier);
                    return new InvalidCredentialsException();
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("Login failed: Password mismatch for user '{}'", user.getUsername());
            throw new InvalidCredentialsException();
        }

        log.info("User '{}' logged in successfully", user.getUsername());
        return createAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String rawToken = request.getRefreshToken().trim();

        RefreshToken refreshToken = refreshTokenRepository.findByToken(rawToken)
                .orElseThrow(() -> new InvalidTokenException("Refresh token not found"));

        if (refreshToken.isRevoked()) {
            log.warn("Attempt to use revoked refresh token for user id: {}", refreshToken.getUserId());
            throw new InvalidTokenException("Refresh token has been revoked");
        }

        if (refreshToken.isExpired()) {
            log.warn("Attempt to use expired refresh token for user id: {}", refreshToken.getUserId());
            throw new TokenExpiredException("Refresh token has expired");
        }

        User user = userRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", refreshToken.getUserId()));

        // Refresh token rotation: Revoke old token
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        // Generate and return new token pair
        return createAuthResponse(user);
    }

    @Override
    @Transactional
    public void logout(RefreshTokenRequest request) {
        if (request != null && request.getRefreshToken() != null) {
            String rawToken = request.getRefreshToken().trim();
            refreshTokenRepository.findByToken(rawToken).ifPresent(token -> {
                token.setRevoked(true);
                refreshTokenRepository.save(token);
                log.info("Revoked refresh token for user id: {}", token.getUserId());
            });
        }
        SecurityContextHolder.clearContext();
    }

    private AuthResponse createAuthResponse(User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken();

        Instant expiresAt = Instant.now().plusMillis(jwtTokenProvider.getRefreshTokenExpiration());

        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .userId(user.getId())
                .token(refreshToken)
                .expiresAt(expiresAt)
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshTokenEntity);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenExpiration())
                .user(UserResponse.fromEntity(user))
                .build();
    }
}
