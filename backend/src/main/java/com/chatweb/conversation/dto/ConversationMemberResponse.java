package com.chatweb.conversation.dto;

import com.chatweb.conversation.entity.ConversationMember;
import com.chatweb.conversation.entity.MemberRole;
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
public class ConversationMemberResponse {

    private UUID id;
    private UUID userId;
    private String username;
    private String displayName;
    private String avatarUrl;
    private MemberRole role;
    private UUID lastReadMessageId;
    private Instant joinedAt;

    public static ConversationMemberResponse fromEntity(ConversationMember member, UserSummaryResponse userSummary) {
        if (member == null) {
            return null;
        }
        return ConversationMemberResponse.builder()
                .id(member.getId())
                .userId(member.getUserId())
                .username(userSummary != null ? userSummary.getUsername() : null)
                .displayName(userSummary != null ? userSummary.getDisplayName() : null)
                .avatarUrl(userSummary != null ? userSummary.getAvatarUrl() : null)
                .role(member.getRole())
                .lastReadMessageId(member.getLastReadMessageId())
                .joinedAt(member.getJoinedAt())
                .build();
    }
}
