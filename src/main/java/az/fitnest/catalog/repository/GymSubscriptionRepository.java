package az.fitnest.catalog.repository;

import az.fitnest.catalog.model.entity.GymSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GymSubscriptionRepository extends JpaRepository<GymSubscription, Long> {
    List<GymSubscription> findByGymId(Long gymId);
    void deleteByGymId(Long gymId);
    void deleteByIdAndGymId(Long id, Long gymId);
}
