package com.chatweb.message.controller;

import com.chatweb.auth.dto.RegisterRequest;
import com.chatweb.auth.repository.RefreshTokenRepository;
import com.chatweb.conversation.dto.CreateDirectConversationRequest;
import com.chatweb.conversation.repository.ConversationMemberRepository;
import com.chatweb.conversation.repository.ConversationRepository;
import com.chatweb.message.dto.SendMessageRequest;
import com.chatweb.message.dto.UpdateMessageRequest;
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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
class MessageControllerIntegrationTest {

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

    private String createDirectConversation(String token, User recipient) throws Exception {
        CreateDirectConversationRequest request = CreateDirectConversationRequest.builder()
                .recipientId(recipient.getId())
                .build();

        MvcResult result = mockMvc.perform(post("/api/conversations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode responseNode = objectMapper.readTree(result.getResponse().getContentAsString());
        return responseNode.get("data").get("id").asText();
    }

    @Test
    @DisplayName("POST /api/conversations/{id}/messages: Should send message successfully")
    void sendMessage_Success() throws Exception {
        String tokenUser1 = registerAndGetToken("user1@example.com", "user1", "User One");
        registerAndGetToken("user2@example.com", "user2", "User Two");
        User user2 = userRepository.findByUsername("user2").orElseThrow();

        String convId = createDirectConversation(tokenUser1, user2);

        SendMessageRequest sendReq = SendMessageRequest.builder()
                .content("Hello User Two!")
                .build();

        mockMvc.perform(post("/api/conversations/" + convId + "/messages")
                        .header("Authorization", "Bearer " + tokenUser1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sendReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is(200)))
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.content", is("Hello User Two!")))
                .andExpect(jsonPath("$.data.sender.username", is("user1")))
                .andExpect(jsonPath("$.data.edited", is(false)))
                .andExpect(jsonPath("$.data.deleted", is(false)));
    }

    @Test
    @DisplayName("GET /api/conversations/{id}/messages: Should return messages with cursor pagination")
    void getMessages_CursorPagination_Success() throws Exception {
        String tokenUser1 = registerAndGetToken("user1@example.com", "user1", "User One");
        registerAndGetToken("user2@example.com", "user2", "User Two");
        User user2 = userRepository.findByUsername("user2").orElseThrow();

        String convId = createDirectConversation(tokenUser1, user2);

        // Send 3 messages
        for (int i = 1; i <= 3; i++) {
            SendMessageRequest sendReq = SendMessageRequest.builder()
                    .content("Message #" + i)
                    .build();
            mockMvc.perform(post("/api/conversations/" + convId + "/messages")
                            .header("Authorization", "Bearer " + tokenUser1)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(sendReq)))
                    .andExpect(status().isCreated());
            Thread.sleep(10);
        }

        // Fetch first page with limit=2
        MvcResult firstPageResult = mockMvc.perform(get("/api/conversations/" + convId + "/messages")
                        .header("Authorization", "Bearer " + tokenUser1)
                        .param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(200)))
                .andExpect(jsonPath("$.data.content", hasSize(2)))
                .andExpect(jsonPath("$.data.hasMore", is(true)))
                .andExpect(jsonPath("$.data.nextCursor", notNullValue()))
                .andReturn();

        JsonNode firstPageNode = objectMapper.readTree(firstPageResult.getResponse().getContentAsString());
        String cursor = firstPageNode.get("data").get("nextCursor").asText();

        // Fetch second page using cursor
        mockMvc.perform(get("/api/conversations/" + convId + "/messages")
                        .header("Authorization", "Bearer " + tokenUser1)
                        .param("cursor", cursor)
                        .param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(200)))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.hasMore", is(false)));
    }

    @Test
    @DisplayName("PUT /api/messages/{id}: Should edit message when user is sender")
    void updateMessage_Success() throws Exception {
        String tokenUser1 = registerAndGetToken("user1@example.com", "user1", "User One");
        registerAndGetToken("user2@example.com", "user2", "User Two");
        User user2 = userRepository.findByUsername("user2").orElseThrow();

        String convId = createDirectConversation(tokenUser1, user2);

        SendMessageRequest sendReq = SendMessageRequest.builder()
                .content("Original text")
                .build();

        MvcResult sendResult = mockMvc.perform(post("/api/conversations/" + convId + "/messages")
                        .header("Authorization", "Bearer " + tokenUser1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sendReq)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode sendNode = objectMapper.readTree(sendResult.getResponse().getContentAsString());
        String messageId = sendNode.get("data").get("id").asText();

        // Edit message
        UpdateMessageRequest updateReq = UpdateMessageRequest.builder()
                .content("Updated content")
                .build();

        mockMvc.perform(put("/api/messages/" + messageId)
                        .header("Authorization", "Bearer " + tokenUser1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(200)))
                .andExpect(jsonPath("$.data.content", is("Updated content")))
                .andExpect(jsonPath("$.data.edited", is(true)));
    }

    @Test
    @DisplayName("PUT /api/messages/{id}: Should return 403 when non-sender attempts to edit")
    void updateMessage_Forbidden() throws Exception {
        String tokenUser1 = registerAndGetToken("user1@example.com", "user1", "User One");
        String tokenUser2 = registerAndGetToken("user2@example.com", "user2", "User Two");
        User user2 = userRepository.findByUsername("user2").orElseThrow();

        String convId = createDirectConversation(tokenUser1, user2);

        SendMessageRequest sendReq = SendMessageRequest.builder()
                .content("Sent by User 1")
                .build();

        MvcResult sendResult = mockMvc.perform(post("/api/conversations/" + convId + "/messages")
                        .header("Authorization", "Bearer " + tokenUser1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sendReq)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode sendNode = objectMapper.readTree(sendResult.getResponse().getContentAsString());
        String messageId = sendNode.get("data").get("id").asText();

        // User 2 tries to edit User 1's message
        UpdateMessageRequest updateReq = UpdateMessageRequest.builder()
                .content("Hacked content")
                .build();

        mockMvc.perform(put("/api/messages/" + messageId)
                        .header("Authorization", "Bearer " + tokenUser2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", is("FORBIDDEN")));
    }

    @Test
    @DisplayName("DELETE /api/messages/{id}: Should soft delete message")
    void deleteMessage_Success() throws Exception {
        String tokenUser1 = registerAndGetToken("user1@example.com", "user1", "User One");
        registerAndGetToken("user2@example.com", "user2", "User Two");
        User user2 = userRepository.findByUsername("user2").orElseThrow();

        String convId = createDirectConversation(tokenUser1, user2);

        SendMessageRequest sendReq = SendMessageRequest.builder()
                .content("To be deleted")
                .build();

        MvcResult sendResult = mockMvc.perform(post("/api/conversations/" + convId + "/messages")
                        .header("Authorization", "Bearer " + tokenUser1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sendReq)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode sendNode = objectMapper.readTree(sendResult.getResponse().getContentAsString());
        String messageId = sendNode.get("data").get("id").asText();

        // Delete message
        mockMvc.perform(delete("/api/messages/" + messageId)
                        .header("Authorization", "Bearer " + tokenUser1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(200)));

        // Get messages to verify it's masked as deleted
        mockMvc.perform(get("/api/conversations/" + convId + "/messages")
                        .header("Authorization", "Bearer " + tokenUser1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].deleted", is(true)))
                .andExpect(jsonPath("$.data.content[0].content", is("Tin nhắn đã bị thu hồi")));
    }
}
