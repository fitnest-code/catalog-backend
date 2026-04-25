package az.fitnest.catalog.repository;

import az.fitnest.catalog.model.entity.ServiceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServiceTypeRepository extends JpaRepository<ServiceType, Long> {
    List<ServiceType> findAllByOrderByNameAsc();

    boolean existsByNameIgnoreCase(String name);
}
