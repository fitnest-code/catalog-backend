package az.fitnest.catalog.repository;

import az.fitnest.catalog.model.entity.ReservationRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReservationRuleRepository extends JpaRepository<ReservationRule, Long> {
    List<ReservationRule> findByGymIdAndCategoryIdAndStatus(Long gymId, Long categoryId, String status);
    List<ReservationRule> findByGymIdAndStatusOrderBySortOrderAsc(Long gymId, String status);
}
