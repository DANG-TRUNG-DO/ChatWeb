package com.chatweb.conversation.service;

import com.chatweb.conversation.dto.ConversationDetailResponse;
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
import com.chatweb.message.entity.MessageType;
import com.chatweb.message.service.MessageService;
import com.chatweb.user.dto.UserSummaryResponse;
import com.chatweb.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private ConversationMemberRepository memberRepository;

    @Mock
    private UserService userService;

    @Mock
    private MessageService messageService;

    @InjectMocks
    private ConversationServiceImpl conversationService;

    private UUID user1Id;
    private UUID user2Id;
    private UUID conversationId;
    private UserSummaryResponse user1Summary;
    private UserSummaryResponse user2Summary;

    @BeforeEach
    void setUp() {
        user1Id = UUID.randomUUID();
        user2Id = UUID.randomUUID();
        conversationId = UUID.randomUUID();

        user1Summary = UserSummaryResponse.builder()
                .id(user1Id)
                .username("user1")
                .displayName("User One")
                .build();

        user2Summary = UserSummaryResponse.builder()
                .id(user2Id)
                .username("user2")
                .displayName("User Two")
                .build();
    }

    @Test
    @DisplayName("Should create new direct conversation when none exists")
    void getOrCreateDirectConversation_CreatesNew() {
        when(userService.getUserById(user2Id)).thenReturn(user2Summary);
        when(conversationRepository.findDirectConversationBetweenUsers(ConversationType.DIRECT, user1Id, user2Id))
                .thenReturn(Optional.empty());

        Conversation savedConv = Conversation.builder()
                .id(conversationId)
                .type(ConversationType.DIRECT)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(conversationRepository.save(any(Conversation.class))).thenReturn(savedConv);

        ConversationMember m1 = ConversationMember.builder()
                .id(UUID.randomUUID())
                .conversationId(conversationId)
                .userId(user1Id)
                .role(MemberRole.MEMBER)
                .build();

        ConversationMember m2 = ConversationMember.builder()
                .id(UUID.randomUUID())
                .conversationId(conversationId)
                .userId(user2Id)
                .role(MemberRole.MEMBER)
                .build();

        when(memberRepository.saveAll(anyList())).thenReturn(List.of(m1, m2));
        when(userService.getUsersSummaryByIds(any())).thenReturn(Map.of(user1Id, user1Summary, user2Id, user2Summary));

        ConversationDetailResponse response = conversationService.getOrCreateDirectConversation(user1Id, user2Id);

        assertNotNull(response);
        assertEquals(conversationId, response.getId());
        assertEquals(ConversationType.DIRECT, response.getType());
        assertEquals("User Two", response.getName());
        assertEquals(2, response.getMembers().size());

        verify(conversationRepository).save(any(Conversation.class));
        verify(memberRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("Should return existing direct conversation if already present")
    void getOrCreateDirectConversation_ReturnsExisting() {
        when(userService.getUserById(user2Id)).thenReturn(user2Summary);

        Conversation existingConv = Conversation.builder()
                .id(conversationId)
                .type(ConversationType.DIRECT)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(conversationRepository.findDirectConversationBetweenUsers(ConversationType.DIRECT, user1Id, user2Id))
                .thenReturn(Optional.of(existingConv));

        ConversationMember m1 = ConversationMember.builder()
                .id(UUID.randomUUID())
                .conversationId(conversationId)
                .userId(user1Id)
                .role(MemberRole.MEMBER)
                .build();

        ConversationMember m2 = ConversationMember.builder()
                .id(UUID.randomUUID())
                .conversationId(conversationId)
                .userId(user2Id)
                .role(MemberRole.MEMBER)
                .build();

        when(memberRepository.findByConversationId(conversationId)).thenReturn(List.of(m1, m2));
        when(userService.getUsersSummaryByIds(any())).thenReturn(Map.of(user1Id, user1Summary, user2Id, user2Summary));

        ConversationDetailResponse response = conversationService.getOrCreateDirectConversation(user1Id, user2Id);

        assertNotNull(response);
        assertEquals(conversationId, response.getId());
        verify(conversationRepository, never()).save(any(Conversation.class));
    }

    @Test
    @DisplayName("Should throw InvalidConversationException when creating conversation with oneself")
    void getOrCreateDirectConversation_SelfChat_ThrowsException() {
        assertThrows(InvalidConversationException.class, () ->
                conversationService.getOrCreateDirectConversation(user1Id, user1Id));
    }

    @Test
    @DisplayName("Should return conversation details when user is a member")
    void getConversationById_Success() {
        when(memberRepository.existsByConversationIdAndUserId(conversationId, user1Id)).thenReturn(true);

        Conversation conv = Conversation.builder()
                .id(conversationId)
                .type(ConversationType.DIRECT)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conv));

        ConversationMember m1 = ConversationMember.builder()
                .id(UUID.randomUUID())
                .conversationId(conversationId)
                .userId(user1Id)
                .build();

        ConversationMember m2 = ConversationMember.builder()
                .id(UUID.randomUUID())
                .conversationId(conversationId)
                .userId(user2Id)
                .build();

        when(memberRepository.findByConversationId(conversationId)).thenReturn(List.of(m1, m2));
        when(userService.getUsersSummaryByIds(any())).thenReturn(Map.of(user1Id, user1Summary, user2Id, user2Summary));

        ConversationDetailResponse response = conversationService.getConversationById(user1Id, conversationId);

        assertNotNull(response);
        assertEquals(conversationId, response.getId());
        assertEquals("User Two", response.getName());
    }

    @Test
    @DisplayName("Should throw ConversationAccessDeniedException when user is not a member")
    void getConversationById_NotMember_ThrowsException() {
        Conversation conv = Conversation.builder()
                .id(conversationId)
                .type(ConversationType.DIRECT)
                .build();

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conv));
        when(memberRepository.existsByConversationIdAndUserId(conversationId, user1Id)).thenReturn(false);

        assertThrows(ConversationAccessDeniedException.class, () ->
                conversationService.getConversationById(user1Id, conversationId));
    }

    @Test
    @DisplayName("Should return list of conversations for user with last message and unread count")
    void getUserConversations_Success() {
        Conversation conv = Conversation.builder()
                .id(conversationId)
                .type(ConversationType.DIRECT)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(conversationRepository.findByUserId(user1Id)).thenReturn(List.of(conv));

        ConversationMember m1 = ConversationMember.builder()
                .conversationId(conversationId)
                .userId(user1Id)
                .build();

        ConversationMember m2 = ConversationMember.builder()
                .conversationId(conversationId)
                .userId(user2Id)
                .build();

        when(memberRepository.findByConversationIdIn(List.of(conversationId))).thenReturn(List.of(m1, m2));
        when(userService.getUsersSummaryByIds(any())).thenReturn(Map.of(user1Id, user1Summary, user2Id, user2Summary));

        MessageSummaryResponse lastMsg = MessageSummaryResponse.builder()
                .id(UUID.randomUUID())
                .conversationId(conversationId)
                .senderId(user2Id)
                .senderName("User Two")
                .content("Hello")
                .type(MessageType.TEXT)
                .build();

        when(messageService.getLatestMessages(List.of(conversationId))).thenReturn(Map.of(conversationId, lastMsg));
        when(messageService.getUnreadCount(eq(conversationId), eq(user1Id), any())).thenReturn(1L);

        List<ConversationSummaryResponse> list = conversationService.getUserConversations(user1Id);

        assertEquals(1, list.size());
        assertEquals(conversationId, list.get(0).getId());
        assertEquals("User Two", list.get(0).getName());
        assertEquals(1L, list.get(0).getUnreadCount());
        assertNotNull(list.get(0).getLastMessage());
        assertEquals("Hello", list.get(0).getLastMessage().getContent());
    }
}
