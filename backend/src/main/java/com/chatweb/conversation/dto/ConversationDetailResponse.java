package com.chatweb.conversation.dto;

import com.chatweb.conversation.entity.Conversation;
import com.chatweb.conversation.entity.ConversationType;
import com.chatweb.user.dto.UserSummaryResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationDetailResponse {

    private UUID id;
    private ConversationType type;
    private String name;
    private String avatarUrl;
    private UserSummaryResponse partner;
    private List<ConversationMemberResponse> members;
    private Instant createdAt;
    private Instant updatedAt;

    public static ConversationDetailResponse fromEntity(
            Conversation conversation,
            UserSummaryResponse partner,
            List<ConversationMemberResponse> members
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

        return ConversationDetailResponse.builder()
                .id(conversation.getId())
                .type(conversation.getType())
                .name(displayName)
                .avatarUrl(displayAvatar)
                .partner(partner)
                .members(members)
                .createdAt(conversation.getCreatedAt())
                .updatedAt(conversation.getUpdatedAt())
                .build();
    }
}
