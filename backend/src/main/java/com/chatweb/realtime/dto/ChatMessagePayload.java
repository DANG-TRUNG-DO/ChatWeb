package com.chatweb.realtime.dto;

import com.chatweb.message.entity.MessageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessagePayload {

    @NotNull(message = "Conversation ID is required")
    private UUID conversationId;

    @NotBlank(message = "Message content must not be blank")
    @Size(max = 5000, message = "Message content must not exceed 5000 characters")
    private String content;

    private MessageType type;

    private UUID replyToId;
}
