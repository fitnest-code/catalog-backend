package az.fitnest.catalog.repository;

import az.fitnest.catalog.model.entity.SupportedService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SupportedServiceRepository extends JpaRepository<SupportedService, Long> {
    java.util.List<SupportedService> findAllByGymId(Long gymId);
    java.util.List<SupportedService> findAllByGymIdIsNull();
    void deleteAllByGymId(Long gymId);
    @org.springframework.data.jpa.repository.Query("SELECT s FROM SupportedService s WHERE LOWER(s.name) = LOWER(:name) AND (:gymId IS NULL AND s.gymId IS NULL OR s.gymId = :gymId)")
    java.util.Optional<SupportedService> findByNameIgnoreCaseAndGymId(@org.springframework.data.repository.query.Param("name") String name, @org.springframework.data.repository.query.Param("gymId") Long gymId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query(value = "DELETE FROM gym_subscription_services WHERE supported_service_id = :serviceId", nativeQuery = true)
    void deleteSubscriptionAssociations(@org.springframework.data.repository.query.Param("serviceId") Long serviceId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query(value = "DELETE FROM gym_subscription_services WHERE gym_subscription_id IN (SELECT id FROM gym_subscriptions WHERE gym_id = :gymId)", nativeQuery = true)
    void deleteSubscriptionAssociationsByGymId(@org.springframework.data.repository.query.Param("gymId") Long gymId);
}
