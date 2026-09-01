package com.chatweb.realtime.service;

import com.chatweb.message.dto.MessageResponse;
import com.chatweb.message.entity.MessageType;
import com.chatweb.realtime.dto.MessageDeletePayload;
import com.chatweb.realtime.dto.MessageReadPayload;
import com.chatweb.realtime.dto.TypingPayload;
import com.chatweb.realtime.dto.WebSocketEvent;
import com.chatweb.realtime.dto.WebSocketEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("RealtimeService Unit Tests")
class RealtimeServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private RealtimeServiceImpl realtimeService;

    private UUID conversationId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        conversationId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Should broadcast MESSAGE_SENT event to conversation topics")
    void broadcastMessageSent_Success() {
        MessageResponse message = MessageResponse.builder()
                .id(UUID.randomUUID())
                .conversationId(conversationId)
                .content("Hello World")
                .type(MessageType.TEXT)
                .build();

        realtimeService.broadcastMessageSent(conversationId, message);

        ArgumentCaptor<WebSocketEvent<?>> eventCaptor = ArgumentCaptor.forClass(WebSocketEvent.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/conversations/" + conversationId), eventCaptor.capture());
        verify(messagingTemplate).convertAndSend(eq("/topic/conversation/" + conversationId), eventCaptor.capture());

        WebSocketEvent<?> event = eventCaptor.getAllValues().get(0);
        assertThat(event.getType()).isEqualTo(WebSocketEventType.MESSAGE_SENT);
        assertThat(event.getPayload()).isEqualTo(message);
    }

    @Test
    @DisplayName("Should broadcast MESSAGE_UPDATED event to conversation topics")
    void broadcastMessageUpdated_Success() {
        MessageResponse message = MessageResponse.builder()
                .id(UUID.randomUUID())
                .conversationId(conversationId)
                .content("Edited message")
                .type(MessageType.TEXT)
                .edited(true)
                .build();

        realtimeService.broadcastMessageUpdated(conversationId, message);

        ArgumentCaptor<WebSocketEvent<?>> eventCaptor = ArgumentCaptor.forClass(WebSocketEvent.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/conversations/" + conversationId), eventCaptor.capture());

        WebSocketEvent<?> event = eventCaptor.getValue();
        assertThat(event.getType()).isEqualTo(WebSocketEventType.MESSAGE_UPDATED);
        assertThat(event.getPayload()).isEqualTo(message);
    }

    @Test
    @DisplayName("Should broadcast MESSAGE_DELETED event to conversation topics")
    void broadcastMessageDeleted_Success() {
        UUID messageId = UUID.randomUUID();

        realtimeService.broadcastMessageDeleted(conversationId, messageId);

        ArgumentCaptor<WebSocketEvent<?>> eventCaptor = ArgumentCaptor.forClass(WebSocketEvent.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/conversations/" + conversationId), eventCaptor.capture());

        WebSocketEvent<?> event = eventCaptor.getValue();
        assertThat(event.getType()).isEqualTo(WebSocketEventType.MESSAGE_DELETED);
        assertThat(event.getPayload()).isInstanceOf(MessageDeletePayload.class);

        MessageDeletePayload payload = (MessageDeletePayload) event.getPayload();
        assertThat(payload.getMessageId()).isEqualTo(messageId);
        assertThat(payload.getConversationId()).isEqualTo(conversationId);
    }

    @Test
    @DisplayName("Should broadcast MESSAGE_READ event to conversation topics")
    void broadcastMessageRead_Success() {
        UUID lastReadId = UUID.randomUUID();

        realtimeService.broadcastMessageRead(conversationId, userId, lastReadId);

        ArgumentCaptor<WebSocketEvent<?>> eventCaptor = ArgumentCaptor.forClass(WebSocketEvent.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/conversations/" + conversationId), eventCaptor.capture());

        WebSocketEvent<?> event = eventCaptor.getValue();
        assertThat(event.getType()).isEqualTo(WebSocketEventType.MESSAGE_READ);
        assertThat(event.getPayload()).isInstanceOf(MessageReadPayload.class);

        MessageReadPayload payload = (MessageReadPayload) event.getPayload();
        assertThat(payload.getConversationId()).isEqualTo(conversationId);
        assertThat(payload.getUserId()).isEqualTo(userId);
        assertThat(payload.getMessageId()).isEqualTo(lastReadId);
    }

    @Test
    @DisplayName("Should broadcast TYPING event to conversation topics")
    void broadcastTyping_Success() {
        TypingPayload typingPayload = TypingPayload.builder()
                .conversationId(conversationId)
                .userId(userId)
                .username("testuser")
                .typing(true)
                .build();

        realtimeService.broadcastTyping(conversationId, typingPayload);

        ArgumentCaptor<WebSocketEvent<?>> eventCaptor = ArgumentCaptor.forClass(WebSocketEvent.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/conversations/" + conversationId), eventCaptor.capture());

        WebSocketEvent<?> event = eventCaptor.getValue();
        assertThat(event.getType()).isEqualTo(WebSocketEventType.TYPING);
        assertThat(event.getPayload()).isEqualTo(typingPayload);
    }

    @Test
    @DisplayName("Should send event to specific user destination")
    void sendEventToUser_Success() {
        WebSocketEvent<String> event = WebSocketEvent.of(WebSocketEventType.MESSAGE_SENT, "test");

        realtimeService.sendEventToUser("john_doe", "/queue/messages", event);

        verify(messagingTemplate).convertAndSendToUser("john_doe", "/queue/messages", event);
    }
}
