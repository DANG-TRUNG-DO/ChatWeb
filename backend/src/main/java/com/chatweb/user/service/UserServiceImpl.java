package com.chatweb.user.service;

import com.chatweb.common.dto.PageResponse;
import com.chatweb.common.exception.ResourceNotFoundException;
import com.chatweb.user.dto.UpdateProfileRequest;
import com.chatweb.user.dto.UserProfileResponse;
import com.chatweb.user.dto.UserSummaryResponse;
import com.chatweb.user.entity.User;
import com.chatweb.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUserProfile(UUID currentUserId) {
        User user = findUserById(currentUserId);
        return UserProfileResponse.fromEntity(user);
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(UUID currentUserId, UpdateProfileRequest request) {
        User user = findUserById(currentUserId);

        if (request.getDisplayName() != null) {
            String trimmedName = request.getDisplayName().trim();
            user.setDisplayName(trimmedName.isEmpty() ? user.getUsername() : trimmedName);
        }

        if (request.getAvatarUrl() != null) {
            String trimmedAvatar = request.getAvatarUrl().trim();
            user.setAvatarUrl(trimmedAvatar.isEmpty() ? null : trimmedAvatar);
        }

        User updatedUser = userRepository.save(user);
        log.info("Updated profile for user id: {}, username: {}", updatedUser.getId(), updatedUser.getUsername());

        return UserProfileResponse.fromEntity(updatedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public UserSummaryResponse getUserById(UUID targetUserId) {
        User user = findUserById(targetUserId);
        return UserSummaryResponse.fromEntity(user);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserSummaryResponse> searchUsers(UUID currentUserId, String query, int page, int size) {
        int pageNo = Math.max(0, page);
        int pageSize = Math.min(Math.max(1, size), 50);
        String sanitizedQuery = (query != null) ? query.trim() : "";

        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by("username").ascending());
        Page<User> userPage = userRepository.searchUsers(sanitizedQuery, currentUserId, pageable);

        List<UserSummaryResponse> content = userPage.getContent()
                .stream()
                .map(UserSummaryResponse::fromEntity)
                .toList();

        return PageResponse.of(content, userPage);
    }

    private User findUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }
}
