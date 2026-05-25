package az.fitnest.catalog.repository;

import az.fitnest.catalog.model.entity.Translation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TranslationRepository extends JpaRepository<Translation, Long> {
    Optional<Translation> findByEntityTypeAndEntityIdAndLanguageCodeAndFieldName(String entityType, String entityId, String languageCode, String fieldName);

    boolean existsByEntityTypeAndEntityIdAndLanguageCodeAndFieldName(String entityType, String entityId, String languageCode, String fieldName);

    @org.springframework.data.jpa.repository.Query("SELECT t FROM Translation t WHERE " +
            "(:entityType IS NULL OR t.entityType = :entityType) AND " +
            "(:entityId IS NULL OR t.entityId = :entityId) AND " +
            "(:fieldName IS NULL OR t.fieldName = :fieldName) AND " +
            "(:languageCode IS NULL OR LOWER(t.languageCode) = LOWER(:languageCode))")
    java.util.List<Translation> findTranslationsByFilters(
            @org.springframework.data.repository.query.Param("entityType") String entityType,
            @org.springframework.data.repository.query.Param("entityId") String entityId,
            @org.springframework.data.repository.query.Param("fieldName") String fieldName,
            @org.springframework.data.repository.query.Param("languageCode") String languageCode
    );
}
