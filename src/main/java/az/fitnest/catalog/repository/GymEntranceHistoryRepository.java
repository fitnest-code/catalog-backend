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
    void deleteByGymId(Long gymId);
    void deleteByUserId(Long userId);
    boolean existsByGymId(Long gymId);
    boolean existsByUserIdAndGymIdAndStatusInAndScanDateBetween(
            Long userId, Long gymId, List<String> statuses, java.time.LocalDateTime start, java.time.LocalDateTime end);

    long countByStatusInAndScanDateBetween(java.util.List<String> statuses, java.time.LocalDateTime start, java.time.LocalDateTime end);

    @org.springframework.data.jpa.repository.Query("SELECT h.gymId, h.packageId, COUNT(h.id), SUM(h.amount) " +
            "FROM GymEntranceHistory h " +
            "WHERE h.status IN ('ELIGIBLE', 'Uğurlu') AND h.scanDate BETWEEN :start AND :end " +
            "GROUP BY h.gymId, h.packageId")
    List<Object[]> getGymPaymentsReport(@org.springframework.data.repository.query.Param("start") java.time.LocalDateTime start, @org.springframework.data.repository.query.Param("end") java.time.LocalDateTime end);

    void deleteByGymIdAndPackageId(Long gymId, Long packageId);
    void deleteByGymIdAndPackageIdIsNull(Long gymId);
}
