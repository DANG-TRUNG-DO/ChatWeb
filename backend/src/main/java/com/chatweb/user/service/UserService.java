package com.chatweb.user.service;

import com.chatweb.common.dto.PageResponse;
import com.chatweb.user.dto.UpdateProfileRequest;
import com.chatweb.user.dto.UserProfileResponse;
import com.chatweb.user.dto.UserSummaryResponse;

import java.util.UUID;

public interface UserService {

    /**
     * Retrieves full profile information for the authenticated user.
     *
     * @param currentUserId the ID of the current authenticated user
     * @return UserProfileResponse containing full profile details
     */
    UserProfileResponse getCurrentUserProfile(UUID currentUserId);

    /**
     * Updates profile details (display name, avatar URL) for the authenticated user.
     *
     * @param currentUserId the ID of the current authenticated user
     * @param request the update profile request
     * @return UserProfileResponse containing updated profile details
     */
    UserProfileResponse updateProfile(UUID currentUserId, UpdateProfileRequest request);

    /**
     * Retrieves public summary information of a user by their ID.
     *
     * @param targetUserId the ID of the user to view
     * @return UserSummaryResponse containing public user information
     */
    UserSummaryResponse getUserById(UUID targetUserId);

    /**
     * Searches for users by username or display name with pagination, excluding the current user.
     *
     * @param currentUserId the ID of the current user to exclude from results
     * @param query the search keyword
     * @param page page number (0-indexed)
     * @param size page size
     * @return PageResponse containing list of UserSummaryResponse
     */
    PageResponse<UserSummaryResponse> searchUsers(UUID currentUserId, String query, int page, int size);
}
