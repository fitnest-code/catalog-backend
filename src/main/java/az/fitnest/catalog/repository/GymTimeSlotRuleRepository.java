package az.fitnest.catalog.repository;

import az.fitnest.catalog.model.entity.GymTimeSlotRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface GymTimeSlotRuleRepository extends JpaRepository<GymTimeSlotRule, Long> {

    List<GymTimeSlotRule> findByGymIdOrderByEffectiveFromDescIdAsc(Long gymId);

    java.util.Optional<GymTimeSlotRule> findByIdAndGymId(Long id, Long gymId);

    @Query("SELECT r FROM GymTimeSlotRule r WHERE r.gym.id = :gymId " +
            "AND r.effectiveFrom <= :date AND (r.effectiveTo IS NULL OR r.effectiveTo >= :date) " +
            "ORDER BY r.effectiveFrom DESC, r.id DESC")
    List<GymTimeSlotRule> findEffectiveRules(@Param("gymId") Long gymId, @Param("date") LocalDate date);

    @Query("SELECT r FROM GymTimeSlotRule r WHERE r.gym.id IN :gymIds " +
            "AND r.effectiveFrom <= :date AND (r.effectiveTo IS NULL OR r.effectiveTo >= :date)")
    List<GymTimeSlotRule> findEffectiveRulesForGyms(@Param("gymIds") List<Long> gymIds,
                                                    @Param("date") LocalDate date);
}
