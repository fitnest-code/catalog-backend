package az.fitnest.catalog.repository;

import az.fitnest.catalog.model.entity.GymPackagePrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface GymPackagePriceRepository extends JpaRepository<GymPackagePrice, Long> {

    List<GymPackagePrice> findByGymIdOrderByEffectiveFromDescPackageIdAsc(Long gymId);

    List<GymPackagePrice> findByGymIdAndEffectiveFromLessThanEqualOrderByEffectiveFromDescPackageIdAsc(
            Long gymId, LocalDate date);

    Optional<GymPackagePrice> findFirstByGymIdAndPackageIdAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
            Long gymId, Long packageId, LocalDate date);

    Optional<GymPackagePrice> findByGymIdAndPackageIdAndEffectiveFrom(
            Long gymId, Long packageId, LocalDate effectiveFrom);
}
