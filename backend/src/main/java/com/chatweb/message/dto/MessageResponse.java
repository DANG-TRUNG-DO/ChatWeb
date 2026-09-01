package com.chatweb.message.dto;

import com.chatweb.message.entity.Message;
import com.chatweb.message.entity.MessageType;
import com.chatweb.user.dto.UserSummaryResponse;
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
public class MessageResponse {

    private UUID id;
    private UUID conversationId;
    private UserSummaryResponse sender;
    private String content;
    private MessageType type;
    private MessageSummaryResponse replyTo;
    private boolean edited;
    private boolean deleted;
    private Instant createdAt;
    private Instant updatedAt;

    public static MessageResponse fromEntity(Message message, UserSummaryResponse sender, MessageSummaryResponse replyTo) {
        if (message == null) {
            return null;
        }
        return MessageResponse.builder()
                .id(message.getId())
                .conversationId(message.getConversationId())
                .sender(sender)
                .content(message.isDeleted() ? "Tin nhắn đã bị thu hồi" : message.getContent())
                .type(message.getType())
                .replyTo(replyTo)
                .edited(message.isEdited())
                .deleted(message.isDeleted())
                .createdAt(message.getCreatedAt())
                .updatedAt(message.getUpdatedAt())
                .build();
    }
}
