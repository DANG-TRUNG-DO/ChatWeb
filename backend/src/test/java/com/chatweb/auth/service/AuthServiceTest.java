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
import com.chatweb.user.entity.User;
import com.chatweb.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthServiceImpl authService;

    private User testUser;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testUser = User.builder()
                .id(testUserId)
                .email("test@example.com")
                .username("testuser")
                .passwordHash("hashed_password")
                .displayName("Test User")
                .build();
    }

    @Test
    @DisplayName("Register: Should successfully register a new user")
    void register_Success() {
        RegisterRequest request = RegisterRequest.builder()
                .email("test@example.com")
                .username("testuser")
                .password("password123")
                .displayName("Test User")
                .build();

        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed_password");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtTokenProvider.generateAccessToken(testUser)).thenReturn("access.jwt.token");
        when(jwtTokenProvider.generateRefreshToken()).thenReturn("random-refresh-token");
        when(jwtTokenProvider.getAccessTokenExpiration()).thenReturn(900000L);
        when(jwtTokenProvider.getRefreshTokenExpiration()).thenReturn(604800000L);

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("access.jwt.token", response.getAccessToken());
        assertEquals("random-refresh-token", response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals("testuser", response.getUser().getUsername());
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Register: Should throw EmailAlreadyExistsException when email is taken")
    void register_EmailAlreadyExists() {
        RegisterRequest request = RegisterRequest.builder()
                .email("test@example.com")
                .username("testuser")
                .password("password123")
                .build();

        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class, () -> authService.register(request));
    }

    @Test
    @DisplayName("Register: Should throw UsernameAlreadyExistsException when username is taken")
    void register_UsernameAlreadyExists() {
        RegisterRequest request = RegisterRequest.builder()
                .email("test@example.com")
                .username("testuser")
                .password("password123")
                .build();

        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        assertThrows(UsernameAlreadyExistsException.class, () -> authService.register(request));
    }

    @Test
    @DisplayName("Login: Should successfully login with valid credentials")
    void login_Success() {
        LoginRequest request = LoginRequest.builder()
                .emailOrUsername("testuser")
                .password("password123")
                .build();

        when(userRepository.findByEmailOrUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "hashed_password")).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(testUser)).thenReturn("access.jwt.token");
        when(jwtTokenProvider.generateRefreshToken()).thenReturn("random-refresh-token");
        when(jwtTokenProvider.getAccessTokenExpiration()).thenReturn(900000L);

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("access.jwt.token", response.getAccessToken());
        assertEquals("random-refresh-token", response.getRefreshToken());
        assertEquals("test@example.com", response.getUser().getEmail());
    }

    @Test
    @DisplayName("Login: Should throw InvalidCredentialsException when user not found")
    void login_UserNotFound() {
        LoginRequest request = LoginRequest.builder()
                .emailOrUsername("unknown")
                .password("password123")
                .build();

        when(userRepository.findByEmailOrUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request));
    }

    @Test
    @DisplayName("Login: Should throw InvalidCredentialsException when password does not match")
    void login_WrongPassword() {
        LoginRequest request = LoginRequest.builder()
                .emailOrUsername("testuser")
                .password("wrongpassword")
                .build();

        when(userRepository.findByEmailOrUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongpassword", "hashed_password")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request));
    }

    @Test
    @DisplayName("RefreshToken: Should successfully rotate tokens when valid")
    void refreshToken_Success() {
        RefreshToken token = RefreshToken.builder()
                .id(UUID.randomUUID())
                .userId(testUserId)
                .token("valid-refresh-token")
                .expiresAt(Instant.now().plusSeconds(3600))
                .revoked(false)
                .build();

        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("valid-refresh-token")
                .build();

        when(refreshTokenRepository.findByToken("valid-refresh-token")).thenReturn(Optional.of(token));
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(jwtTokenProvider.generateAccessToken(testUser)).thenReturn("new.access.token");
        when(jwtTokenProvider.generateRefreshToken()).thenReturn("new-refresh-token");

        AuthResponse response = authService.refreshToken(request);

        assertNotNull(response);
        assertEquals("new.access.token", response.getAccessToken());
        assertEquals("new-refresh-token", response.getRefreshToken());
        assertTrue(token.isRevoked());
    }

    @Test
    @DisplayName("RefreshToken: Should throw InvalidTokenException when revoked")
    void refreshToken_Revoked() {
        RefreshToken token = RefreshToken.builder()
                .id(UUID.randomUUID())
                .userId(testUserId)
                .token("revoked-token")
                .expiresAt(Instant.now().plusSeconds(3600))
                .revoked(true)
                .build();

        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("revoked-token")
                .build();

        when(refreshTokenRepository.findByToken("revoked-token")).thenReturn(Optional.of(token));

        assertThrows(InvalidTokenException.class, () -> authService.refreshToken(request));
    }

    @Test
    @DisplayName("RefreshToken: Should throw TokenExpiredException when expired")
    void refreshToken_Expired() {
        RefreshToken token = RefreshToken.builder()
                .id(UUID.randomUUID())
                .userId(testUserId)
                .token("expired-token")
                .expiresAt(Instant.now().minusSeconds(100))
                .revoked(false)
                .build();

        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("expired-token")
                .build();

        when(refreshTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(token));

        assertThrows(TokenExpiredException.class, () -> authService.refreshToken(request));
    }

    @Test
    @DisplayName("Logout: Should mark token revoked")
    void logout_Success() {
        RefreshToken token = RefreshToken.builder()
                .id(UUID.randomUUID())
                .userId(testUserId)
                .token("active-token")
                .expiresAt(Instant.now().plusSeconds(3600))
                .revoked(false)
                .build();

        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("active-token")
                .build();

        when(refreshTokenRepository.findByToken("active-token")).thenReturn(Optional.of(token));

        authService.logout(request);

        assertTrue(token.isRevoked());
        verify(refreshTokenRepository).save(token);
    }
}
