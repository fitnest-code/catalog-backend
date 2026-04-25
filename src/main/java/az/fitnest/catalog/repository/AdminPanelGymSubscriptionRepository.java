package az.fitnest.catalog.repository;

import az.fitnest.catalog.model.entity.AdminPanelGymSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdminPanelGymSubscriptionRepository extends JpaRepository<AdminPanelGymSubscription, Long> {

    List<AdminPanelGymSubscription> findAllByGymId(Long gymId);

    void deleteAllByGymId(Long gymId);
}
