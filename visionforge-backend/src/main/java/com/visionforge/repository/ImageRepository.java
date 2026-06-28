package com.visionforge.repository;

import com.visionforge.entity.Image;
import com.visionforge.entity.User;
import com.visionforge.entity.enums.ImageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * ImageRepository — Data Access Layer for the Image entity.
 *
 * Extends JpaRepository, which provides out-of-the-box:
 *   save(), findById(), findAll(), deleteById(), count(), existsById(), etc.
 *
 * All custom methods below use Spring Data's method-name query derivation
 * or explicit @Query (JPQL) for clarity — no native SQL, stays portable.
 *
 * Why no @Transactional here?
 *   Spring Data repositories are transactional by default for all
 *   write operations. Read methods run without a transaction unless
 *   called from within a @Transactional service method (which they are).
 */
@Repository
public interface ImageRepository extends JpaRepository<Image, Long> {

    /**
     * Fetch all images belonging to the given user, ordered newest first.
     *
     * Used by: GET /api/images/my-images
     * Index:   idx_images_user_created (user_id, created_at DESC) — see Image entity.
     *
     * @param user the authenticated user entity
     * @return list of images, newest first; empty list if none exist
     */
    List<Image> findByUserOrderByCreatedAtDesc(User user);

    /**
     * Fetch a specific image that belongs to a specific user.
     *
     * Used by: DELETE /api/images/{id} — verifies ownership before deletion.
     * Combining the ID check + user check in ONE query eliminates a
     * separate "is this user the owner?" round-trip to the database.
     *
     * @param id   the image primary key
     * @param user the authenticated user entity
     * @return Optional.empty() if image doesn't exist OR belongs to another user
     */
    Optional<Image> findByIdAndUser(Long id, User user);

    /**
     * Count how many successful images a user has generated.
     * Useful for analytics, dashboard stats, or rate-limiting checks.
     *
     * @param user   the user entity
     * @param status typically ImageStatus.SUCCESS
     * @return count of matching rows
     */
    long countByUserAndStatus(User user, ImageStatus status);

    /**
     * Check ownership without loading the full entity.
     * Used as a lightweight pre-check before performing expensive operations.
     *
     * @param id   image primary key
     * @param user the user to check ownership for
     * @return true if the image exists and belongs to this user
     */
    boolean existsByIdAndUser(Long id, User user);

    /**
     * Fetch only successful images for a user (excludes PENDING/FAILED).
     *
     * Useful for gallery views where failed generations shouldn't appear.
     *
     * @param user   the authenticated user
     * @param status ImageStatus.SUCCESS
     * @return list of successful images, newest first
     */
    @Query("SELECT i FROM Image i WHERE i.user = :user AND i.status = :status " +
           "ORDER BY i.createdAt DESC")
    List<Image> findByUserAndStatusOrderByCreatedAtDesc(
            @Param("user") User user,
            @Param("status") ImageStatus status);
}
