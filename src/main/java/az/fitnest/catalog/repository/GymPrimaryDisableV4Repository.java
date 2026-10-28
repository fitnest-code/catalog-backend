package az.fitnest.catalog.repository;

import az.fitnest.catalog.model.entity.GymPrimaryDisableV4;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GymPrimaryDisableV4Repository extends JpaRepository<GymPrimaryDisableV4, Long> {
  List<GymPrimaryDisableV4> findByGymIdAndPackageIdOrderByDisabledAtAsc(Long gymId, Long packageId);
}
