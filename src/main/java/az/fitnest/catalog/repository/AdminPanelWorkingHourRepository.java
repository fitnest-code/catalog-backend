package az.fitnest.catalog.repository;

import az.fitnest.catalog.model.entity.AdminPanelWorkingHour;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AdminPanelWorkingHourRepository extends JpaRepository<AdminPanelWorkingHour, Long> {

    List<AdminPanelWorkingHour> findAllByGymIdOrderByDayOfWeekAsc(Long gymId);

    boolean existsByGymIdAndDayOfWeek(Long gymId, Integer dayOfWeek);

    Optional<AdminPanelWorkingHour> findByIdAndGymId(Long id, Long gymId);
}