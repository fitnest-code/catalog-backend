package az.fitnest.catalog.repository;

import az.fitnest.catalog.model.entity.SupportedService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SupportedServiceRepository extends JpaRepository<SupportedService, Long> {
    java.util.List<SupportedService> findAllByGymId(Long gymId);
    java.util.Optional<SupportedService> findByNameIgnoreCase(String name);
}
