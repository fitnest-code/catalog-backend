package az.fitnest.catalog.repository;

import az.fitnest.catalog.model.entity.GymAdmin;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface GymAdminRepository extends JpaRepository<GymAdmin, Long> {
    Optional<GymAdmin> findFirstByGymId(Long gymId);
    List<GymAdmin> findByGymId(Long gymId);
    void deleteAllByGymId(Long gymId);
}
