package com.chatweb.realtime.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageReadPayload {

    @NotNull(message = "Conversation ID is required")
    private UUID conversationId;

    private UUID messageId;

    private UUID userId;
}
