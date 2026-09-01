package com.chatweb.message.dto;

import com.chatweb.message.entity.Message;
import com.chatweb.message.entity.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageSummaryResponse {

    private UUID id;
    private UUID conversationId;
    private UUID senderId;
    private String senderName;
    private String content;
    private MessageType type;
    private boolean deleted;
    private Instant createdAt;

    public static MessageSummaryResponse fromEntity(Message message, String senderName) {
        if (message == null) {
            return null;
        }
        return MessageSummaryResponse.builder()
                .id(message.getId())
                .conversationId(message.getConversationId())
                .senderId(message.getSenderId())
                .senderName(senderName)
                .content(message.isDeleted() ? "Tin nhắn đã bị thu hồi" : message.getContent())
                .type(message.getType())
                .deleted(message.isDeleted())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
