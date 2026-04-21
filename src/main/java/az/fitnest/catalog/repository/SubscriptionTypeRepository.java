package az.fitnest.catalog.repository;

import az.fitnest.catalog.model.entity.SubscriptionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubscriptionTypeRepository extends JpaRepository<SubscriptionType, Long> {
    List<SubscriptionType> findAllByOrderByNameAsc();
}
