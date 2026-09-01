package com.chatweb.message.repository;

import com.chatweb.message.entity.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    Optional<Message> findFirstByConversationIdOrderByCreatedAtDesc(UUID conversationId);

    List<Message> findByConversationIdOrderByCreatedAtDesc(UUID conversationId, Pageable pageable);

    @Query("""
        SELECT m FROM Message m
        WHERE m.conversationId = :conversationId
          AND m.createdAt < :cursorCreatedAt
        ORDER BY m.createdAt DESC
    """)
    List<Message> findMessagesBeforeCursor(
        @Param("conversationId") UUID conversationId,
        @Param("cursorCreatedAt") Instant cursorCreatedAt,
        Pageable pageable
    );

    @Query("""
        SELECT COUNT(m) FROM Message m
        WHERE m.conversationId = :conversationId
          AND m.createdAt > :lastReadCreatedAt
          AND m.senderId <> :userId
    """)
    long countUnreadMessagesAfter(
        @Param("conversationId") UUID conversationId,
        @Param("userId") UUID userId,
        @Param("lastReadCreatedAt") Instant lastReadCreatedAt
    );

    @Query("""
        SELECT COUNT(m) FROM Message m
        WHERE m.conversationId = :conversationId
          AND m.senderId <> :userId
    """)
    long countUnreadMessages(
        @Param("conversationId") UUID conversationId,
        @Param("userId") UUID userId
    );

    @Query("""
        SELECT m FROM Message m
        WHERE m.conversationId IN :conversationIds
          AND m.createdAt = (
              SELECT MAX(m2.createdAt) FROM Message m2
              WHERE m2.conversationId = m.conversationId
          )
    """)
    List<Message> findLatestMessagesByConversationIds(@Param("conversationIds") Collection<UUID> conversationIds);
}
