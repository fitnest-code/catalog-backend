package az.fitnest.catalog.repository;

import az.fitnest.catalog.model.entity.GymAdmin;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GymAdminRepository extends JpaRepository<GymAdmin, Long> {
}
