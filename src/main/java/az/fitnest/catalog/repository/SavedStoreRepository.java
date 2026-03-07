package az.fitnest.catalog.repository;

import az.fitnest.catalog.model.entity.SavedStore;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SavedStoreRepository
        extends JpaRepository<SavedStore, Long> {
    public Optional<SavedStore> findByUserIdAndStoreId(Long var1, Long var2);

    public boolean existsByUserIdAndStoreId(Long var1, Long var2);

    public List<SavedStore> findByUserId(Long var1);

    public List<SavedStore> findByUserIdAndStoreIdIn(Long var1, List<Long> var2);

    @Query(value = "select ss.store.id from SavedStore ss where ss.userId = :userId and ss.store.id in :storeIds")
    public List<Long> findStoreIdsByUserIdAndStoreIdIn(@Param(value = "userId") Long var1, @Param(value = "storeIds") List<Long> var2);

    public void deleteByUserIdAndStoreId(Long var1, Long var2);

    @Modifying
    @Query(value = "delete from SavedStore ss where ss.store.id = :storeId")
    public void deleteByStoreId(@Param(value = "storeId") Long var1);
}
