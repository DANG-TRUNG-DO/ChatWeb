package com.chatweb.realtime.controller;

import com.chatweb.auth.security.UserPrincipal;
import com.chatweb.conversation.service.ConversationService;
import com.chatweb.message.dto.MarkAsReadRequest;
import com.chatweb.message.dto.MessageResponse;
import com.chatweb.message.dto.SendMessageRequest;
import com.chatweb.message.entity.MessageType;
import com.chatweb.message.service.MessageService;
import com.chatweb.realtime.dto.ChatMessagePayload;
import com.chatweb.realtime.dto.MessageReadPayload;
import com.chatweb.realtime.dto.TypingPayload;
import com.chatweb.realtime.service.RealtimeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.security.Principal;
import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatController Unit Tests")
class ChatControllerTest {

    @Mock
    private MessageService messageService;

    @Mock
    private ConversationService conversationService;

    @Mock
    private RealtimeService realtimeService;

    @InjectMocks
    private ChatController chatController;

    private UUID userId;
    private UUID conversationId;
    private UserPrincipal userPrincipal;
    private Principal principal;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        conversationId = UUID.randomUUID();

        userPrincipal = UserPrincipal.builder()
                .id(userId)
                .email("user@example.com")
                .username("testuser")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")))
                .build();

        principal = new UsernamePasswordAuthenticationToken(userPrincipal, null, userPrincipal.getAuthorities());
    }

    @Test
    @DisplayName("Should handle chat.send message correctly and delegate to messageService")
    void handleSendMessage_Success() {
        ChatMessagePayload payload = ChatMessagePayload.builder()
                .conversationId(conversationId)
                .content("Hello World")
                .type(MessageType.TEXT)
                .build();

        when(messageService.sendMessage(eq(userId), eq(conversationId), any(SendMessageRequest.class)))
                .thenReturn(MessageResponse.builder().id(UUID.randomUUID()).build());

        chatController.handleSendMessage(payload, principal);

        ArgumentCaptor<SendMessageRequest> requestCaptor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(messageService).sendMessage(eq(userId), eq(conversationId), requestCaptor.capture());

        SendMessageRequest captured = requestCaptor.getValue();
        assertThat(captured.getContent()).isEqualTo("Hello World");
        assertThat(captured.getType()).isEqualTo(MessageType.TEXT);
    }

    @Test
    @DisplayName("Should handle chat.read message correctly and delegate to messageService")
    void handleMarkAsRead_Success() {
        UUID messageId = UUID.randomUUID();
        MessageReadPayload payload = MessageReadPayload.builder()
                .conversationId(conversationId)
                .messageId(messageId)
                .build();

        chatController.handleMarkAsRead(payload, principal);

        ArgumentCaptor<MarkAsReadRequest> requestCaptor = ArgumentCaptor.forClass(MarkAsReadRequest.class);
        verify(messageService).markAsRead(eq(userId), eq(conversationId), requestCaptor.capture());

        MarkAsReadRequest captured = requestCaptor.getValue();
        assertThat(captured.getMessageId()).isEqualTo(messageId);
    }

    @Test
    @DisplayName("Should handle chat.typing message, validate membership, and broadcast typing event")
    void handleTyping_Success() {
        TypingPayload payload = TypingPayload.builder()
                .conversationId(conversationId)
                .typing(true)
                .build();

        chatController.handleTyping(payload, principal);

        verify(conversationService).validateUserInConversation(conversationId, userId);

        ArgumentCaptor<TypingPayload> payloadCaptor = ArgumentCaptor.forClass(TypingPayload.class);
        verify(realtimeService).broadcastTyping(eq(conversationId), payloadCaptor.capture());

        TypingPayload captured = payloadCaptor.getValue();
        assertThat(captured.getUserId()).isEqualTo(userId);
        assertThat(captured.getUsername()).isEqualTo("testuser");
        assertThat(captured.isTyping()).isTrue();
    }

    @Test
    @DisplayName("Should throw BadCredentialsException when principal is not authenticated")
    void handleSendMessage_Unauthenticated_ThrowsException() {
        ChatMessagePayload payload = ChatMessagePayload.builder()
                .conversationId(conversationId)
                .content("Hello")
                .build();

        assertThatThrownBy(() -> chatController.handleSendMessage(payload, null))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("User is not authenticated");
    }

    private static <T> T any(Class<T> type) {
        return org.mockito.ArgumentMatchers.any(type);
    }
}
