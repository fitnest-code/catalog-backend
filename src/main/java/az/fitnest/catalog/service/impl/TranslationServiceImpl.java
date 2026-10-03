package az.fitnest.catalog.service.impl;

import az.fitnest.catalog.model.entity.Translation;
import az.fitnest.catalog.dto.*;
import az.fitnest.catalog.dto.request.*;
import az.fitnest.catalog.dto.response.*;
import az.fitnest.catalog.repository.TranslationRepository;
import az.fitnest.catalog.service.TranslationService;
import lombok.RequiredArgsConstructor;
import az.fitnest.catalog.dto.request.CreateTranslationRequest;
import az.fitnest.catalog.exception.ConflictException;
import az.fitnest.catalog.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class TranslationServiceImpl implements TranslationService {
    private final TranslationRepository translationRepository;
    private final org.springframework.cache.CacheManager cacheManager;

    public TranslationServiceImpl(TranslationRepository translationRepository, org.springframework.cache.CacheManager cacheManager) {
        this.translationRepository = translationRepository;
        this.cacheManager = cacheManager;
    }

    @Override
    @org.springframework.cache.annotation.Cacheable(value = "translations", key = "#entityType + '_' + #entityId + '_' + #fieldName + '_' + #languageCode")
    public String getTranslatedValue(String entityType, String entityId, String fieldName, String languageCode) {
        if (languageCode == null || languageCode.equalsIgnoreCase("AZ")) {
            return null;
        }
        if (isProperNounField(entityType, fieldName)) {
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

        String existingValue = translationRepository.findByEntityTypeAndEntityIdAndLanguageCodeAndFieldName(
                entityType.toUpperCase(),
                entityId,
                languageCode.toUpperCase(),
                fieldName
        )
        .map(Translation::getFieldValue)
        .orElse(null);

        if (existingValue != null) {
            return existingValue;
        }

        if (entityType != null && (entityType.equalsIgnoreCase("CATEGORY") || entityType.equalsIgnoreCase("LESSONTYPE") || entityType.equalsIgnoreCase("LESSON_TYPE")
                || entityType.equalsIgnoreCase("GYM") || entityType.equalsIgnoreCase("ROOM")
                || entityType.equalsIgnoreCase("SUPPORTED_SERVICE") || entityType.equalsIgnoreCase("SUPPORTEDSERVICE")
                || entityType.equalsIgnoreCase("RESERVATION_RULE") || entityType.equalsIgnoreCase("RESERVATIONRULE")
                || entityType.equalsIgnoreCase("GYMSUBSCRIPTION")
                || entityType.equalsIgnoreCase("TRAINER") || entityType.equalsIgnoreCase("PROFESSION"))) {
            return null;
        }

        // Manual translations only: AZ lives on its own entity table, EN/RU live in
        // the translations table (admin-provided). No machine translation.
        return null;
    }

    @Override
    @org.springframework.cache.annotation.CacheEvict(value = "translations", allEntries = true)
    public Translation createTranslation(CreateTranslationRequest request) {
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
        return translationRepository.save(translation);
    }

    @Override
    @org.springframework.cache.annotation.CacheEvict(value = "translations", allEntries = true)
    public void deleteTranslation(Long id) {
        if (!translationRepository.existsById(id)) {
            throw new ResourceNotFoundException("TRANSLATION_NOT_FOUND", "error.resource_not_found");
        }
        translationRepository.deleteById(id);
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
    @org.springframework.cache.annotation.CacheEvict(value = "translations", allEntries = true)
    public void copyAzForProperNouns(String entityType, String entityId, String fieldName, String originalValueAz) {
        if (originalValueAz == null || originalValueAz.trim().isEmpty()) {
            return;
        }
        // Proper nouns (names, addresses) are identical in every language:
        // copy the AZ source to EN/RU. All other fields stay untranslated until
        // an admin provides EN/RU manually. No machine translation.
        if (isProperNounField(entityType, fieldName)) {
            saveOrUpdateTranslation(entityType, entityId, "EN", fieldName, originalValueAz);
            saveOrUpdateTranslation(entityType, entityId, "RU", fieldName, originalValueAz);
        }
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
        String normalizedEntityType = entityType.toUpperCase();
        String normalizedLanguageCode = languageCode.toUpperCase();

        Translation existing = translationRepository.findByEntityTypeAndEntityIdAndLanguageCodeAndFieldName(
                normalizedEntityType, entityId, normalizedLanguageCode, fieldName
        ).orElse(null);

        if (existing != null) {
            existing.setFieldValue(fieldValue);
            translationRepository.save(existing);
        } else {
            Translation translation = Translation.builder()
                    .entityType(normalizedEntityType)
                    .entityId(entityId)
                    .languageCode(normalizedLanguageCode)
                    .fieldName(fieldName)
                    .fieldValue(fieldValue)
                    .build();
            translationRepository.save(translation);
        }

        evictCache(normalizedEntityType, entityId, fieldName, normalizedLanguageCode);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.cache.annotation.CacheEvict(value = "translations", allEntries = true)
    public Translation saveOrUpdateTranslation(CreateTranslationRequest request) {
        String normalizedEntityType = request.entityType().toUpperCase();
        String normalizedLanguageCode = request.languageCode().toUpperCase();

        Translation existing = translationRepository.findByEntityTypeAndEntityIdAndLanguageCodeAndFieldName(
                normalizedEntityType, request.entityId(), normalizedLanguageCode, request.fieldName()
        ).orElse(null);

        Translation saved;
        if (existing != null) {
            existing.setFieldValue(request.fieldValue());
            saved = translationRepository.save(existing);
        } else {
            Translation translation = Translation.builder()
                    .entityType(normalizedEntityType)
                    .entityId(request.entityId())
                    .languageCode(normalizedLanguageCode)
                    .fieldName(request.fieldName())
                    .fieldValue(request.fieldValue())
                    .build();
            saved = translationRepository.save(translation);
        }

        evictCache(normalizedEntityType, request.entityId(), request.fieldName(), normalizedLanguageCode);
        return saved;
    }

    private void evictCache(String entityType, String entityId, String fieldName, String languageCode) {
        if (cacheManager != null) {
            try {
                org.springframework.cache.Cache cache = cacheManager.getCache("translations");
                if (cache != null) {
                    String key = entityType + "_" + entityId + "_" + fieldName + "_" + languageCode;
                    cache.evict(key);
                }
            } catch (Exception e) {
            }
        }
    }
}
