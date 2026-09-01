package com.chatweb.realtime.service;

import com.chatweb.message.dto.MessageResponse;
import com.chatweb.realtime.dto.TypingPayload;
import com.chatweb.realtime.dto.WebSocketEvent;

import java.util.UUID;

public interface RealtimeService {

    void sendEventToConversation(UUID conversationId, WebSocketEvent<?> event);

    void sendEventToUser(String username, String destination, WebSocketEvent<?> event);

    void broadcastMessageSent(UUID conversationId, MessageResponse message);

    void broadcastMessageUpdated(UUID conversationId, MessageResponse message);

    void broadcastMessageDeleted(UUID conversationId, UUID messageId);

    void broadcastMessageRead(UUID conversationId, UUID userId, UUID lastReadMessageId);

    void broadcastTyping(UUID conversationId, TypingPayload typingPayload);
}
