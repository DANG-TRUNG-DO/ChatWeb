package com.chatweb.realtime.controller;

import com.chatweb.auth.security.UserPrincipal;
import com.chatweb.conversation.service.ConversationService;
import com.chatweb.message.dto.MarkAsReadRequest;
import com.chatweb.message.dto.SendMessageRequest;
import com.chatweb.message.service.MessageService;
import com.chatweb.realtime.dto.ChatMessagePayload;
import com.chatweb.realtime.dto.MessageReadPayload;
import com.chatweb.realtime.dto.TypingPayload;
import com.chatweb.realtime.service.RealtimeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {

    private final MessageService messageService;
    private final ConversationService conversationService;
    private final RealtimeService realtimeService;

    @MessageMapping("/chat.send")
    public void handleSendMessage(@Valid @Payload ChatMessagePayload payload, Principal principal) {
        UserPrincipal userPrincipal = getUserPrincipal(principal);
        log.debug("WebSocket chat.send received from user {} for conversation {}", userPrincipal.getUsername(), payload.getConversationId());

        SendMessageRequest request = SendMessageRequest.builder()
                .content(payload.getContent())
                .type(payload.getType())
                .replyToId(payload.getReplyToId())
                .build();

        messageService.sendMessage(userPrincipal.getId(), payload.getConversationId(), request);
    }

    @MessageMapping("/chat.read")
    public void handleMarkAsRead(@Valid @Payload MessageReadPayload payload, Principal principal) {
        UserPrincipal userPrincipal = getUserPrincipal(principal);
        log.debug("WebSocket chat.read received from user {} for conversation {}", userPrincipal.getUsername(), payload.getConversationId());

        MarkAsReadRequest request = MarkAsReadRequest.builder()
                .messageId(payload.getMessageId())
                .build();

        messageService.markAsRead(userPrincipal.getId(), payload.getConversationId(), request);
    }

    @MessageMapping("/chat.typing")
    public void handleTyping(@Valid @Payload TypingPayload payload, Principal principal) {
        UserPrincipal userPrincipal = getUserPrincipal(principal);
        log.debug("WebSocket chat.typing received from user {} for conversation {}", userPrincipal.getUsername(), payload.getConversationId());

        conversationService.validateUserInConversation(payload.getConversationId(), userPrincipal.getId());

        payload.setUserId(userPrincipal.getId());
        payload.setUsername(userPrincipal.getUsername());

        realtimeService.broadcastTyping(payload.getConversationId(), payload);
    }

    private UserPrincipal getUserPrincipal(Principal principal) {
        if (principal instanceof Authentication auth && auth.getPrincipal() instanceof UserPrincipal userPrincipal) {
            return userPrincipal;
        }
        throw new BadCredentialsException("User is not authenticated for WebSocket communication");
    }
}
