package az.fitnest.catalog.repository;

import az.fitnest.catalog.model.entity.GymServiceItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GymServiceItemRepository extends JpaRepository<GymServiceItem, Long> {
    List<GymServiceItem> findAllByGymId(Long gymId);

    void deleteAllByGymId(Long gymId);
}