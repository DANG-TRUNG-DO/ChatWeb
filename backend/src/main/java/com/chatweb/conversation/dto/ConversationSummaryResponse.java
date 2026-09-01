package com.chatweb.conversation.dto;

import com.chatweb.conversation.entity.Conversation;
import com.chatweb.conversation.entity.ConversationType;
import com.chatweb.message.dto.MessageSummaryResponse;
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
public class ConversationSummaryResponse {

    private UUID id;
    private ConversationType type;
    private String name;
    private String avatarUrl;
    private UserSummaryResponse partner;
    private MessageSummaryResponse lastMessage;
    private long unreadCount;
    private Instant createdAt;
    private Instant updatedAt;

    public static ConversationSummaryResponse fromEntity(
            Conversation conversation,
            UserSummaryResponse partner,
            MessageSummaryResponse lastMessage,
            long unreadCount
    ) {
        if (conversation == null) {
            return null;
        }

        String displayName = conversation.getName();
        String displayAvatar = conversation.getAvatarUrl();

        if (conversation.getType() == ConversationType.DIRECT && partner != null) {
            displayName = partner.getDisplayName() != null ? partner.getDisplayName() : partner.getUsername();
            displayAvatar = partner.getAvatarUrl();
        }

        return ConversationSummaryResponse.builder()
                .id(conversation.getId())
                .type(conversation.getType())
                .name(displayName)
                .avatarUrl(displayAvatar)
                .partner(partner)
                .lastMessage(lastMessage)
                .unreadCount(unreadCount)
                .createdAt(conversation.getCreatedAt())
                .updatedAt(conversation.getUpdatedAt())
                .build();
    }
}
