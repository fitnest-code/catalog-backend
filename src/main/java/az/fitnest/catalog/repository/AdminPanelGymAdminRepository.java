package az.fitnest.catalog.repository;

import az.fitnest.catalog.model.entity.AdminPanelGymAdmin;
import az.fitnest.catalog.model.enums.GymAdminRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AdminPanelGymAdminRepository extends JpaRepository<AdminPanelGymAdmin, Long> {

    List<AdminPanelGymAdmin> findAllByGymId(Long gymId);

    Optional<AdminPanelGymAdmin> findByIdAndGymId(Long id, Long gymId);

    boolean existsByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);

    long countByGymId(Long gymId);

    boolean existsByGymIdAndRole(Long gymId, GymAdminRole role);
}
