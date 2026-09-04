package com.chatweb.auth.controller;

import com.chatweb.auth.dto.LoginRequest;
import com.chatweb.auth.dto.RefreshTokenRequest;
import com.chatweb.auth.dto.RegisterRequest;
import com.chatweb.auth.repository.RefreshTokenRepository;
import com.chatweb.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private com.chatweb.conversation.repository.ConversationRepository conversationRepository;

    @Autowired
    private com.chatweb.conversation.repository.ConversationMemberRepository memberRepository;

    @Autowired
    private com.chatweb.message.repository.MessageRepository messageRepository;

    @BeforeEach
    void setUp() {
        messageRepository.deleteAll();
        memberRepository.deleteAll();
        conversationRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Should successfully register a new user")
    void register_Success() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("alice@example.com")
                .username("alice")
                .password("password123")
                .displayName("Alice Wonderland")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is(200)))
                .andExpect(jsonPath("$.data.accessToken", notNullValue()))
                .andExpect(jsonPath("$.data.refreshToken", notNullValue()))
                .andExpect(jsonPath("$.data.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.data.user.email", is("alice@example.com")))
                .andExpect(jsonPath("$.data.user.username", is("alice")))
                .andExpect(jsonPath("$.data.user.displayName", is("Alice Wonderland")));
    }

    @Test
    @DisplayName("Should return 409 Conflict when registering with duplicate email")
    void register_DuplicateEmail() throws Exception {
        RegisterRequest request1 = RegisterRequest.builder()
                .email("duplicate@example.com")
                .username("user1")
                .password("password123")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        RegisterRequest request2 = RegisterRequest.builder()
                .email("duplicate@example.com")
                .username("user2")
                .password("password123")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", is("EMAIL_ALREADY_EXISTS")));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when validation fails")
    void register_ValidationFailure() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("invalid-email")
                .username("a") // too short
                .password("123") // too short
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.fieldErrors.email", notNullValue()))
                .andExpect(jsonPath("$.fieldErrors.username", notNullValue()))
                .andExpect(jsonPath("$.fieldErrors.password", notNullValue()));
    }

    @Test
    @DisplayName("Should login successfully with email or username")
    void login_Success() throws Exception {
        // Register user first
        RegisterRequest registerReq = RegisterRequest.builder()
                .email("bob@example.com")
                .username("bobby")
                .password("securePass123")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated());

        // Login with email
        LoginRequest loginEmailReq = LoginRequest.builder()
                .emailOrUsername("bob@example.com")
                .password("securePass123")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginEmailReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken", notNullValue()))
                .andExpect(jsonPath("$.data.user.username", is("bobby")));

        // Login with username
        LoginRequest loginUserReq = LoginRequest.builder()
                .emailOrUsername("bobby")
                .password("securePass123")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginUserReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken", notNullValue()))
                .andExpect(jsonPath("$.data.user.email", is("bob@example.com")));
    }

    @Test
    @DisplayName("Should return 401 Unauthorized when password is wrong")
    void login_WrongPassword() throws Exception {
        RegisterRequest registerReq = RegisterRequest.builder()
                .email("charlie@example.com")
                .username("charlie")
                .password("correctPassword")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated());

        LoginRequest loginReq = LoginRequest.builder()
                .emailOrUsername("charlie")
                .password("wrongPassword")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("INVALID_CREDENTIALS")));
    }

    @Test
    @DisplayName("Should protect /api/users/me and allow access only with valid JWT Bearer token")
    void accessProtectedEndpoint_WithAndWithoutToken() throws Exception {
        // Without token -> 401 Unauthorized
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("UNAUTHORIZED")));

        // Register user to obtain access token
        RegisterRequest registerReq = RegisterRequest.builder()
                .email("david@example.com")
                .username("david")
                .password("pass123456")
                .displayName("David Bowie")
                .build();

        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode responseNode = objectMapper.readTree(registerResult.getResponse().getContentAsString());
        String accessToken = responseNode.get("data").get("accessToken").asText();

        // With valid token -> 200 OK
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email", is("david@example.com")))
                .andExpect(jsonPath("$.data.username", is("david")))
                .andExpect(jsonPath("$.data.displayName", is("David Bowie")));
    }

    @Test
    @DisplayName("Should successfully refresh token with rotation and reject reused old token")
    void refreshToken_RotationAndReuseRejection() throws Exception {
        // Register user
        RegisterRequest registerReq = RegisterRequest.builder()
                .email("eve@example.com")
                .username("eve")
                .password("password123")
                .build();

        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode jsonNode = objectMapper.readTree(registerResult.getResponse().getContentAsString());
        String oldRefreshToken = jsonNode.get("data").get("refreshToken").asText();

        // 1. Refresh using initial token -> should succeed and return new tokens
        RefreshTokenRequest refreshReq = RefreshTokenRequest.builder()
                .refreshToken(oldRefreshToken)
                .build();

        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken", notNullValue()))
                .andExpect(jsonPath("$.data.refreshToken", notNullValue()))
                .andReturn();

        JsonNode refreshNode = objectMapper.readTree(refreshResult.getResponse().getContentAsString());
        String newRefreshToken = refreshNode.get("data").get("refreshToken").asText();

        // 2. Attempting to reuse old revoked token -> should return 401 Unauthorized
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("INVALID_TOKEN")));

        // 3. Using new refresh token -> should succeed
        RefreshTokenRequest newRefreshReq = RefreshTokenRequest.builder()
                .refreshToken(newRefreshToken)
                .build();

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newRefreshReq)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should successfully logout and revoke refresh token")
    void logout_Success() throws Exception {
        RegisterRequest registerReq = RegisterRequest.builder()
                .email("frank@example.com")
                .username("frank")
                .password("password123")
                .build();

        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode jsonNode = objectMapper.readTree(registerResult.getResponse().getContentAsString());
        String refreshToken = jsonNode.get("data").get("refreshToken").asText();

        // Logout
        RefreshTokenRequest logoutReq = RefreshTokenRequest.builder()
                .refreshToken(refreshToken)
                .build();

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutReq)))
                .andExpect(status().isOk());

        // Attempting to refresh with revoked token should fail with 401
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutReq)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("INVALID_TOKEN")));
    }
}
