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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("dev")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ConversationMemberRepositoryTest {

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private ConversationMemberRepository memberRepository;

    @Autowired
    private UserRepository userRepository;

    private User user1;
    private User user2;
    private Conversation conversation;

    @BeforeEach
    void setUp() {
        memberRepository.deleteAll();
        conversationRepository.deleteAll();
        userRepository.deleteAll();

        user1 = userRepository.save(User.builder()
                .email("u1@example.com")
                .username("user1")
                .passwordHash("hashed")
                .build());

        user2 = userRepository.save(User.builder()
                .email("u2@example.com")
                .username("user2")
                .passwordHash("hashed")
                .build());

        conversation = conversationRepository.save(Conversation.builder()
                .type(ConversationType.DIRECT)
                .build());
    }

    @Test
    @DisplayName("Should save member and find by conversationId and userId")
    void findByConversationIdAndUserId() {
        ConversationMember member = memberRepository.save(ConversationMember.builder()
                .conversationId(conversation.getId())
                .userId(user1.getId())
                .role(MemberRole.OWNER)
                .build());

        Optional<ConversationMember> found = memberRepository.findByConversationIdAndUserId(
                conversation.getId(), user1.getId());

        assertTrue(found.isPresent());
        assertEquals(member.getId(), found.get().getId());
        assertEquals(MemberRole.OWNER, found.get().getRole());
    }

    @Test
    @DisplayName("Should check existence of member in conversation")
    void existsByConversationIdAndUserId() {
        memberRepository.save(ConversationMember.builder()
                .conversationId(conversation.getId())
                .userId(user1.getId())
                .build());

        assertTrue(memberRepository.existsByConversationIdAndUserId(conversation.getId(), user1.getId()));
        assertFalse(memberRepository.existsByConversationIdAndUserId(conversation.getId(), user2.getId()));
    }

    @Test
    @DisplayName("Should find all members by conversation ID")
    void findByConversationId() {
        memberRepository.save(ConversationMember.builder()
                .conversationId(conversation.getId())
                .userId(user1.getId())
                .build());

        memberRepository.save(ConversationMember.builder()
                .conversationId(conversation.getId())
                .userId(user2.getId())
                .build());

        List<ConversationMember> members = memberRepository.findByConversationId(conversation.getId());
        assertEquals(2, members.size());
        assertEquals(2, memberRepository.countByConversationId(conversation.getId()));
    }
}
