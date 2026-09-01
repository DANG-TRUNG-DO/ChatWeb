package com.chatweb.message.service;

import com.chatweb.common.dto.CursorPageResponse;
import com.chatweb.common.exception.ResourceNotFoundException;
import com.chatweb.conversation.service.ConversationService;
import com.chatweb.message.dto.MarkAsReadRequest;
import com.chatweb.message.dto.MessageResponse;
import com.chatweb.message.dto.MessageSummaryResponse;
import com.chatweb.message.dto.SendMessageRequest;
import com.chatweb.message.dto.UpdateMessageRequest;
import com.chatweb.message.entity.Message;
import com.chatweb.message.entity.MessageType;
import com.chatweb.message.exception.InvalidMessageException;
import com.chatweb.message.exception.MessageAccessDeniedException;
import com.chatweb.message.repository.MessageRepository;
import com.chatweb.realtime.service.RealtimeService;
import com.chatweb.user.dto.UserSummaryResponse;
import com.chatweb.user.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;
    private final UserService userService;
    private final ConversationService conversationService;
    private final RealtimeService realtimeService;

    public MessageServiceImpl(
            MessageRepository messageRepository,
            UserService userService,
            @Lazy ConversationService conversationService,
            @Lazy RealtimeService realtimeService
    ) {
        this.messageRepository = messageRepository;
        this.userService = userService;
        this.conversationService = conversationService;
        this.realtimeService = realtimeService;
    }

    @Override
    @Transactional
    public MessageResponse sendMessage(UUID currentUserId, UUID conversationId, SendMessageRequest request) {
        conversationService.validateUserInConversation(conversationId, currentUserId);

        MessageSummaryResponse replyToSummary = null;
        if (request.getReplyToId() != null) {
            Message replyToMsg = messageRepository.findById(request.getReplyToId())
                    .orElseThrow(() -> new ResourceNotFoundException("Message", "replyToId", request.getReplyToId()));

            if (!replyToMsg.getConversationId().equals(conversationId)) {
                throw new InvalidMessageException("Replied message does not belong to this conversation");
            }

            UserSummaryResponse replySender = userService.getUserById(replyToMsg.getSenderId());
            String replySenderName = replySender != null
                    ? (replySender.getDisplayName() != null ? replySender.getDisplayName() : replySender.getUsername())
                    : null;
            replyToSummary = MessageSummaryResponse.fromEntity(replyToMsg, replySenderName);
        }

        Message message = Message.builder()
                .conversationId(conversationId)
                .senderId(currentUserId)
                .content(request.getContent().trim())
                .type(request.getType() != null ? request.getType() : MessageType.TEXT)
                .replyToId(request.getReplyToId())
                .edited(false)
                .deleted(false)
                .build();

        Message savedMessage = messageRepository.save(message);
        conversationService.updateConversationTimestamp(conversationId);
        log.info("Message {} sent by user {} in conversation {}", savedMessage.getId(), currentUserId, conversationId);

        UserSummaryResponse senderSummary = userService.getUserById(currentUserId);
        MessageResponse response = MessageResponse.fromEntity(savedMessage, senderSummary, replyToSummary);
        realtimeService.broadcastMessageSent(conversationId, response);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPageResponse<MessageResponse> getMessages(UUID currentUserId, UUID conversationId, String cursor, int limit) {
        conversationService.validateUserInConversation(conversationId, currentUserId);

        int pageSize = Math.min(Math.max(1, limit), 50);
        Pageable pageable = PageRequest.of(0, pageSize + 1);

        List<Message> messages;
        if (cursor == null || cursor.isBlank()) {
            messages = messageRepository.findByConversationIdOrderByCreatedAtDesc(conversationId, pageable);
        } else {
            Instant cursorInstant = parseCursor(cursor);
            messages = messageRepository.findMessagesBeforeCursor(conversationId, cursorInstant, pageable);
        }

        boolean hasMore = messages.size() > pageSize;
        List<Message> contentList = hasMore ? messages.subList(0, pageSize) : messages;
        String nextCursor = (hasMore && !contentList.isEmpty())
                ? contentList.get(contentList.size() - 1).getCreatedAt().toString()
                : null;

        if (contentList.isEmpty()) {
            return CursorPageResponse.of(Collections.emptyList(), null, false);
        }

        Set<UUID> senderIds = contentList.stream()
                .map(Message::getSenderId)
                .collect(Collectors.toSet());

        Set<UUID> replyToIds = contentList.stream()
                .map(Message::getReplyToId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<UUID, Message> replyMessages = replyToIds.isEmpty()
                ? Collections.emptyMap()
                : messageRepository.findAllById(replyToIds).stream()
                        .collect(Collectors.toMap(Message::getId, m -> m));

        Set<UUID> replySenderIds = replyMessages.values().stream()
                .map(Message::getSenderId)
                .collect(Collectors.toSet());

        Set<UUID> allUserIds = new HashSet<>(senderIds);
        allUserIds.addAll(replySenderIds);

        Map<UUID, UserSummaryResponse> userMap = userService.getUsersSummaryByIds(allUserIds);

        List<MessageResponse> responses = contentList.stream()
                .map(m -> {
                    UserSummaryResponse sender = userMap.get(m.getSenderId());
                    MessageSummaryResponse replyTo = null;
                    if (m.getReplyToId() != null && replyMessages.containsKey(m.getReplyToId())) {
                        Message replyMsg = replyMessages.get(m.getReplyToId());
                        UserSummaryResponse replySender = userMap.get(replyMsg.getSenderId());
                        String replySenderName = replySender != null
                                ? (replySender.getDisplayName() != null ? replySender.getDisplayName() : replySender.getUsername())
                                : null;
                        replyTo = MessageSummaryResponse.fromEntity(replyMsg, replySenderName);
                    }
                    return MessageResponse.fromEntity(m, sender, replyTo);
                })
                .toList();

        return CursorPageResponse.of(responses, nextCursor, hasMore);
    }

    @Override
    @Transactional
    public MessageResponse updateMessage(UUID currentUserId, UUID messageId, UpdateMessageRequest request) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message", "id", messageId));

        if (!message.getSenderId().equals(currentUserId)) {
            throw new MessageAccessDeniedException("You can only edit your own messages");
        }

        if (message.isDeleted()) {
            throw new InvalidMessageException("Cannot edit a deleted message");
        }

        message.setContent(request.getContent().trim());
        message.setEdited(true);
        Message updatedMessage = messageRepository.save(message);
        log.info("Message {} edited by user {}", messageId, currentUserId);

        UserSummaryResponse senderSummary = userService.getUserById(currentUserId);
        MessageSummaryResponse replyToSummary = null;
        if (updatedMessage.getReplyToId() != null) {
            messageRepository.findById(updatedMessage.getReplyToId()).ifPresent(replyMsg -> {
                UserSummaryResponse replySender = userService.getUserById(replyMsg.getSenderId());
                String replySenderName = replySender != null
                        ? (replySender.getDisplayName() != null ? replySender.getDisplayName() : replySender.getUsername())
                        : null;
            });
        }

        MessageResponse response = MessageResponse.fromEntity(updatedMessage, senderSummary, replyToSummary);
        realtimeService.broadcastMessageUpdated(updatedMessage.getConversationId(), response);
        return response;
    }

    @Override
    @Transactional
    public void deleteMessage(UUID currentUserId, UUID messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message", "id", messageId));

        if (!message.getSenderId().equals(currentUserId)) {
            throw new MessageAccessDeniedException("You can only delete your own messages");
        }

        message.setDeleted(true);
        message.setContent(null);
        messageRepository.save(message);
        log.info("Message {} soft-deleted by user {}", messageId, currentUserId);
        realtimeService.broadcastMessageDeleted(message.getConversationId(), messageId);
    }

    @Override
    @Transactional
    public void markAsRead(UUID currentUserId, UUID conversationId, MarkAsReadRequest request) {
        conversationService.validateUserInConversation(conversationId, currentUserId);

        UUID targetMessageId = null;
        if (request != null && request.getMessageId() != null) {
            Message message = messageRepository.findById(request.getMessageId())
                    .orElseThrow(() -> new ResourceNotFoundException("Message", "id", request.getMessageId()));

            if (!message.getConversationId().equals(conversationId)) {
                throw new InvalidMessageException("Message does not belong to this conversation");
            }
            targetMessageId = message.getId();
        } else {
            Optional<Message> latest = messageRepository.findFirstByConversationIdOrderByCreatedAtDesc(conversationId);
            if (latest.isPresent()) {
                targetMessageId = latest.get().getId();
            }
        }

        if (targetMessageId != null) {
            conversationService.updateLastReadMessage(conversationId, currentUserId, targetMessageId);
            realtimeService.broadcastMessageRead(conversationId, currentUserId, targetMessageId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public MessageSummaryResponse getLatestMessage(UUID conversationId) {
        return messageRepository.findFirstByConversationIdOrderByCreatedAtDesc(conversationId)
                .map(m -> {
                    var sender = userService.getUserById(m.getSenderId());
                    String senderName = sender != null
                            ? (sender.getDisplayName() != null ? sender.getDisplayName() : sender.getUsername())
                            : null;
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
                            String senderName = sender != null
                                    ? (sender.getDisplayName() != null ? sender.getDisplayName() : sender.getUsername())
                                    : null;
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

    private Instant parseCursor(String cursor) {
        try {
            return Instant.parse(cursor);
        } catch (DateTimeParseException e) {
            try {
                long epochMillis = Long.parseLong(cursor);
                return Instant.ofEpochMilli(epochMillis);
            } catch (NumberFormatException ex) {
                throw new InvalidMessageException("Invalid cursor format. Expected ISO-8601 timestamp or epoch milliseconds.");
            }
        }
    }
}
