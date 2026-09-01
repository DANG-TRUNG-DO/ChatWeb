package com.chatweb.conversation.service;

import com.chatweb.common.exception.ResourceNotFoundException;
import com.chatweb.conversation.dto.ConversationDetailResponse;
import com.chatweb.conversation.dto.ConversationMemberResponse;
import com.chatweb.conversation.dto.ConversationSummaryResponse;
import com.chatweb.conversation.entity.Conversation;
import com.chatweb.conversation.entity.ConversationMember;
import com.chatweb.conversation.entity.ConversationType;
import com.chatweb.conversation.entity.MemberRole;
import com.chatweb.conversation.exception.ConversationAccessDeniedException;
import com.chatweb.conversation.exception.InvalidConversationException;
import com.chatweb.conversation.repository.ConversationMemberRepository;
import com.chatweb.conversation.repository.ConversationRepository;
import com.chatweb.message.dto.MessageSummaryResponse;
import com.chatweb.message.service.MessageService;
import com.chatweb.user.dto.UserSummaryResponse;
import com.chatweb.user.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ConversationServiceImpl implements ConversationService {

    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository memberRepository;
    private final UserService userService;
    private final MessageService messageService;

    public ConversationServiceImpl(
            ConversationRepository conversationRepository,
            ConversationMemberRepository memberRepository,
            UserService userService,
            @Lazy MessageService messageService
    ) {
        this.conversationRepository = conversationRepository;
        this.memberRepository = memberRepository;
        this.userService = userService;
        this.messageService = messageService;
    }

    @Override
    @Transactional
    public ConversationDetailResponse getOrCreateDirectConversation(UUID currentUserId, UUID recipientId) {
        if (currentUserId.equals(recipientId)) {
            throw new InvalidConversationException("Cannot create a direct conversation with yourself");
        }

        // Validate recipient exists
        UserSummaryResponse recipientSummary = userService.getUserById(recipientId);

        // Check if direct conversation already exists
        Optional<Conversation> existing = conversationRepository.findDirectConversationBetweenUsers(
                ConversationType.DIRECT, currentUserId, recipientId);

        Conversation conversation;
        List<ConversationMember> members;

        if (existing.isPresent()) {
            conversation = existing.get();
            members = memberRepository.findByConversationId(conversation.getId());
        } else {
            conversation = conversationRepository.save(Conversation.builder()
                    .type(ConversationType.DIRECT)
                    .build());

            ConversationMember memberCurrent = ConversationMember.builder()
                    .conversationId(conversation.getId())
                    .userId(currentUserId)
                    .role(MemberRole.MEMBER)
                    .build();

            ConversationMember memberRecipient = ConversationMember.builder()
                    .conversationId(conversation.getId())
                    .userId(recipientId)
                    .role(MemberRole.MEMBER)
                    .build();

            members = memberRepository.saveAll(List.of(memberCurrent, memberRecipient));
            log.info("Created new direct conversation {} between user {} and {}",
                    conversation.getId(), currentUserId, recipientId);
        }

        Set<UUID> memberUserIds = members.stream()
                .map(ConversationMember::getUserId)
                .collect(Collectors.toSet());
        Map<UUID, UserSummaryResponse> userMap = userService.getUsersSummaryByIds(memberUserIds);

        List<ConversationMemberResponse> memberResponses = members.stream()
                .map(m -> ConversationMemberResponse.fromEntity(m, userMap.get(m.getUserId())))
                .toList();

        return ConversationDetailResponse.fromEntity(conversation, recipientSummary, memberResponses);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConversationSummaryResponse> getUserConversations(UUID currentUserId) {
        List<Conversation> conversations = conversationRepository.findByUserId(currentUserId);
        if (conversations.isEmpty()) {
            return Collections.emptyList();
        }

        List<UUID> conversationIds = conversations.stream()
                .map(Conversation::getId)
                .toList();

        List<ConversationMember> allMembers = memberRepository.findByConversationIdIn(conversationIds);
        Map<UUID, List<ConversationMember>> membersByConv = allMembers.stream()
                .collect(Collectors.groupingBy(ConversationMember::getConversationId));

        Set<UUID> userIds = allMembers.stream()
                .map(ConversationMember::getUserId)
                .collect(Collectors.toSet());
        Map<UUID, UserSummaryResponse> userMap = userService.getUsersSummaryByIds(userIds);

        Map<UUID, MessageSummaryResponse> latestMessages = (messageService != null)
                ? messageService.getLatestMessages(conversationIds)
                : Collections.emptyMap();

        List<ConversationSummaryResponse> results = new ArrayList<>();

        for (Conversation conversation : conversations) {
            List<ConversationMember> convMembers = membersByConv.getOrDefault(conversation.getId(), Collections.emptyList());

            UserSummaryResponse partner = null;
            if (conversation.getType() == ConversationType.DIRECT) {
                UUID partnerId = convMembers.stream()
                        .map(ConversationMember::getUserId)
                        .filter(id -> !id.equals(currentUserId))
                        .findFirst()
                        .orElse(null);

                if (partnerId != null) {
                    partner = userMap.get(partnerId);
                }
            }

            ConversationMember currentMember = convMembers.stream()
                    .filter(m -> m.getUserId().equals(currentUserId))
                    .findFirst()
                    .orElse(null);

            UUID lastReadMsgId = currentMember != null ? currentMember.getLastReadMessageId() : null;
            long unreadCount = (messageService != null)
                    ? messageService.getUnreadCount(conversation.getId(), currentUserId, lastReadMsgId)
                    : 0;

            MessageSummaryResponse lastMsg = latestMessages.get(conversation.getId());

            results.add(ConversationSummaryResponse.fromEntity(conversation, partner, lastMsg, unreadCount));
        }

        return results;
    }

    @Override
    @Transactional(readOnly = true)
    public ConversationDetailResponse getConversationById(UUID currentUserId, UUID conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", "id", conversationId));

        validateUserInConversation(conversationId, currentUserId);

        List<ConversationMember> members = memberRepository.findByConversationId(conversationId);
        Set<UUID> memberUserIds = members.stream()
                .map(ConversationMember::getUserId)
                .collect(Collectors.toSet());
        Map<UUID, UserSummaryResponse> userMap = userService.getUsersSummaryByIds(memberUserIds);

        UserSummaryResponse partner = null;
        if (conversation.getType() == ConversationType.DIRECT) {
            UUID partnerId = members.stream()
                    .map(ConversationMember::getUserId)
                    .filter(id -> !id.equals(currentUserId))
                    .findFirst()
                    .orElse(null);

            if (partnerId != null) {
                partner = userMap.get(partnerId);
            }
        }

        List<ConversationMemberResponse> memberResponses = members.stream()
                .map(m -> ConversationMemberResponse.fromEntity(m, userMap.get(m.getUserId())))
                .toList();

        return ConversationDetailResponse.fromEntity(conversation, partner, memberResponses);
    }

    @Override
    @Transactional
    public void deleteConversation(UUID currentUserId, UUID conversationId) {
        ConversationMember member = memberRepository.findByConversationIdAndUserId(conversationId, currentUserId)
                .orElseThrow(() -> new ConversationAccessDeniedException("You are not a member of this conversation"));

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", "id", conversationId));

        if (conversation.getType() == ConversationType.DIRECT || member.getRole() == MemberRole.OWNER) {
            conversationRepository.delete(conversation);
            log.info("User {} deleted conversation {}", currentUserId, conversationId);
        } else {
            memberRepository.delete(member);
            log.info("User {} left group conversation {}", currentUserId, conversationId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isUserMemberOfConversation(UUID conversationId, UUID userId) {
        return memberRepository.existsByConversationIdAndUserId(conversationId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public void validateUserInConversation(UUID conversationId, UUID userId) {
        if (!isUserMemberOfConversation(conversationId, userId)) {
            throw new ConversationAccessDeniedException("You are not a member of this conversation");
        }
    }

    @Override
    @Transactional
    public void updateConversationTimestamp(UUID conversationId) {
        conversationRepository.findById(conversationId).ifPresent(c -> {
            c.setUpdatedAt(Instant.now());
            conversationRepository.save(c);
        });
    }
}
