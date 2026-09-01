package com.chatweb.message.service;

import com.chatweb.common.dto.CursorPageResponse;
import com.chatweb.message.dto.MarkAsReadRequest;
import com.chatweb.message.dto.MessageResponse;
import com.chatweb.message.dto.MessageSummaryResponse;
import com.chatweb.message.dto.SendMessageRequest;
import com.chatweb.message.dto.UpdateMessageRequest;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public interface MessageService {

    MessageResponse sendMessage(UUID currentUserId, UUID conversationId, SendMessageRequest request);

    CursorPageResponse<MessageResponse> getMessages(UUID currentUserId, UUID conversationId, String cursor, int limit);

    MessageResponse updateMessage(UUID currentUserId, UUID messageId, UpdateMessageRequest request);

    void deleteMessage(UUID currentUserId, UUID messageId);

    void markAsRead(UUID currentUserId, UUID conversationId, MarkAsReadRequest request);

    MessageSummaryResponse getLatestMessage(UUID conversationId);

    Map<UUID, MessageSummaryResponse> getLatestMessages(Collection<UUID> conversationIds);

    long getUnreadCount(UUID conversationId, UUID userId, UUID lastReadMessageId);
}
