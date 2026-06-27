package com.visionforge.repository;

import com.visionforge.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * UserRepository — Database access layer for User entity.
 *
 * Spring Data JPA auto-generates SQL from method names at runtime.
 * No SQL is written manually here.
 *
 * JpaRepository<User, Long> provides out of the box:
 *   save(), findById(), findAll(), delete(), count(), existsById() etc.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * SELECT * FROM users WHERE email = ?
     * Returns Optional to force null-safe handling at call sites.
     */
    Optional<User> findByEmail(String email);

    /**
     * SELECT * FROM users WHERE username = ?
     */
    Optional<User> findByUsername(String username);

    /**
     * SELECT EXISTS(SELECT 1 FROM users WHERE email = ?)
     * Faster than loading the full entity — used for duplicate checks.
     */
    boolean existsByEmail(String email);

    /**
     * SELECT EXISTS(SELECT 1 FROM users WHERE username = ?)
     */
    boolean existsByUsername(String username);

    /**
     * Deducts credits atomically without loading the full entity.
     * Condition: only deducts if user has enough credits.
     * Returns 1 on success, 0 if insufficient credits.
     *
     * @Modifying: required for UPDATE/DELETE/INSERT queries
     * @Transactional: must be set on the calling service method
     */
    @Modifying
    @Query("UPDATE User u SET u.credits = u.credits - :amount " +
           "WHERE u.id = :userId AND u.credits >= :amount")
    int deductCredits(@Param("userId") Long userId, @Param("amount") int amount);
}
