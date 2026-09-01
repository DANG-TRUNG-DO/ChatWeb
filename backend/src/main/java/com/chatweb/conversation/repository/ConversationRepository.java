package com.chatweb.conversation.repository;

import com.chatweb.conversation.entity.Conversation;
import com.chatweb.conversation.entity.ConversationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    @Query("""
        SELECT c FROM Conversation c
        JOIN ConversationMember cm1 ON c.id = cm1.conversationId
        JOIN ConversationMember cm2 ON c.id = cm2.conversationId
        WHERE c.type = :type
          AND cm1.userId = :userId1
          AND cm2.userId = :userId2
    """)
    Optional<Conversation> findDirectConversationBetweenUsers(
        @Param("type") ConversationType type,
        @Param("userId1") UUID userId1,
        @Param("userId2") UUID userId2
    );

    @Query("""
        SELECT c FROM Conversation c
        JOIN ConversationMember cm ON c.id = cm.conversationId
        WHERE cm.userId = :userId
        ORDER BY c.updatedAt DESC
    """)
    List<Conversation> findByUserId(@Param("userId") UUID userId);

    @Query("""
        SELECT c FROM Conversation c
        JOIN ConversationMember cm ON c.id = cm.conversationId
        WHERE cm.userId = :userId
        ORDER BY c.updatedAt DESC
    """)
    Page<Conversation> findByUserId(@Param("userId") UUID userId, Pageable pageable);
}
