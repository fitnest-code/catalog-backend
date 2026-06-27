package az.fitnest.catalog.repository;

import az.fitnest.catalog.model.entity.GymDescription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GymDescriptionRepository extends JpaRepository<GymDescription, Long> {
    Optional<GymDescription> findByGymIdAndCategoryId(Long gymId, Long categoryId);
    List<GymDescription> findAllByGymId(Long gymId);
    void deleteAllByGymId(Long gymId);
}
