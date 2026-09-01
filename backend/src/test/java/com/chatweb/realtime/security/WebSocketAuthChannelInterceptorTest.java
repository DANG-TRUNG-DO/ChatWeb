package com.chatweb.realtime.security;

import com.chatweb.auth.security.CustomUserDetailsService;
import com.chatweb.auth.security.UserPrincipal;
import com.chatweb.auth.service.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("WebSocketAuthChannelInterceptor Unit Tests")
class WebSocketAuthChannelInterceptorTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private MessageChannel messageChannel;

    @InjectMocks
    private WebSocketAuthChannelInterceptor interceptor;

    private UUID userId;
    private UserPrincipal userPrincipal;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        userPrincipal = UserPrincipal.builder()
                .id(userId)
                .email("user@example.com")
                .username("testuser")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")))
                .build();
    }

    @Test
    @DisplayName("Should authenticate successfully when Authorization header has valid Bearer token")
    void preSend_ValidBearerTokenInAuthorizationHeader_AuthenticatesSuccessfully() {
        String token = "valid-jwt-token";
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setLeaveMutable(true);
        accessor.setNativeHeader("Authorization", "Bearer " + token);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(jwtTokenProvider.isTokenValid(token)).thenReturn(true);
        when(jwtTokenProvider.extractUserId(token)).thenReturn(userId);
        when(userDetailsService.loadUserById(userId)).thenReturn(userPrincipal);

        Message<?> result = interceptor.preSend(message, messageChannel);

        assertThat(result).isNotNull();
        StompHeaderAccessor resultAccessor = MessageHeaderAccessor.getAccessor(result, StompHeaderAccessor.class);
        assertThat(resultAccessor).isNotNull();
        assertThat(resultAccessor.getUser()).isInstanceOf(UsernamePasswordAuthenticationToken.class);
        UsernamePasswordAuthenticationToken auth = (UsernamePasswordAuthenticationToken) resultAccessor.getUser();
        assertThat(auth.getPrincipal()).isEqualTo(userPrincipal);
    }

    @Test
    @DisplayName("Should authenticate successfully when token header has valid JWT token")
    void preSend_ValidTokenInTokenHeader_AuthenticatesSuccessfully() {
        String token = "valid-jwt-token";
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setLeaveMutable(true);
        accessor.setNativeHeader("token", token);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(jwtTokenProvider.isTokenValid(token)).thenReturn(true);
        when(jwtTokenProvider.extractUserId(token)).thenReturn(userId);
        when(userDetailsService.loadUserById(userId)).thenReturn(userPrincipal);

        Message<?> result = interceptor.preSend(message, messageChannel);

        assertThat(result).isNotNull();
        StompHeaderAccessor resultAccessor = MessageHeaderAccessor.getAccessor(result, StompHeaderAccessor.class);
        assertThat(resultAccessor).isNotNull();
        assertThat(resultAccessor.getUser()).isInstanceOf(UsernamePasswordAuthenticationToken.class);
    }

    @Test
    @DisplayName("Should throw BadCredentialsException when token is missing in CONNECT frame")
    void preSend_MissingToken_ThrowsBadCredentialsException() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThatThrownBy(() -> interceptor.preSend(message, messageChannel))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Missing or invalid JWT token");
    }

    @Test
    @DisplayName("Should throw BadCredentialsException when token is invalid")
    void preSend_InvalidToken_ThrowsBadCredentialsException() {
        String token = "invalid-token";
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer " + token);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(jwtTokenProvider.isTokenValid(token)).thenReturn(false);

        assertThatThrownBy(() -> interceptor.preSend(message, messageChannel))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Missing or invalid JWT token");
    }

    @Test
    @DisplayName("Should throw BadCredentialsException when user not found in database")
    void preSend_UserNotFound_ThrowsBadCredentialsException() {
        String token = "valid-token-unknown-user";
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer " + token);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(jwtTokenProvider.isTokenValid(token)).thenReturn(true);
        when(jwtTokenProvider.extractUserId(token)).thenReturn(userId);
        when(userDetailsService.loadUserById(userId)).thenThrow(new UsernameNotFoundException("User not found"));

        assertThatThrownBy(() -> interceptor.preSend(message, messageChannel))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Failed to authenticate WebSocket connection");
    }

    @Test
    @DisplayName("Should pass non-CONNECT messages through without token validation")
    void preSend_NonConnectCommand_PassesThrough() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setDestination("/app/chat.send");
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(message, messageChannel);

        assertThat(result).isSameAs(message);
        verifyNoInteractions(jwtTokenProvider, userDetailsService);
    }
}
