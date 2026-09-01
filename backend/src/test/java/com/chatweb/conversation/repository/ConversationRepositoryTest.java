package com.chatweb.conversation.repository;

import com.chatweb.conversation.entity.Conversation;
import com.chatweb.conversation.entity.ConversationMember;
import com.chatweb.conversation.entity.ConversationType;
import com.chatweb.conversation.entity.MemberRole;
import com.chatweb.user.entity.User;
import com.chatweb.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("dev")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ConversationRepositoryTest {

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private ConversationMemberRepository memberRepository;

    @Autowired
    private UserRepository userRepository;

    private User userA;
    private User userB;
    private User userC;

    @BeforeEach
    void setUp() {
        memberRepository.deleteAll();
        conversationRepository.deleteAll();
        userRepository.deleteAll();

        userA = userRepository.save(User.builder()
                .email("userA@example.com")
                .username("userA")
                .displayName("User A")
                .passwordHash("hashed")
                .build());

        userB = userRepository.save(User.builder()
                .email("userB@example.com")
                .username("userB")
                .displayName("User B")
                .passwordHash("hashed")
                .build());

        userC = userRepository.save(User.builder()
                .email("userC@example.com")
                .username("userC")
                .displayName("User C")
                .passwordHash("hashed")
                .build());
    }

    @Test
    @DisplayName("Should find direct conversation between two users")
    void findDirectConversationBetweenUsers_Success() {
        Conversation conv = conversationRepository.save(Conversation.builder()
                .type(ConversationType.DIRECT)
                .build());

        memberRepository.save(ConversationMember.builder()
                .conversationId(conv.getId())
                .userId(userA.getId())
                .role(MemberRole.MEMBER)
                .build());

        memberRepository.save(ConversationMember.builder()
                .conversationId(conv.getId())
                .userId(userB.getId())
                .role(MemberRole.MEMBER)
                .build());

        Optional<Conversation> foundForward = conversationRepository.findDirectConversationBetweenUsers(
                ConversationType.DIRECT, userA.getId(), userB.getId());
        Optional<Conversation> foundReverse = conversationRepository.findDirectConversationBetweenUsers(
                ConversationType.DIRECT, userB.getId(), userA.getId());

        assertTrue(foundForward.isPresent());
        assertTrue(foundReverse.isPresent());
        assertEquals(conv.getId(), foundForward.get().getId());
        assertEquals(conv.getId(), foundReverse.get().getId());
    }

    @Test
    @DisplayName("Should return empty when direct conversation between users does not exist")
    void findDirectConversationBetweenUsers_NotFound() {
        Optional<Conversation> found = conversationRepository.findDirectConversationBetweenUsers(
                ConversationType.DIRECT, userA.getId(), userC.getId());

        assertTrue(found.isEmpty());
    }

    @Test
    @DisplayName("Should find all conversations for a specific user ordered by updatedAt DESC")
    void findByUserId_Success() {
        Conversation conv1 = conversationRepository.save(Conversation.builder()
                .type(ConversationType.DIRECT)
                .build());

        Conversation conv2 = conversationRepository.save(Conversation.builder()
                .type(ConversationType.DIRECT)
                .build());

        memberRepository.save(ConversationMember.builder()
                .conversationId(conv1.getId())
                .userId(userA.getId())
                .build());

        memberRepository.save(ConversationMember.builder()
                .conversationId(conv2.getId())
                .userId(userA.getId())
                .build());

        List<Conversation> conversations = conversationRepository.findByUserId(userA.getId());

        assertEquals(2, conversations.size());
        assertNotNull(conversations.get(0).getCreatedAt());
        assertNotNull(conversations.get(0).getUpdatedAt());
    }
}
