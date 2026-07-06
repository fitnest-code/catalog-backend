package az.fitnest.catalog.repository;

import az.fitnest.catalog.model.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository
        extends JpaRepository<Review, Long> {
    @Query("SELECT r FROM Review r WHERE r.gym.id = :gymId AND r.status = 'ACCEPTED'")
    public Page<Review> findByGymId(@Param("gymId") Long gymId, Pageable pageable);

    @Query("SELECT new map(AVG(r.rating) as avgRating, COUNT(r) as totalCount) FROM Gym g JOIN g.reviews r WHERE g.id = :gymId AND r.status = 'ACCEPTED'")
    public java.util.Map<String, Object> getRatingAndCountByGymId(@Param("gymId") Long gymId);

    public Page<Review> findByStatus(az.fitnest.catalog.model.enums.ReviewStatus status, Pageable pageable);

    @Query("SELECT r FROM Review r WHERE r.gym.id = :gymId AND (:status IS NULL OR r.status = :status) " +
           "AND (:search IS NULL OR CAST(:search AS string) = '' OR LOWER(r.comment) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
           "OR CAST(r.id AS string) LIKE CONCAT('%', CAST(:search AS string), '%'))")
    Page<Review> findByGymIdAndStatusAndSearch(@Param("gymId") Long gymId,
                                              @Param("status") az.fitnest.catalog.model.enums.ReviewStatus status,
                                              @Param("search") String search,
                                              Pageable pageable);

    @Query("SELECT r FROM Review r WHERE r.gymId = :gymId")
    public Page<Review> findAllByGymId(@Param("gymId") Long gymId, Pageable pageable);

    @Modifying
    @Query("UPDATE Gym g SET g.reviewsCount = COALESCE(g.reviewsCount, 0) + 1, " +
           "g.rating = ((COALESCE(g.rating, 0.0) * COALESCE(g.reviewsCount, 0)) + :newRating) / (COALESCE(g.reviewsCount, 0) + 1.0) " +
           "WHERE g.id = :gymId")
    public void incrementReviewCountAndRating(@Param("gymId") Long gymId, @Param("newRating") Double newRating);

    @Modifying
    @Query("UPDATE Review r SET r.status = :status WHERE r.id = :reviewId")
    public void updateStatus(@Param("reviewId") Long reviewId, @Param("status") az.fitnest.catalog.model.enums.ReviewStatus status);

    void deleteByUserId(Long userId);
}
