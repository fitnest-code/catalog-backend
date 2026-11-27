package az.fitnest.catalog.repository;

import az.fitnest.catalog.model.entity.GymCategoryServiceLink;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GymCategoryServiceLinkRepository
        extends JpaRepository<GymCategoryServiceLink, GymCategoryServiceLink.Pk> {

    List<GymCategoryServiceLink> findByGymIdAndCategoryIdIn(Long gymId, Collection<Long> categoryIds);

    void deleteByGymIdAndCategoryId(Long gymId, Long categoryId);
}
