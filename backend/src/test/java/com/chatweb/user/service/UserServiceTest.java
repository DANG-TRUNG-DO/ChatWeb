package com.chatweb.user.service;

import com.chatweb.common.dto.PageResponse;
import com.chatweb.common.exception.ResourceNotFoundException;
import com.chatweb.user.dto.UpdateProfileRequest;
import com.chatweb.user.dto.UserProfileResponse;
import com.chatweb.user.dto.UserSummaryResponse;
import com.chatweb.user.entity.User;
import com.chatweb.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testUser = User.builder()
                .id(testUserId)
                .email("user@example.com")
                .username("testuser")
                .displayName("Original Name")
                .avatarUrl("https://example.com/avatar.png")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("GetCurrentUserProfile: Should return full user profile")
    void getCurrentUserProfile_Success() {
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));

        UserProfileResponse response = userService.getCurrentUserProfile(testUserId);

        assertNotNull(response);
        assertEquals(testUserId, response.getId());
        assertEquals("user@example.com", response.getEmail());
        assertEquals("testuser", response.getUsername());
        assertEquals("Original Name", response.getDisplayName());
        assertEquals("https://example.com/avatar.png", response.getAvatarUrl());
    }

    @Test
    @DisplayName("GetCurrentUserProfile: Should throw ResourceNotFoundException when user does not exist")
    void getCurrentUserProfile_NotFound() {
        UUID unknownId = UUID.randomUUID();
        when(userRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getCurrentUserProfile(unknownId));
    }

    @Test
    @DisplayName("UpdateProfile: Should update displayName and avatarUrl successfully")
    void updateProfile_Success() {
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .displayName("Updated Name")
                .avatarUrl("https://example.com/new-avatar.png")
                .build();

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfileResponse response = userService.updateProfile(testUserId, request);

        assertNotNull(response);
        assertEquals("Updated Name", response.getDisplayName());
        assertEquals("https://example.com/new-avatar.png", response.getAvatarUrl());
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("GetUserById: Should return public user summary")
    void getUserById_Success() {
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));

        UserSummaryResponse response = userService.getUserById(testUserId);

        assertNotNull(response);
        assertEquals(testUserId, response.getId());
        assertEquals("testuser", response.getUsername());
        assertEquals("Original Name", response.getDisplayName());
        assertEquals("https://example.com/avatar.png", response.getAvatarUrl());
    }

    @Test
    @DisplayName("SearchUsers: Should return paginated user summary results")
    void searchUsers_Success() {
        UUID otherUserId = UUID.randomUUID();
        User otherUser = User.builder()
                .id(otherUserId)
                .username("otheruser")
                .displayName("Other User")
                .build();

        when(userRepository.searchUsers(eq("other"), eq(testUserId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(otherUser)));

        PageResponse<UserSummaryResponse> response = userService.searchUsers(testUserId, "other", 0, 10);

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        assertEquals("otheruser", response.getContent().get(0).getUsername());
        assertEquals(1, response.getTotalElements());
    }
}
