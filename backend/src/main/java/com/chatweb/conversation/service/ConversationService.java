package com.chatweb.conversation.service;

import com.chatweb.conversation.dto.ConversationDetailResponse;
import com.chatweb.conversation.dto.ConversationSummaryResponse;

import java.util.List;
import java.util.UUID;

public interface ConversationService {

    /**
     * Gets existing direct conversation between two users or creates a new one.
     *
     * @param currentUserId the ID of current authenticated user
     * @param recipientId the ID of the other user
     * @return ConversationDetailResponse with conversation details and members
     */
    ConversationDetailResponse getOrCreateDirectConversation(UUID currentUserId, UUID recipientId);

    /**
     * Retrieves all conversations the current user is a member of, ordered by latest update.
     *
     * @param currentUserId the ID of current authenticated user
     * @return List of ConversationSummaryResponse for the conversation list UI
     */
    List<ConversationSummaryResponse> getUserConversations(UUID currentUserId);

    /**
     * Retrieves detailed information of a specific conversation.
     *
     * @param currentUserId the ID of current authenticated user
     * @param conversationId the ID of conversation
     * @return ConversationDetailResponse with members
     */
    ConversationDetailResponse getConversationById(UUID currentUserId, UUID conversationId);

    /**
     * Deletes a direct conversation or leaves/deletes a group conversation.
     *
     * @param currentUserId the ID of current authenticated user
     * @param conversationId the ID of conversation
     */
    void deleteConversation(UUID currentUserId, UUID conversationId);

    /**
     * Checks if a user is a member of a conversation.
     *
     * @param conversationId the ID of conversation
     * @param userId the ID of user
     * @return true if user is a member, false otherwise
     */
    boolean isUserMemberOfConversation(UUID conversationId, UUID userId);

    /**
     * Validates that the user is a member of the conversation, throwing an exception if not.
     *
     * @param conversationId the ID of conversation
     * @param userId the ID of user
     */
    void validateUserInConversation(UUID conversationId, UUID userId);

    /**
     * Updates the timestamp of a conversation (e.g. when a new message is sent).
     *
     * @param conversationId the ID of conversation
     */
    void updateConversationTimestamp(UUID conversationId);

    /**
     * Updates the last read message ID for a user in a conversation.
     *
     * @param conversationId the ID of conversation
     * @param userId the ID of user
     * @param messageId the ID of the last read message
     */
    void updateLastReadMessage(UUID conversationId, UUID userId, UUID messageId);
}
