package az.fitnest.catalog.repository;

import az.fitnest.catalog.model.entity.GymLessonType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GymLessonTypeRepository extends JpaRepository<GymLessonType, Long> {

    List<GymLessonType> findByGymId(Long gymId);

    Optional<GymLessonType> findByGymIdAndName(Long gymId, String name);

    boolean existsByGymIdAndName(Long gymId, String name);
}
