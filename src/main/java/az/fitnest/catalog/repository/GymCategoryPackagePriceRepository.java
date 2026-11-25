package az.fitnest.catalog.repository;

import az.fitnest.catalog.model.entity.GymCategoryPackagePrice;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GymCategoryPackagePriceRepository extends JpaRepository<GymCategoryPackagePrice, Long> {

    Optional<GymCategoryPackagePrice>
            findFirstByGymIdAndCategoryIdAndPackageIdAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
                    Long gymId, Long categoryId, Long packageId, LocalDate date);

    List<GymCategoryPackagePrice> findByGymIdAndCategoryIdOrderByPackageIdAscEffectiveFromDesc(
            Long gymId, Long categoryId);

    @Query(
            "SELECT p FROM GymCategoryPackagePrice p WHERE p.gym.id = :gymId AND p.categoryId IN :categoryIds "
                    + "AND p.effectiveFrom <= :date AND p.effectiveFrom = ("
                    + "SELECT MAX(p2.effectiveFrom) FROM GymCategoryPackagePrice p2 "
                    + "WHERE p2.gym.id = p.gym.id AND p2.categoryId = p.categoryId "
                    + "AND p2.packageId = p.packageId AND p2.effectiveFrom <= :date)")
    List<GymCategoryPackagePrice> findLatestByGymAndCategories(
            @Param("gymId") Long gymId,
            @Param("categoryIds") Collection<Long> categoryIds,
            @Param("date") LocalDate date);

    void deleteByGymIdAndCategoryId(Long gymId, Long categoryId);
}
