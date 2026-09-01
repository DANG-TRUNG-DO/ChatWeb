package com.chatweb.conversation.repository;

import com.chatweb.conversation.entity.ConversationMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationMemberRepository extends JpaRepository<ConversationMember, UUID> {

    Optional<ConversationMember> findByConversationIdAndUserId(UUID conversationId, UUID userId);

    boolean existsByConversationIdAndUserId(UUID conversationId, UUID userId);

    List<ConversationMember> findByConversationId(UUID conversationId);

    List<ConversationMember> findByConversationIdIn(Collection<UUID> conversationIds);

    List<ConversationMember> findByUserId(UUID userId);

    long countByConversationId(UUID conversationId);

    void deleteByConversationIdAndUserId(UUID conversationId, UUID userId);
}
