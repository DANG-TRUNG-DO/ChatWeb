package com.chatweb.user.repository;

import com.chatweb.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    @Query("SELECT u FROM User u WHERE u.email = :identifier OR u.username = :identifier")
    Optional<User> findByEmailOrUsername(@Param("identifier") String identifier);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    @Query("SELECT u FROM User u WHERE " +
           "(:query IS NULL OR :query = '' OR " +
           " LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " (u.displayName IS NOT NULL AND LOWER(u.displayName) LIKE LOWER(CONCAT('%', :query, '%')))) AND " +
           "u.id != :currentUserId")
    Page<User> searchUsers(
            @Param("query") String query,
            @Param("currentUserId") UUID currentUserId,
            Pageable pageable
    );
}
