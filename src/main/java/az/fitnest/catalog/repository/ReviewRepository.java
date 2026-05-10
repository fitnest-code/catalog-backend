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
    @Query(value = "SELECT r FROM Gym g JOIN g.reviews r WHERE g.id = :gymId AND r.status = 'ACCEPTED'")
    public Page<Review> findByGymId(@Param(value = "gymId") Long var1, Pageable var2);

    @Query("SELECT new map(AVG(r.rating) as avgRating, COUNT(r) as totalCount) FROM Gym g JOIN g.reviews r WHERE g.id = :gymId AND r.status = 'ACCEPTED'")
    public java.util.Map<String, Object> getRatingAndCountByGymId(@Param("gymId") Long gymId);

    public Page<Review> findByStatus(az.fitnest.catalog.model.enums.ReviewStatus status, Pageable pageable);

    @Query("SELECT r FROM Review r WHERE r.gym.id = :gymId AND (:status IS NULL OR r.status = :status) " +
           "AND (:search IS NULL OR :search = '' OR LOWER(r.comment) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR CAST(r.id AS string) LIKE CONCAT('%', :search, '%'))")
    Page<Review> findByGymIdAndStatusAndSearch(@Param("gymId") Long gymId, 
                                              @Param("status") az.fitnest.catalog.model.enums.ReviewStatus status, 
                                              @Param("search") String search, 
                                              Pageable pageable);

    @Query("SELECT r FROM Review r WHERE r.gymId = :gymId")
    public Page<Review> findAllByGymId(@Param("gymId") Long gymId, Pageable pageable);

    @Modifying
    @Query("UPDATE Gym g SET g.reviewsCount = g.reviewsCount + 1, g.rating = ((g.rating * g.reviewsCount) + :newRating) / CAST((g.reviewsCount + 1) AS double) WHERE g.id = :gymId")
    public void incrementReviewCountAndRating(@Param("gymId") Long gymId, @Param("newRating") Double newRating);
}
