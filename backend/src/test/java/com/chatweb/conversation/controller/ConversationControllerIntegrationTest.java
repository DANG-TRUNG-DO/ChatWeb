package com.chatweb.conversation.controller;

import com.chatweb.auth.dto.RegisterRequest;
import com.chatweb.auth.repository.RefreshTokenRepository;
import com.chatweb.conversation.dto.CreateDirectConversationRequest;
import com.chatweb.conversation.repository.ConversationMemberRepository;
import com.chatweb.conversation.repository.ConversationRepository;
import com.chatweb.message.repository.MessageRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
class ConversationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private ConversationMemberRepository memberRepository;

    @Autowired
    private MessageRepository messageRepository;

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
    @DisplayName("POST /api/conversations: Should create and return direct conversation")
    void createDirectConversation_Success() throws Exception {
        String tokenUser1 = registerAndGetToken("user1@example.com", "user1", "User One");
        registerAndGetToken("user2@example.com", "user2", "User Two");

        User user2 = userRepository.findByUsername("user2").orElseThrow();

        CreateDirectConversationRequest request = CreateDirectConversationRequest.builder()
                .recipientId(user2.getId())
                .build();

        mockMvc.perform(post("/api/conversations")
                        .header("Authorization", "Bearer " + tokenUser1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(200)))
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.type", is("DIRECT")))
                .andExpect(jsonPath("$.data.name", is("User Two")))
                .andExpect(jsonPath("$.data.members", hasSize(2)));
    }

    @Test
    @DisplayName("POST /api/conversations: Should return 400 when attempting self-chat")
    void createDirectConversation_SelfChat_Fails() throws Exception {
        String tokenUser1 = registerAndGetToken("user1@example.com", "user1", "User One");
        User user1 = userRepository.findByUsername("user1").orElseThrow();

        CreateDirectConversationRequest request = CreateDirectConversationRequest.builder()
                .recipientId(user1.getId())
                .build();

        mockMvc.perform(post("/api/conversations")
                        .header("Authorization", "Bearer " + tokenUser1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("INVALID_CONVERSATION")));
    }

    @Test
    @DisplayName("GET /api/conversations: Should list conversations for authenticated user")
    void getUserConversations_Success() throws Exception {
        String tokenUser1 = registerAndGetToken("user1@example.com", "user1", "User One");
        registerAndGetToken("user2@example.com", "user2", "User Two");
        User user2 = userRepository.findByUsername("user2").orElseThrow();

        CreateDirectConversationRequest request = CreateDirectConversationRequest.builder()
                .recipientId(user2.getId())
                .build();

        // Create direct conversation
        mockMvc.perform(post("/api/conversations")
                        .header("Authorization", "Bearer " + tokenUser1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Get user conversations list
        mockMvc.perform(get("/api/conversations")
                        .header("Authorization", "Bearer " + tokenUser1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(200)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name", is("User Two")))
                .andExpect(jsonPath("$.data[0].partner.username", is("user2")));
    }

    @Test
    @DisplayName("GET /api/conversations/{id}: Should return 403 Forbidden when user is not member")
    void getConversationById_Forbidden() throws Exception {
        String tokenUser1 = registerAndGetToken("user1@example.com", "user1", "User One");
        registerAndGetToken("user2@example.com", "user2", "User Two");
        String tokenUser3 = registerAndGetToken("user3@example.com", "user3", "User Three");

        User user2 = userRepository.findByUsername("user2").orElseThrow();

        CreateDirectConversationRequest request = CreateDirectConversationRequest.builder()
                .recipientId(user2.getId())
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/conversations")
                        .header("Authorization", "Bearer " + tokenUser1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode responseNode = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String conversationId = responseNode.get("data").get("id").asText();

        // User 3 tries to access conversation between User 1 and User 2
        mockMvc.perform(get("/api/conversations/" + conversationId)
                        .header("Authorization", "Bearer " + tokenUser3))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", is("FORBIDDEN")));
    }

    @Test
    @DisplayName("DELETE /api/conversations/{id}: Should delete conversation")
    void deleteConversation_Success() throws Exception {
        String tokenUser1 = registerAndGetToken("user1@example.com", "user1", "User One");
        registerAndGetToken("user2@example.com", "user2", "User Two");

        User user2 = userRepository.findByUsername("user2").orElseThrow();

        CreateDirectConversationRequest request = CreateDirectConversationRequest.builder()
                .recipientId(user2.getId())
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/conversations")
                        .header("Authorization", "Bearer " + tokenUser1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode responseNode = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String conversationId = responseNode.get("data").get("id").asText();

        // Delete conversation
        mockMvc.perform(delete("/api/conversations/" + conversationId)
                        .header("Authorization", "Bearer " + tokenUser1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(200)));

        // Verify conversation is gone (404 Not Found)
        mockMvc.perform(get("/api/conversations/" + conversationId)
                        .header("Authorization", "Bearer " + tokenUser1))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("RESOURCE_NOT_FOUND")));
    }
}
