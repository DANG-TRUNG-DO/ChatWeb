package com.chatweb.user.controller;

import com.chatweb.auth.dto.RegisterRequest;
import com.chatweb.auth.repository.RefreshTokenRepository;
import com.chatweb.user.dto.UpdateProfileRequest;
import com.chatweb.user.entity.User;
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

import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
class UserControllerIntegrationTest {

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

    private String registerAndGetToken(String email, String username, String displayName) throws Exception {
        RegisterRequest registerReq = RegisterRequest.builder()
                .email(email)
                .username(username)
                .password("password123")
                .displayName(displayName)
                .build();

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode responseNode = objectMapper.readTree(result.getResponse().getContentAsString());
        return responseNode.get("data").get("accessToken").asText();
    }

    @Test
    @DisplayName("GET /api/users/me: Should return current user profile when authenticated")
    void getCurrentUser_Success() throws Exception {
        String token = registerAndGetToken("alice@example.com", "alice", "Alice Wonderland");

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(200)))
                .andExpect(jsonPath("$.data.email", is("alice@example.com")))
                .andExpect(jsonPath("$.data.username", is("alice")))
                .andExpect(jsonPath("$.data.displayName", is("Alice Wonderland")))
                .andExpect(jsonPath("$.data.createdAt", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/users/me: Should return 401 Unauthorized when token is missing")
    void getCurrentUser_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("UNAUTHORIZED")));
    }

    @Test
    @DisplayName("PUT /api/users/me: Should update profile successfully")
    void updateProfile_Success() throws Exception {
        String token = registerAndGetToken("bob@example.com", "bobby", "Bob");

        UpdateProfileRequest updateReq = UpdateProfileRequest.builder()
                .displayName("Robert The Great")
                .avatarUrl("https://example.com/robert.jpg")
                .build();

        mockMvc.perform(put("/api/users/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(200)))
                .andExpect(jsonPath("$.data.displayName", is("Robert The Great")))
                .andExpect(jsonPath("$.data.avatarUrl", is("https://example.com/robert.jpg")));
    }

    @Test
    @DisplayName("PUT /api/users/me: Should return 400 Bad Request when validation fails")
    void updateProfile_ValidationFailure() throws Exception {
        String token = registerAndGetToken("charlie@example.com", "charlie", "Charlie");

        UpdateProfileRequest updateReq = UpdateProfileRequest.builder()
                .displayName("A".repeat(101)) // Exceeds max 100 characters
                .build();

        mockMvc.perform(put("/api/users/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.fieldErrors.displayName", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/users/{id}: Should return public user summary by ID")
    void getUserById_Success() throws Exception {
        String token = registerAndGetToken("viewer@example.com", "viewer", "Viewer");
        registerAndGetToken("target@example.com", "target_user", "Target User");

        User targetUser = userRepository.findByUsername("target_user").orElseThrow();

        mockMvc.perform(get("/api/users/" + targetUser.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(200)))
                .andExpect(jsonPath("$.data.id", is(targetUser.getId().toString())))
                .andExpect(jsonPath("$.data.username", is("target_user")))
                .andExpect(jsonPath("$.data.displayName", is("Target User")))
                // Ensure sensitive email is NOT exposed in public summary
                .andExpect(jsonPath("$.data.email").doesNotExist());
    }

    @Test
    @DisplayName("GET /api/users/{id}: Should return 404 Not Found when user does not exist")
    void getUserById_NotFound() throws Exception {
        String token = registerAndGetToken("searcher@example.com", "searcher", "Searcher");

        mockMvc.perform(get("/api/users/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("RESOURCE_NOT_FOUND")));
    }

    @Test
    @DisplayName("GET /api/users/search: Should return paginated search results excluding current user")
    void searchUsers_Success() throws Exception {
        String token = registerAndGetToken("current@example.com", "developer", "Senior Dev");
        registerAndGetToken("dev1@example.com", "developer_one", "Dev One");
        registerAndGetToken("dev2@example.com", "developer_two", "Dev Two");
        registerAndGetToken("designer@example.com", "ui_designer", "UI Designer");

        // Search for "developer" -> should return developer_one and developer_two, but EXCLUDE current user ("developer")
        mockMvc.perform(get("/api/users/search")
                        .header("Authorization", "Bearer " + token)
                        .param("q", "developer")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(200)))
                .andExpect(jsonPath("$.data.totalElements", is(2)))
                .andExpect(jsonPath("$.data.content", hasSize(2)))
                .andExpect(jsonPath("$.data.content[0].username", is("developer_one")))
                .andExpect(jsonPath("$.data.content[1].username", is("developer_two")));
    }

    @Test
    @DisplayName("GET /api/users/search: Should return 401 Unauthorized when token is missing")
    void searchUsers_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/users/search")
                        .param("q", "test"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("UNAUTHORIZED")));
    }
}
