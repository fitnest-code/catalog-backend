package az.fitnest.catalog.repository;

import az.fitnest.catalog.model.entity.GymCategory;
import az.fitnest.catalog.model.entity.GymCategoryId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author: nijataghayev
 */

@Repository
public interface GymCategoryRepository extends JpaRepository<GymCategory, GymCategoryId> {

    List<GymCategory> findByGymId(Long gymId);

    @Modifying
    @Query("DELETE FROM GymCategory gc WHERE gc.gym.id = :gymId")
    void deleteAllByGymId(@Param("gymId") Long gymId);
}