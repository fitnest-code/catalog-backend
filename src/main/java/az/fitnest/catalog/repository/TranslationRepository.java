package az.fitnest.catalog.repository;

import az.fitnest.catalog.model.entity.Translation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TranslationRepository extends JpaRepository<Translation, Long> {
    Optional<Translation> findByEntityTypeAndEntityIdAndLanguageCodeAndFieldName(String entityType, String entityId, String languageCode, String fieldName);

    boolean existsByEntityTypeAndEntityIdAndLanguageCodeAndFieldName(String entityType, String entityId, String languageCode, String fieldName);

    List<Translation> findByEntityTypeAndEntityIdInAndLanguageCode(String entityType, java.util.Collection<String> entityIds, String languageCode);

    @Modifying
    @Query("DELETE FROM Translation t WHERE t.entityType = :entityType AND t.entityId = :entityId")
    void deleteByEntityTypeAndEntityId(@Param("entityType") String entityType, @Param("entityId") String entityId);
}
