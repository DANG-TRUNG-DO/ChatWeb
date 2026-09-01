package com.chatweb.realtime.service;

import com.chatweb.message.dto.MessageResponse;
import com.chatweb.realtime.dto.MessageDeletePayload;
import com.chatweb.realtime.dto.MessageReadPayload;
import com.chatweb.realtime.dto.TypingPayload;
import com.chatweb.realtime.dto.WebSocketEvent;
import com.chatweb.realtime.dto.WebSocketEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RealtimeServiceImpl implements RealtimeService {

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void sendEventToConversation(UUID conversationId, WebSocketEvent<?> event) {
        String topic1 = "/topic/conversations/" + conversationId;
        String topic2 = "/topic/conversation/" + conversationId;

        log.debug("Broadcasting event {} to conversation {}", event.getType(), conversationId);
        messagingTemplate.convertAndSend(topic1, event);
        messagingTemplate.convertAndSend(topic2, event);
    }

    @Override
    public void sendEventToUser(String username, String destination, WebSocketEvent<?> event) {
        log.debug("Sending event {} to user {} at destination {}", event.getType(), username, destination);
        messagingTemplate.convertAndSendToUser(username, destination, event);
    }

    @Override
    public void broadcastMessageSent(UUID conversationId, MessageResponse message) {
        WebSocketEvent<MessageResponse> event = WebSocketEvent.of(WebSocketEventType.MESSAGE_SENT, message);
        sendEventToConversation(conversationId, event);
    }

    @Override
    public void broadcastMessageUpdated(UUID conversationId, MessageResponse message) {
        WebSocketEvent<MessageResponse> event = WebSocketEvent.of(WebSocketEventType.MESSAGE_UPDATED, message);
        sendEventToConversation(conversationId, event);
    }

    @Override
    public void broadcastMessageDeleted(UUID conversationId, UUID messageId) {
        MessageDeletePayload payload = MessageDeletePayload.builder()
                .conversationId(conversationId)
                .messageId(messageId)
                .build();
        WebSocketEvent<MessageDeletePayload> event = WebSocketEvent.of(WebSocketEventType.MESSAGE_DELETED, payload);
        sendEventToConversation(conversationId, event);
    }

    @Override
    public void broadcastMessageRead(UUID conversationId, UUID userId, UUID lastReadMessageId) {
        MessageReadPayload payload = MessageReadPayload.builder()
                .conversationId(conversationId)
                .userId(userId)
                .messageId(lastReadMessageId)
                .build();
        WebSocketEvent<MessageReadPayload> event = WebSocketEvent.of(WebSocketEventType.MESSAGE_READ, payload);
        sendEventToConversation(conversationId, event);
    }

    @Override
    public void broadcastTyping(UUID conversationId, TypingPayload typingPayload) {
        WebSocketEvent<TypingPayload> event = WebSocketEvent.of(WebSocketEventType.TYPING, typingPayload);
        sendEventToConversation(conversationId, event);
    }
}
