package com.chatweb.message.service;

import com.chatweb.common.dto.CursorPageResponse;
import com.chatweb.conversation.service.ConversationService;
import com.chatweb.message.dto.MessageResponse;
import com.chatweb.message.dto.SendMessageRequest;
import com.chatweb.message.dto.UpdateMessageRequest;
import com.chatweb.message.entity.Message;
import com.chatweb.message.entity.MessageType;
import com.chatweb.message.exception.InvalidMessageException;
import com.chatweb.message.exception.MessageAccessDeniedException;
import com.chatweb.message.repository.MessageRepository;
import com.chatweb.user.dto.UserSummaryResponse;
import com.chatweb.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private UserService userService;

    @Mock
    private ConversationService conversationService;

    @InjectMocks
    private MessageServiceImpl messageService;

    private UUID userId;
    private UUID otherUserId;
    private UUID conversationId;
    private UUID messageId;
    private UserSummaryResponse userSummary;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        otherUserId = UUID.randomUUID();
        conversationId = UUID.randomUUID();
        messageId = UUID.randomUUID();

        userSummary = UserSummaryResponse.builder()
                .id(userId)
                .username("testuser")
                .displayName("Test User")
                .build();
    }

    @Test
    @DisplayName("Should send message successfully")
    void sendMessage_Success() {
        doNothing().when(conversationService).validateUserInConversation(conversationId, userId);
        when(userService.getUserById(userId)).thenReturn(userSummary);

        Message savedMsg = Message.builder()
                .id(messageId)
                .conversationId(conversationId)
                .senderId(userId)
                .content("Hello world")
                .type(MessageType.TEXT)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(messageRepository.save(any(Message.class))).thenReturn(savedMsg);

        SendMessageRequest request = SendMessageRequest.builder()
                .content("Hello world")
                .build();

        MessageResponse response = messageService.sendMessage(userId, conversationId, request);

        assertNotNull(response);
        assertEquals(messageId, response.getId());
        assertEquals("Hello world", response.getContent());
        assertEquals("testuser", response.getSender().getUsername());
        assertFalse(response.isEdited());
        assertFalse(response.isDeleted());

        verify(conversationService).validateUserInConversation(conversationId, userId);
        verify(conversationService).updateConversationTimestamp(conversationId);
    }

    @Test
    @DisplayName("Should send message with replyTo reference")
    void sendMessage_WithReply_Success() {
        UUID replyId = UUID.randomUUID();
        doNothing().when(conversationService).validateUserInConversation(conversationId, userId);
        when(userService.getUserById(userId)).thenReturn(userSummary);

        UserSummaryResponse otherSummary = UserSummaryResponse.builder()
                .id(otherUserId)
                .username("otheruser")
                .displayName("Other User")
                .build();
        when(userService.getUserById(otherUserId)).thenReturn(otherSummary);

        Message replyTarget = Message.builder()
                .id(replyId)
                .conversationId(conversationId)
                .senderId(otherUserId)
                .content("Original message")
                .type(MessageType.TEXT)
                .createdAt(Instant.now())
                .build();

        when(messageRepository.findById(replyId)).thenReturn(Optional.of(replyTarget));

        Message savedMsg = Message.builder()
                .id(messageId)
                .conversationId(conversationId)
                .senderId(userId)
                .content("Reply message")
                .type(MessageType.TEXT)
                .replyToId(replyId)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(messageRepository.save(any(Message.class))).thenReturn(savedMsg);

        SendMessageRequest request = SendMessageRequest.builder()
                .content("Reply message")
                .replyToId(replyId)
                .build();

        MessageResponse response = messageService.sendMessage(userId, conversationId, request);

        assertNotNull(response);
        assertNotNull(response.getReplyTo());
        assertEquals(replyId, response.getReplyTo().getId());
        assertEquals("Original message", response.getReplyTo().getContent());
        assertEquals("Other User", response.getReplyTo().getSenderName());
    }

    @Test
    @DisplayName("Should throw InvalidMessageException when replying to a message from a different conversation")
    void sendMessage_ReplyDifferentConversation_ThrowsException() {
        UUID replyId = UUID.randomUUID();
        UUID otherConvId = UUID.randomUUID();
        doNothing().when(conversationService).validateUserInConversation(conversationId, userId);

        Message replyTarget = Message.builder()
                .id(replyId)
                .conversationId(otherConvId)
                .senderId(otherUserId)
                .content("From other conversation")
                .build();

        when(messageRepository.findById(replyId)).thenReturn(Optional.of(replyTarget));

        SendMessageRequest request = SendMessageRequest.builder()
                .content("Invalid reply")
                .replyToId(replyId)
                .build();

        assertThrows(InvalidMessageException.class, () ->
                messageService.sendMessage(userId, conversationId, request));
    }

    @Test
    @DisplayName("Should retrieve messages with cursor pagination")
    void getMessages_CursorPagination_Success() {
        doNothing().when(conversationService).validateUserInConversation(conversationId, userId);

        Instant time1 = Instant.now().minusSeconds(100);
        Instant time2 = Instant.now().minusSeconds(50);
        Instant time3 = Instant.now();

        Message m3 = Message.builder().id(UUID.randomUUID()).conversationId(conversationId).senderId(userId).content("M3").createdAt(time3).build();
        Message m2 = Message.builder().id(UUID.randomUUID()).conversationId(conversationId).senderId(userId).content("M2").createdAt(time2).build();
        Message m1 = Message.builder().id(UUID.randomUUID()).conversationId(conversationId).senderId(userId).content("M1").createdAt(time1).build();

        // Limit requested = 2, repository returns 3 items (limit + 1)
        when(messageRepository.findByConversationIdOrderByCreatedAtDesc(eq(conversationId), any(Pageable.class)))
                .thenReturn(List.of(m3, m2, m1));

        when(userService.getUsersSummaryByIds(any())).thenReturn(Map.of(userId, userSummary));

        CursorPageResponse<MessageResponse> response = messageService.getMessages(userId, conversationId, null, 2);

        assertNotNull(response);
        assertEquals(2, response.getContent().size());
        assertTrue(response.isHasMore());
        assertEquals(time2.toString(), response.getNextCursor());
        assertEquals("M3", response.getContent().get(0).getContent());
        assertEquals("M2", response.getContent().get(1).getContent());
    }

    @Test
    @DisplayName("Should update message content and set edited = true")
    void updateMessage_Success() {
        Message existing = Message.builder()
                .id(messageId)
                .conversationId(conversationId)
                .senderId(userId)
                .content("Original text")
                .edited(false)
                .build();

        when(messageRepository.findById(messageId)).thenReturn(Optional.of(existing));
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userService.getUserById(userId)).thenReturn(userSummary);

        UpdateMessageRequest request = UpdateMessageRequest.builder()
                .content("Edited text")
                .build();

        MessageResponse response = messageService.updateMessage(userId, messageId, request);

        assertNotNull(response);
        assertEquals("Edited text", response.getContent());
        assertTrue(response.isEdited());
    }

    @Test
    @DisplayName("Should throw MessageAccessDeniedException when non-sender attempts to edit")
    void updateMessage_NotSender_ThrowsAccessDenied() {
        Message existing = Message.builder()
                .id(messageId)
                .conversationId(conversationId)
                .senderId(otherUserId) // Not userId
                .content("Original text")
                .build();

        when(messageRepository.findById(messageId)).thenReturn(Optional.of(existing));

        UpdateMessageRequest request = UpdateMessageRequest.builder()
                .content("Edited text")
                .build();

        assertThrows(MessageAccessDeniedException.class, () ->
                messageService.updateMessage(userId, messageId, request));
    }

    @Test
    @DisplayName("Should soft delete message when requested by sender")
    void deleteMessage_Success() {
        Message existing = Message.builder()
                .id(messageId)
                .conversationId(conversationId)
                .senderId(userId)
                .content("Original text")
                .deleted(false)
                .build();

        when(messageRepository.findById(messageId)).thenReturn(Optional.of(existing));

        messageService.deleteMessage(userId, messageId);

        assertTrue(existing.isDeleted());
        assertNull(existing.getContent());
        verify(messageRepository).save(existing);
    }

    @Test
    @DisplayName("Should throw MessageAccessDeniedException when non-sender attempts to delete")
    void deleteMessage_NotSender_ThrowsAccessDenied() {
        Message existing = Message.builder()
                .id(messageId)
                .conversationId(conversationId)
                .senderId(otherUserId)
                .content("Original text")
                .build();

        when(messageRepository.findById(messageId)).thenReturn(Optional.of(existing));

        assertThrows(MessageAccessDeniedException.class, () ->
                messageService.deleteMessage(userId, messageId));
    }

    @Test
    @DisplayName("Should mark conversation as read up to latest message when request has null messageId")
    void markAsRead_LatestMessage_Success() {
        doNothing().when(conversationService).validateUserInConversation(conversationId, userId);

        Message latest = Message.builder()
                .id(messageId)
                .conversationId(conversationId)
                .senderId(otherUserId)
                .build();

        when(messageRepository.findFirstByConversationIdOrderByCreatedAtDesc(conversationId))
                .thenReturn(Optional.of(latest));

        messageService.markAsRead(userId, conversationId, null);

        verify(conversationService).updateLastReadMessage(conversationId, userId, messageId);
    }

    @Test
    @DisplayName("Should mark conversation as read up to specific message when request provides messageId")
    void markAsRead_SpecificMessage_Success() {
        doNothing().when(conversationService).validateUserInConversation(conversationId, userId);

        Message specific = Message.builder()
                .id(messageId)
                .conversationId(conversationId)
                .senderId(otherUserId)
                .build();

        when(messageRepository.findById(messageId)).thenReturn(Optional.of(specific));

        com.chatweb.message.dto.MarkAsReadRequest req = com.chatweb.message.dto.MarkAsReadRequest.builder()
                .messageId(messageId)
                .build();

        messageService.markAsRead(userId, conversationId, req);

        verify(conversationService).updateLastReadMessage(conversationId, userId, messageId);
    }
}
