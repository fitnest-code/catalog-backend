package az.fitnest.catalog.service.impl;

import az.fitnest.catalog.model.entity.Translation;
import az.fitnest.catalog.dto.request.CreateTranslationRequest;
import az.fitnest.catalog.exception.ConflictException;
import az.fitnest.catalog.exception.ResourceNotFoundException;
import az.fitnest.catalog.repository.TranslationRepository;
import az.fitnest.catalog.service.TranslationService;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class TranslationServiceImpl implements TranslationService {

    /** Response caches that embed translated strings — clear with every translation write. */
    private static final String[] RESPONSE_CACHES = {
            "translations",
            "gym-detail",
            "gymDetails",
            "main-page-gyms",
            "gym-listings",
            "admin-gyms",
            "landing-gyms",
            "landing-home-gyms",
            "landing-gym-detail",
            "landing-gym-filters",
            "landing-gyms-v2",
            "landing-gym-detail-v2",
            "landing-gym-filters-v2"
    };

    private final TranslationRepository translationRepository;
    private final CacheManager cacheManager;

    public TranslationServiceImpl(TranslationRepository translationRepository, CacheManager cacheManager) {
        this.translationRepository = translationRepository;
        this.cacheManager = cacheManager;
    }

    @Override
    // Key is uppercased so "ru"/"RU" share one entry. Nulls are not cached — a miss before admin
    // adds EN/RU must not stick forever after they save.
    @org.springframework.cache.annotation.Cacheable(value = "translations",
            key = "(#entityType == null ? '' : #entityType.toUpperCase()) + '_' + #entityId + '_' + #fieldName + '_' + (#languageCode == null ? '' : #languageCode.toUpperCase())",
            unless = "#result == null")
    public String getTranslatedValue(String entityType, String entityId, String fieldName, String languageCode) {
        if (languageCode == null || languageCode.equalsIgnoreCase("AZ")) {
            return null;
        }
        // Gym/store names are one shared string on the entity row (same in AZ/EN/RU).
        if (isSharedNameField(entityType, fieldName)) {
            return null;
        }

        if (entityType != null) {
            String normType = entityType.toUpperCase();
            if (normType.equals("GYM_STATUS") || normType.equals("GYMSTATUS") || normType.equals("STORE_STATUS") || normType.equals("STORESTATUS")) {
                String status = entityId.toUpperCase();
                if (languageCode.equalsIgnoreCase("EN")) {
                    switch (status) {
                        case "ACTIVE": return "Active";
                        case "INACTIVE": return "Inactive";
                        case "DRAFT": return "Draft";
                        default: return entityId;
                    }
                } else if (languageCode.equalsIgnoreCase("RU")) {
                    switch (status) {
                        case "ACTIVE": return "Активный";
                        case "INACTIVE": return "Неактивный";
                        case "DRAFT": return "Черновик";
                        default: return entityId;
                    }
                }
                return entityId;
            } else if (normType.equals("RESERVATION_STATUS") || normType.equals("RESERVATIONSTATUS")) {
                String status = entityId.toUpperCase();
                if (languageCode.equalsIgnoreCase("EN")) {
                    switch (status) {
                        case "PENDING": return "Pending";
                        case "APPROVED": return "Approved";
                        case "CANCELLED": return "Cancelled";
                        case "REJECTED": return "Rejected";
                        case "EXPIRED": return "Expired";
                        default: return entityId;
                    }
                } else if (languageCode.equalsIgnoreCase("RU")) {
                    switch (status) {
                        case "PENDING": return "В ожидании";
                        case "APPROVED": return "Одобрено";
                        case "CANCELLED": return "Отменено";
                        case "REJECTED": return "Отклонено";
                        case "EXPIRED": return "Истек";
                        default: return entityId;
                    }
                }
                return entityId;
            } else if (normType.equals("REVIEW_STATUS") || normType.equals("REVIEWSTATUS")) {
                String status = entityId.toUpperCase();
                if (languageCode.equalsIgnoreCase("EN")) {
                    switch (status) {
                        case "PENDING": return "Pending";
                        case "ACCEPTED": return "Accepted";
                        case "REJECTED": return "Rejected";
                        default: return entityId;
                    }
                } else if (languageCode.equalsIgnoreCase("RU")) {
                    switch (status) {
                        case "PENDING": return "В ожидании";
                        case "ACCEPTED": return "Принято";
                        case "REJECTED": return "Отклонено";
                        default: return entityId;
                    }
                }
                return entityId;
            } else if (normType.equals("SESSION_STATUS") || normType.equals("SESSIONSTATUS")) {
                String status = entityId.toUpperCase();
                if (languageCode.equalsIgnoreCase("EN")) {
                    switch (status) {
                        case "ACTIVE": return "Active";
                        case "INACTIVE": return "Inactive";
                        default: return entityId;
                    }
                } else if (languageCode.equalsIgnoreCase("RU")) {
                    switch (status) {
                        case "ACTIVE": return "Активный";
                        case "INACTIVE": return "Неактивный";
                        default: return entityId;
                    }
                }
                return entityId;
            } else if (normType.equals("ENTRANCE_STATUS")) {
                String status = entityId;
                if (languageCode.equalsIgnoreCase("EN")) {
                    if ("Uğurlu".equalsIgnoreCase(status) || "ELIGIBLE".equalsIgnoreCase(status)) return "Successful";
                    if ("Uğursuz".equalsIgnoreCase(status) || "INELIGIBLE".equalsIgnoreCase(status)) return "Unsuccessful";
                    return status;
                } else if (languageCode.equalsIgnoreCase("RU")) {
                    if ("Uğurlu".equalsIgnoreCase(status) || "ELIGIBLE".equalsIgnoreCase(status)) return "Успешно";
                    if ("Uğursuz".equalsIgnoreCase(status) || "INELIGIBLE".equalsIgnoreCase(status)) return "Неуспешно";
                    return status;
                }
                return status;
            }
        }

        return translationRepository
                .findByEntityTypeAndEntityIdAndLanguageCodeAndFieldName(
                        entityType == null ? null : entityType.toUpperCase(),
                        entityId,
                        languageCode.toUpperCase(),
                        fieldName)
                .map(Translation::getFieldValue)
                .orElse(null);
    }

    @Override
    public Translation createTranslation(CreateTranslationRequest request) {
        if (isSharedNameField(request.entityType(), request.fieldName())) {
            // Gym/store name lives on the entity; do not store per-language name rows.
            return null;
        }
        String normalizedEntityType = request.entityType().toUpperCase();
        Translation existing = translationRepository.findByEntityTypeAndEntityIdAndLanguageCodeAndFieldName(
                normalizedEntityType, request.entityId(), request.languageCode().toUpperCase(), request.fieldName()
        ).orElse(null);

        if (existing != null) {
            throw new ConflictException("TRANSLATION_ALREADY_EXISTS", "error.translation_already_exists");
        }

        Translation translation = Translation.builder()
                .entityType(normalizedEntityType)
                .entityId(request.entityId())
                .languageCode(request.languageCode().toUpperCase())
                .fieldName(request.fieldName())
                .fieldValue(request.fieldValue())
                .build();
        Translation saved = translationRepository.save(translation);
        clearResponseCaches();
        return saved;
    }

    @Override
    public void deleteTranslation(Long id) {
        if (!translationRepository.existsById(id)) {
            throw new ResourceNotFoundException("TRANSLATION_NOT_FOUND", "error.resource_not_found");
        }
        translationRepository.deleteById(id);
        clearResponseCaches();
    }

    @Override
    public List<Translation> getTranslations(String entityType, String entityId, String fieldName, String languageCode) {
        String normType = entityType != null ? entityType.toUpperCase() : null;
        return translationRepository.findAll().stream()
                .filter(t -> normType == null || t.getEntityType().equals(normType))
                .filter(t -> entityId == null || t.getEntityId().equals(entityId))
                .filter(t -> fieldName == null || t.getFieldName().equals(fieldName))
                .filter(t -> languageCode == null || t.getLanguageCode().equalsIgnoreCase(languageCode))
                .toList();
    }

    @Override
    @org.springframework.scheduling.annotation.Async
    public void copyAzForProperNouns(String entityType, String entityId, String fieldName, String originalValueAz) {
        if (originalValueAz == null || originalValueAz.trim().isEmpty()) {
            return;
        }
        if (!isProperNounField(entityType, fieldName)) {
            return;
        }
        // Gym/store names: keep EN/RU rows identical to the entity name (for admin listing only).
        // Reads always use gyms.name / stores.name via getTranslatedValue → null.
        if (isSharedNameField(entityType, fieldName)) {
            upsert(entityType, entityId, "EN", fieldName, originalValueAz);
            upsert(entityType, entityId, "RU", fieldName, originalValueAz);
            clearResponseCaches();
            return;
        }
        seedIfAbsent(entityType, entityId, "EN", fieldName, originalValueAz);
        seedIfAbsent(entityType, entityId, "RU", fieldName, originalValueAz);
        clearResponseCaches();
    }

    private void seedIfAbsent(String entityType, String entityId, String languageCode,
                              String fieldName, String value) {
        boolean exists = translationRepository
                .findByEntityTypeAndEntityIdAndLanguageCodeAndFieldName(
                        entityType.toUpperCase(), entityId, languageCode.toUpperCase(), fieldName)
                .isPresent();
        if (!exists) {
            upsert(entityType, entityId, languageCode, fieldName, value);
        }
    }

    /** Gym/store display name: one string for every locale. */
    private static boolean isSharedNameField(String entityType, String fieldName) {
        if (fieldName == null || !fieldName.equalsIgnoreCase("name") || entityType == null) {
            return false;
        }
        String type = entityType.toUpperCase(java.util.Locale.ROOT).replace("_", "");
        return type.equals("GYM") || type.equals("STORE");
    }

    private static boolean isProperNounField(String entityType, String fieldName) {
        if (fieldName == null) {
            return false;
        }
        String field = fieldName.toLowerCase(java.util.Locale.ROOT);
        boolean addressLike = field.equals("addresstext") || field.equals("city") || field.equals("rayon");
        boolean nameLike = field.equals("name") || field.equals("surname");
        if (!addressLike && !nameLike) {
            return false;
        }
        if (entityType == null) {
            return addressLike;
        }
        String type = entityType.toUpperCase(java.util.Locale.ROOT).replace("_", "");
        if (addressLike) {
            return type.equals("GYM") || type.equals("STORE");
        }
        return type.equals("GYM") || type.equals("STORE") || type.equals("TRAINER") || type.equals("GYMADMIN");
    }

    @org.springframework.transaction.annotation.Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void saveOrUpdateTranslation(String entityType, String entityId, String languageCode, String fieldName, String fieldValue) {
        if (isSharedNameField(entityType, fieldName)) {
            return;
        }
        upsert(entityType, entityId, languageCode, fieldName, fieldValue);
        clearResponseCaches();
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public Translation saveOrUpdateTranslation(CreateTranslationRequest request) {
        if (isSharedNameField(request.entityType(), request.fieldName())) {
            return null;
        }
        Translation saved = upsert(request.entityType(), request.entityId(), request.languageCode(),
                request.fieldName(), request.fieldValue());
        clearResponseCaches();
        return saved;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void saveOrUpdateTranslations(List<CreateTranslationRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return;
        }

        // Load once per entity type instead of one query per value.
        Map<String, Set<String>> idsByType = new HashMap<>();
        for (CreateTranslationRequest request : requests) {
            if (isTranslatable(request)) {
                idsByType.computeIfAbsent(normalize(request.entityType()), k -> new HashSet<>())
                        .add(request.entityId());
            }
        }
        Map<String, Translation> byKey = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : idsByType.entrySet()) {
            for (Translation translation : translationRepository
                    .findByEntityTypeAndEntityIdIn(entry.getKey(), entry.getValue())) {
                byKey.put(translationKey(translation.getEntityType(), translation.getEntityId(),
                        translation.getFieldName(), translation.getLanguageCode()), translation);
            }
        }

        List<Translation> toSave = new ArrayList<>();
        Set<String> processedKeys = new HashSet<>();
        for (CreateTranslationRequest request : requests) {
            if (!isTranslatable(request)) {
                continue;
            }
            String entityType = normalize(request.entityType());
            String languageCode = normalize(request.languageCode());
            String fieldName = request.fieldName();
            // Blank localized values are stored as null so every read path falls back to AZ.
            String value = (request.fieldValue() == null || request.fieldValue().isBlank())
                    ? null : request.fieldValue();
            String key = translationKey(entityType, request.entityId(), fieldName, languageCode);
            if (!processedKeys.add(key)) {
                continue;
            }

            Translation translation = byKey.get(key);
            if (translation == null) {
                if (value == null) {
                    continue;
                }
                translation = Translation.builder()
                        .entityType(entityType)
                        .entityId(request.entityId())
                        .languageCode(languageCode)
                        .fieldName(fieldName)
                        .fieldValue(value)
                        .build();
            } else {
                translation.setFieldValue(value);
            }
            byKey.put(key, translation);
            toSave.add(translation);
        }

        if (!toSave.isEmpty()) {
            translationRepository.saveAll(toSave);
            clearResponseCaches();
        }
    }

    private boolean isTranslatable(CreateTranslationRequest request) {
        return request != null && request.entityType() != null && request.entityId() != null
                && request.fieldName() != null && request.languageCode() != null
                && !isSharedNameField(request.entityType(), request.fieldName());
    }

    private static String normalize(String value) {
        return value.toUpperCase(Locale.ROOT);
    }

    private static String translationKey(String entityType, String entityId, String fieldName, String languageCode) {
        return normalize(entityType) + "|" + entityId + "|" + fieldName + "|" + normalize(languageCode);
    }

    private Translation upsert(String entityType, String entityId, String languageCode,
                               String fieldName, String fieldValue) {
        String normalizedEntityType = entityType.toUpperCase();
        String normalizedLanguageCode = languageCode.toUpperCase();

        Translation existing = translationRepository.findByEntityTypeAndEntityIdAndLanguageCodeAndFieldName(
                normalizedEntityType, entityId, normalizedLanguageCode, fieldName
        ).orElse(null);

        if (existing != null) {
            existing.setFieldValue(fieldValue);
            return translationRepository.save(existing);
        }
        return translationRepository.save(Translation.builder()
                .entityType(normalizedEntityType)
                .entityId(entityId)
                .languageCode(normalizedLanguageCode)
                .fieldName(fieldName)
                .fieldValue(fieldValue)
                .build());
    }

    /** Programmatic clear so self-invokes and @Async paths always drop stale entries. */
    private void clearResponseCaches() {
        if (cacheManager == null) {
            return;
        }
        for (String name : RESPONSE_CACHES) {
            Cache cache = cacheManager.getCache(name);
            if (cache != null) {
                cache.clear();
            }
        }
    }
}
