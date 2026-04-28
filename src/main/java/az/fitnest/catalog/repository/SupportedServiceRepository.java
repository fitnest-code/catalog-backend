package az.fitnest.catalog.repository;

import az.fitnest.catalog.model.entity.SupportedService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SupportedServiceRepository extends JpaRepository<SupportedService, Long> {
}
