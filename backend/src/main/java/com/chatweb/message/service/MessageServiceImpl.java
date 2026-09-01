package com.chatweb.message.service;

import com.chatweb.common.dto.CursorPageResponse;
import com.chatweb.message.dto.MarkAsReadRequest;
import com.chatweb.message.dto.MessageResponse;
import com.chatweb.message.dto.MessageSummaryResponse;
import com.chatweb.message.dto.SendMessageRequest;
import com.chatweb.message.dto.UpdateMessageRequest;
import com.chatweb.message.entity.Message;
import com.chatweb.message.repository.MessageRepository;
import com.chatweb.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;
    private final UserService userService;

    @Override
    public MessageResponse sendMessage(UUID currentUserId, UUID conversationId, SendMessageRequest request) {
        return null;
    }

    @Override
    public CursorPageResponse<MessageResponse> getMessages(UUID currentUserId, UUID conversationId, String cursor, int limit) {
        return null;
    }

    @Override
    public MessageResponse updateMessage(UUID currentUserId, UUID messageId, UpdateMessageRequest request) {
        return null;
    }

    @Override
    public void deleteMessage(UUID currentUserId, UUID messageId) {
    }

    @Override
    public void markAsRead(UUID currentUserId, UUID conversationId, MarkAsReadRequest request) {
    }

    @Override
    @Transactional(readOnly = true)
    public MessageSummaryResponse getLatestMessage(UUID conversationId) {
        return messageRepository.findFirstByConversationIdOrderByCreatedAtDesc(conversationId)
                .map(m -> {
                    var sender = userService.getUserById(m.getSenderId());
                    String senderName = sender != null ? (sender.getDisplayName() != null ? sender.getDisplayName() : sender.getUsername()) : null;
                    return MessageSummaryResponse.fromEntity(m, senderName);
                })
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, MessageSummaryResponse> getLatestMessages(Collection<UUID> conversationIds) {
        if (conversationIds == null || conversationIds.isEmpty()) {
            return Collections.emptyMap();
        }

        var messages = messageRepository.findLatestMessagesByConversationIds(conversationIds);
        var senderIds = messages.stream().map(Message::getSenderId).collect(Collectors.toSet());
        var senders = userService.getUsersSummaryByIds(senderIds);

        return messages.stream()
                .collect(Collectors.toMap(
                        Message::getConversationId,
                        m -> {
                            var sender = senders.get(m.getSenderId());
                            String senderName = sender != null ? (sender.getDisplayName() != null ? sender.getDisplayName() : sender.getUsername()) : null;
                            return MessageSummaryResponse.fromEntity(m, senderName);
                        },
                        (m1, m2) -> m1
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(UUID conversationId, UUID userId, UUID lastReadMessageId) {
        if (lastReadMessageId == null) {
            return messageRepository.countUnreadMessages(conversationId, userId);
        }

        return messageRepository.findById(lastReadMessageId)
                .map(lastRead -> messageRepository.countUnreadMessagesAfter(conversationId, userId, lastRead.getCreatedAt()))
                .orElseGet(() -> messageRepository.countUnreadMessages(conversationId, userId));
    }
}
