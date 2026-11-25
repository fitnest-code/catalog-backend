package az.fitnest.catalog.repository;

import az.fitnest.catalog.model.entity.GymCategoryWorkHour;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GymCategoryWorkHourRepository extends JpaRepository<GymCategoryWorkHour, Long> {

    List<GymCategoryWorkHour> findByGymIdAndCategoryIdIn(Long gymId, Collection<Long> categoryIds);

    void deleteByGymIdAndCategoryId(Long gymId, Long categoryId);
}
