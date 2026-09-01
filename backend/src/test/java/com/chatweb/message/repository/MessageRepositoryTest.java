package com.chatweb.message.repository;

import com.chatweb.conversation.entity.Conversation;
import com.chatweb.conversation.entity.ConversationType;
import com.chatweb.conversation.repository.ConversationMemberRepository;
import com.chatweb.conversation.repository.ConversationRepository;
import com.chatweb.message.entity.Message;
import com.chatweb.message.entity.MessageType;
import com.chatweb.user.entity.User;
import com.chatweb.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("dev")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class MessageRepositoryTest {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private ConversationMemberRepository memberRepository;

    @Autowired
    private UserRepository userRepository;

    private User sender;
    private User recipient;
    private Conversation conversation;

    @BeforeEach
    void setUp() {
        messageRepository.deleteAll();
        memberRepository.deleteAll();
        conversationRepository.deleteAll();
        userRepository.deleteAll();

        sender = userRepository.save(User.builder()
                .email("sender@example.com")
                .username("sender")
                .passwordHash("hashed")
                .build());

        recipient = userRepository.save(User.builder()
                .email("recipient@example.com")
                .username("recipient")
                .passwordHash("hashed")
                .build());

        conversation = conversationRepository.save(Conversation.builder()
                .type(ConversationType.DIRECT)
                .build());
    }

    @Test
    @DisplayName("Should find latest message in conversation")
    void findFirstByConversationIdOrderByCreatedAtDesc() throws InterruptedException {
        Message msg1 = messageRepository.save(Message.builder()
                .conversationId(conversation.getId())
                .senderId(sender.getId())
                .content("First message")
                .type(MessageType.TEXT)
                .build());

        Thread.sleep(10);

        Message msg2 = messageRepository.save(Message.builder()
                .conversationId(conversation.getId())
                .senderId(recipient.getId())
                .content("Second message")
                .type(MessageType.TEXT)
                .build());

        Optional<Message> latest = messageRepository.findFirstByConversationIdOrderByCreatedAtDesc(conversation.getId());
        assertTrue(latest.isPresent());
        assertEquals(msg2.getId(), latest.get().getId());
        assertEquals("Second message", latest.get().getContent());
    }

    @Test
    @DisplayName("Should query messages before cursor timestamp for cursor pagination")
    void findMessagesBeforeCursor() {
        Instant baseTime = Instant.now().minus(10, ChronoUnit.MINUTES);

        Message msg1 = messageRepository.save(Message.builder()
                .conversationId(conversation.getId())
                .senderId(sender.getId())
                .content("Msg 1")
                .type(MessageType.TEXT)
                .createdAt(baseTime.plus(1, ChronoUnit.MINUTES))
                .build());

        Message msg2 = messageRepository.save(Message.builder()
                .conversationId(conversation.getId())
                .senderId(recipient.getId())
                .content("Msg 2")
                .type(MessageType.TEXT)
                .createdAt(baseTime.plus(2, ChronoUnit.MINUTES))
                .build());

        Message msg3 = messageRepository.save(Message.builder()
                .conversationId(conversation.getId())
                .senderId(sender.getId())
                .content("Msg 3")
                .type(MessageType.TEXT)
                .createdAt(baseTime.plus(3, ChronoUnit.MINUTES))
                .build());

        // Initial fetch with PageRequest limit 2
        List<Message> firstPage = messageRepository.findByConversationIdOrderByCreatedAtDesc(
                conversation.getId(), PageRequest.of(0, 2));

        assertEquals(2, firstPage.size());
        assertEquals(msg3.getId(), firstPage.get(0).getId());
        assertEquals(msg2.getId(), firstPage.get(1).getId());

        // Next page before cursor (msg2's createdAt)
        List<Message> secondPage = messageRepository.findMessagesBeforeCursor(
                conversation.getId(), msg2.getCreatedAt(), PageRequest.of(0, 2));

        assertEquals(1, secondPage.size());
        assertEquals(msg1.getId(), secondPage.get(0).getId());
    }

    @Test
    @DisplayName("Should count unread messages correctly")
    void countUnreadMessages() {
        Instant baseTime = Instant.now().minus(10, ChronoUnit.MINUTES);

        // Sender sends 3 messages
        Message msg1 = messageRepository.save(Message.builder()
                .conversationId(conversation.getId())
                .senderId(sender.getId())
                .content("Msg 1")
                .createdAt(baseTime.plus(1, ChronoUnit.MINUTES))
                .build());

        Message msg2 = messageRepository.save(Message.builder()
                .conversationId(conversation.getId())
                .senderId(sender.getId())
                .content("Msg 2")
                .createdAt(baseTime.plus(2, ChronoUnit.MINUTES))
                .build());

        Message msg3 = messageRepository.save(Message.builder()
                .conversationId(conversation.getId())
                .senderId(sender.getId())
                .content("Msg 3")
                .createdAt(baseTime.plus(3, ChronoUnit.MINUTES))
                .build());

        // For recipient who has not read anything
        long totalUnread = messageRepository.countUnreadMessages(conversation.getId(), recipient.getId());
        assertEquals(3, totalUnread);

        // For recipient who read up to msg1
        long unreadAfterMsg1 = messageRepository.countUnreadMessagesAfter(
                conversation.getId(), recipient.getId(), msg1.getCreatedAt());
        assertEquals(2, unreadAfterMsg1);

        // For sender themselves (own messages are excluded)
        long senderUnread = messageRepository.countUnreadMessages(conversation.getId(), sender.getId());
        assertEquals(0, senderUnread);
    }
}
