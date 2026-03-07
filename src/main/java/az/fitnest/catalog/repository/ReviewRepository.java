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
    @Query(value = "SELECT r FROM Gym g JOIN g.reviews r WHERE g.id = :gymId")
    public Page<Review> findByGymId(@Param(value = "gymId") Long var1, Pageable var2);

    @Query("SELECT new map(AVG(r.rating) as avgRating, COUNT(r) as totalCount) FROM Gym g JOIN g.reviews r WHERE g.id = :gymId")
    public java.util.Map<String, Object> getRatingAndCountByGymId(@Param("gymId") Long gymId);

    @Modifying
    @Query("UPDATE Gym g SET g.reviewsCount = g.reviewsCount + 1, g.rating = ((g.rating * g.reviewsCount) + :newRating) / CAST((g.reviewsCount + 1) AS double) WHERE g.id = :gymId")
    public void incrementReviewCountAndRating(@Param("gymId") Long gymId, @Param("newRating") Double newRating);
}
