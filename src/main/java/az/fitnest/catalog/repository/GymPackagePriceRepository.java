package az.fitnest.catalog.repository;

import az.fitnest.catalog.model.entity.GymPackagePrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface GymPackagePriceRepository extends JpaRepository<GymPackagePrice, Long> {

    List<GymPackagePrice> findByGymIdOrderByEffectiveFromDescPackageIdAsc(Long gymId);

    List<GymPackagePrice> findByGymIdAndEffectiveFromLessThanEqualOrderByEffectiveFromDescPackageIdAsc(
            Long gymId, LocalDate date);

    Optional<GymPackagePrice> findFirstByGymIdAndPackageIdAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
            Long gymId, Long packageId, LocalDate date);

    Optional<GymPackagePrice> findFirstByGymIdAndPackageIdAndEffectiveFromLessThanOrderByEffectiveFromDesc(
            Long gymId, Long packageId, LocalDate date);

    Optional<GymPackagePrice> findByGymIdAndPackageIdAndEffectiveFrom(
            Long gymId, Long packageId, LocalDate effectiveFrom);

    /**
     * One row per gym/package holding the newest price version effective on {@code date}.
     */
    @Query("SELECT p FROM GymPackagePrice p " +
            "WHERE p.gym.id IN :gymIds AND p.effectiveFrom <= :date " +
            "AND p.effectiveFrom = (SELECT MAX(p2.effectiveFrom) FROM GymPackagePrice p2 " +
            "WHERE p2.gym.id = p.gym.id AND p2.packageId = p.packageId AND p2.effectiveFrom <= :date)")
    List<GymPackagePrice> findLatestByGymIdIn(@Param("gymIds") Collection<Long> gymIds,
                                              @Param("date") LocalDate date);
}
