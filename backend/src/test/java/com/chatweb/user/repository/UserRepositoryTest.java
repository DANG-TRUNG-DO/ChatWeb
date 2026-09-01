package com.chatweb.user.repository;

import com.chatweb.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("dev")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private User currentUser;
    private User userAlice;
    private User userBob;
    private User userCharlie;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        currentUser = userRepository.save(User.builder()
                .email("me@example.com")
                .username("myusername")
                .displayName("My Display Name")
                .passwordHash("hashed")
                .build());

        userAlice = userRepository.save(User.builder()
                .email("alice@example.com")
                .username("alice_wonder")
                .displayName("Alice In Chains")
                .passwordHash("hashed")
                .build());

        userBob = userRepository.save(User.builder()
                .email("bob@example.com")
                .username("bobby_tables")
                .displayName("Robert Tables")
                .passwordHash("hashed")
                .build());

        userCharlie = userRepository.save(User.builder()
                .email("charlie@example.com")
                .username("charlie_brown")
                .displayName("Chuck")
                .passwordHash("hashed")
                .build());
    }

    @Test
    @DisplayName("Should find users by username case-insensitively and exclude current user")
    void searchUsers_ByUsername() {
        Page<User> result = userRepository.searchUsers("ALICE", currentUser.getId(), PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals("alice_wonder", result.getContent().get(0).getUsername());
    }

    @Test
    @DisplayName("Should find users by displayName case-insensitively and exclude current user")
    void searchUsers_ByDisplayName() {
        Page<User> result = userRepository.searchUsers("robert", currentUser.getId(), PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals("bobby_tables", result.getContent().get(0).getUsername());
    }

    @Test
    @DisplayName("Should not return current user even if current user matches query")
    void searchUsers_ExcludesCurrentUser() {
        Page<User> result = userRepository.searchUsers("myusername", currentUser.getId(), PageRequest.of(0, 10));

        assertEquals(0, result.getTotalElements());
    }

    @Test
    @DisplayName("Should return all other users when query is empty and support pagination")
    void searchUsers_EmptyQueryWithPagination() {
        Page<User> result = userRepository.searchUsers("", currentUser.getId(), PageRequest.of(0, 2));

        assertEquals(3, result.getTotalElements()); // Alice, Bob, Charlie (excluding current user)
        assertEquals(2, result.getContent().size()); // page size = 2
        assertEquals(2, result.getTotalPages());
    }
}
