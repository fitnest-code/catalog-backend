package az.fitnest.catalog.repository;

import az.fitnest.catalog.model.entity.GymEntranceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GymEntranceHistoryRepository extends JpaRepository<GymEntranceHistory, Long> {
    List<GymEntranceHistory> findByGymIdOrderByScanDateDesc(Long gymId);
    List<GymEntranceHistory> findAllByUserIdOrderByScanDateDesc(Long userId);
    List<GymEntranceHistory> findByGymIdAndScanDateBetweenOrderByScanDateDesc(Long gymId, java.time.LocalDateTime startDate, java.time.LocalDateTime endDate);
}
