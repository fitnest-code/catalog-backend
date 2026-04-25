package az.fitnest.catalog.repository;

import az.fitnest.catalog.model.entity.GymRating;
import az.fitnest.catalog.model.enums.RatingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface GymRatingRepository extends JpaRepository<GymRating, Long> {

    @Query("""
        SELECT r FROM GymRating r
        WHERE r.gymId = :gymId
          AND (:status IS NULL OR r.status = :status)
          AND (:search IS NULL OR
               LOWER(r.customerFullName) LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(r.comment) LIKE LOWER(CONCAT('%', :search, '%')))
    """)
    Page<GymRating> findAllByGymId(
            @Param("gymId") Long gymId,
            @Param("search") String search,
            @Param("status") RatingStatus status,
            Pageable pageable
    );

    Optional<GymRating> findByIdAndGymId(Long id, Long gymId);
}