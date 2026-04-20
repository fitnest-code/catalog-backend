package az.fitnest.catalog.repository;

import az.fitnest.catalog.model.entity.Trainer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface TrainerRepository
        extends JpaRepository<Trainer, Long> {
    @Query(value = "SELECT t FROM Gym g JOIN g.trainers t LEFT JOIN FETCH t.profession WHERE g.id = :gymId")
    public Page<Trainer> findByGymId(@Param(value = "gymId") Long var1, Pageable var2);

    @Modifying
    @Transactional
    @Query("UPDATE Trainer t SET t.profession = NULL")
    void clearAllProfessions();

    @Query("""
        SELECT t FROM Trainer t
        WHERE t.gymId = :gymId
          AND (:search IS NULL OR
               LOWER(t.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(t.lastName) LIKE LOWER(CONCAT('%', :search, '%')))
    """)
    Page<Trainer> findAllByGymId(
            @Param("gymId") Long gymId,
            @Param("search") String search,
            Pageable pageable
    );
}
