package com.chatweb.realtime.controller;

import com.chatweb.auth.repository.RefreshTokenRepository;
import com.chatweb.auth.service.JwtTokenProvider;
import com.chatweb.conversation.entity.Conversation;
import com.chatweb.conversation.entity.ConversationMember;
import com.chatweb.conversation.entity.ConversationType;
import com.chatweb.conversation.entity.MemberRole;
import com.chatweb.conversation.repository.ConversationMemberRepository;
import com.chatweb.conversation.repository.ConversationRepository;
import com.chatweb.message.entity.Message;
import com.chatweb.message.entity.MessageType;
import com.chatweb.message.repository.MessageRepository;
import com.chatweb.realtime.dto.ChatMessagePayload;
import com.chatweb.realtime.dto.MessageReadPayload;
import com.chatweb.realtime.dto.TypingPayload;
import com.chatweb.realtime.dto.WebSocketEvent;
import com.chatweb.realtime.dto.WebSocketEventType;
import com.chatweb.user.entity.User;
import com.chatweb.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
class ChatWebSocketIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private ConversationMemberRepository conversationMemberRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    private WebSocketStompClient stompClient;
    private User userA;
    private User userB;
    private Conversation conversation;
    private String tokenA;
    private String tokenB;

    @BeforeEach
    void setUp() {
        cleanDatabase();

        userA = userRepository.save(User.builder()
                .email("alice@chat.com")
                .username("alice")
                .passwordHash("hash_alice")
                .displayName("Alice")
                .build());

        userB = userRepository.save(User.builder()
                .email("bob@chat.com")
                .username("bob")
                .passwordHash("hash_bob")
                .displayName("Bob")
                .build());

        tokenA = jwtTokenProvider.generateAccessToken(userA);
        tokenB = jwtTokenProvider.generateAccessToken(userB);

        conversation = conversationRepository.save(Conversation.builder()
                .type(ConversationType.DIRECT)
                .build());

        conversationMemberRepository.save(ConversationMember.builder()
                .conversationId(conversation.getId())
                .userId(userA.getId())
                .role(MemberRole.MEMBER)
                .build());

        conversationMemberRepository.save(ConversationMember.builder()
                .conversationId(conversation.getId())
                .userId(userB.getId())
                .role(MemberRole.MEMBER)
                .build());

        List<Transport> transports = Collections.singletonList(new WebSocketTransport(new StandardWebSocketClient()));
        stompClient = new WebSocketStompClient(new SockJsClient(transports));
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(objectMapper);
        stompClient.setMessageConverter(converter);
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    private void cleanDatabase() {
        messageRepository.deleteAll();
        conversationMemberRepository.deleteAll();
        conversationRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    private StompSession connectSession(String token) throws Exception {
        String url = "http://localhost:" + port + "/ws";
        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer " + token);

        CompletableFuture<StompSession> sessionFuture = new CompletableFuture<>();
        stompClient.connectAsync(url, new WebSocketHttpHeaders(), connectHeaders, new StompSessionHandlerAdapter() {
            @Override
            public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                sessionFuture.complete(session);
            }
        });
        return sessionFuture.get(5, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("Should send message via /app/chat.send, persist to DB, and broadcast to /topic/conversations/{id}")
    void sendAndReceiveMessage_Realtime_Success() throws Exception {
        StompSession sessionB = connectSession(tokenB);
        StompSession sessionA = connectSession(tokenA);

        try {
            CompletableFuture<WebSocketEvent<?>> receivedEventFuture = new CompletableFuture<>();

            sessionB.subscribe("/topic/conversations/" + conversation.getId(), new StompFrameHandler() {
                @Override
                public Type getPayloadType(StompHeaders headers) {
                    return WebSocketEvent.class;
                }

                @Override
                public void handleFrame(StompHeaders headers, Object payload) {
                    receivedEventFuture.complete((WebSocketEvent<?>) payload);
                }
            });

            Thread.sleep(500);

            ChatMessagePayload messagePayload = ChatMessagePayload.builder()
                    .conversationId(conversation.getId())
                    .content("Hello Bob from Alice in realtime!")
                    .type(MessageType.TEXT)
                    .build();

            sessionA.send("/app/chat.send", messagePayload);

            WebSocketEvent<?> event = receivedEventFuture.get(5, TimeUnit.SECONDS);
            assertThat(event).isNotNull();
            assertThat(event.getType()).isEqualTo(WebSocketEventType.MESSAGE_SENT);

            assertThat(messageRepository.findByConversationIdOrderByCreatedAtDesc(conversation.getId(), org.springframework.data.domain.Pageable.unpaged()))
                    .hasSize(1)
                    .first()
                    .satisfies(m -> {
                        assertThat(m.getContent()).isEqualTo("Hello Bob from Alice in realtime!");
                        assertThat(m.getSenderId()).isEqualTo(userA.getId());
                    });
        } finally {
            sessionA.disconnect();
            sessionB.disconnect();
        }
    }

    @Test
    @DisplayName("Should send typing indicator via /app/chat.typing and broadcast to subscribers")
    void sendAndReceiveTypingIndicator_Realtime_Success() throws Exception {
        StompSession sessionB = connectSession(tokenB);
        StompSession sessionA = connectSession(tokenA);

        try {
            CompletableFuture<WebSocketEvent<?>> receivedEventFuture = new CompletableFuture<>();

            sessionB.subscribe("/topic/conversations/" + conversation.getId(), new StompFrameHandler() {
                @Override
                public Type getPayloadType(StompHeaders headers) {
                    return WebSocketEvent.class;
                }

                @Override
                public void handleFrame(StompHeaders headers, Object payload) {
                    receivedEventFuture.complete((WebSocketEvent<?>) payload);
                }
            });

            Thread.sleep(500);

            TypingPayload typingPayload = TypingPayload.builder()
                    .conversationId(conversation.getId())
                    .typing(true)
                    .build();

            sessionA.send("/app/chat.typing", typingPayload);

            WebSocketEvent<?> event = receivedEventFuture.get(5, TimeUnit.SECONDS);
            assertThat(event).isNotNull();
            assertThat(event.getType()).isEqualTo(WebSocketEventType.TYPING);
        } finally {
            sessionA.disconnect();
            sessionB.disconnect();
        }
    }

    @Test
    @DisplayName("Should send read receipt via /app/chat.read and broadcast to subscribers")
    void sendAndReceiveMarkAsRead_Realtime_Success() throws Exception {
        // Create an existing message in conversation first
        Message msg = messageRepository.save(Message.builder()
                .conversationId(conversation.getId())
                .senderId(userB.getId())
                .content("Hello Alice")
                .type(MessageType.TEXT)
                .edited(false)
                .deleted(false)
                .build());

        StompSession sessionB = connectSession(tokenB);
        StompSession sessionA = connectSession(tokenA);

        try {
            CompletableFuture<WebSocketEvent<?>> receivedEventFuture = new CompletableFuture<>();

            sessionB.subscribe("/topic/conversations/" + conversation.getId(), new StompFrameHandler() {
                @Override
                public Type getPayloadType(StompHeaders headers) {
                    return WebSocketEvent.class;
                }

                @Override
                public void handleFrame(StompHeaders headers, Object payload) {
                    receivedEventFuture.complete((WebSocketEvent<?>) payload);
                }
            });

            Thread.sleep(500);

            MessageReadPayload readPayload = MessageReadPayload.builder()
                    .conversationId(conversation.getId())
                    .messageId(msg.getId())
                    .build();

            sessionA.send("/app/chat.read", readPayload);

            WebSocketEvent<?> event = receivedEventFuture.get(5, TimeUnit.SECONDS);
            assertThat(event).isNotNull();
            assertThat(event.getType()).isEqualTo(WebSocketEventType.MESSAGE_READ);
        } finally {
            sessionA.disconnect();
            sessionB.disconnect();
        }
    }
}
