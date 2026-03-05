package az.fitnest.catalog.repository;

import az.fitnest.catalog.model.entity.RecentSearch;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecentSearchRepository extends JpaRepository<RecentSearch, Long> {
    
    Optional<RecentSearch> findByUserIdAndTypeAndQueryIgnoreCase(Long userId, String type, String query);
    
    List<RecentSearch> findByUserIdAndTypeOrderByCreatedDateDesc(Long userId, String type, Pageable pageable);

    org.springframework.data.domain.Page<RecentSearch> findAllByUserIdAndType(Long userId, String type, Pageable pageable);
    
    long countByUserIdAndType(Long userId, String type);
    
    void deleteByUserIdAndTypeAndQueryIgnoreCase(Long userId, String type, String query);

    void deleteByUserIdAndType(Long userId, String type);
}
