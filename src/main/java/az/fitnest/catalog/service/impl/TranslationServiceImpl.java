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
@RequiredArgsConstructor
public class TranslationServiceImpl implements TranslationService {
    private final TranslationRepository translationRepository;
    private final org.springframework.cache.CacheManager cacheManager;

    @Override
    @org.springframework.cache.annotation.Cacheable(value = "translations", key = "#entityType + '_' + #entityId + '_' + #fieldName + '_' + #languageCode")
    public String getTranslatedValue(String entityType, String entityId, String fieldName, String languageCode) {
        if (languageCode == null || languageCode.equalsIgnoreCase("AZ")) {
            return null;
        }

        return translationRepository.findByEntityTypeAndEntityIdAndLanguageCodeAndFieldName(
                entityType.toUpperCase(),
                entityId,
                languageCode.toUpperCase(),
                fieldName
        )
        .map(Translation::getFieldValue)
        .orElse(null);
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
        return translationRepository.findAll().stream()
                .filter(t -> entityType == null || t.getEntityType().equals(entityType))
                .filter(t -> entityId == null || t.getEntityId().equals(entityId))
                .filter(t -> fieldName == null || t.getFieldName().equals(fieldName))
                .filter(t -> languageCode == null || t.getLanguageCode().equalsIgnoreCase(languageCode))
                .toList();
    }

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TranslationServiceImpl.class);

    @Override
    @org.springframework.scheduling.annotation.Async
    @org.springframework.cache.annotation.CacheEvict(value = "translations", allEntries = true)
    public void autoTranslateAndSave(String entityType, String entityId, String fieldName, String originalValueAz) {
        if (originalValueAz == null || originalValueAz.trim().isEmpty()) {
            log.warn("Auto-translation skipped: originalValueAz is null or empty for entityType={}, entityId={}, fieldName={}", 
                entityType, entityId, fieldName);
            return;
        }

        log.info("Starting auto-translation process for entityType={}, entityId={}, fieldName={}, originalValueAz='{}'", 
            entityType, entityId, fieldName, originalValueAz);

        // Translate to EN
        String enValue = translateText(originalValueAz, "en");
        if (enValue != null && !enValue.trim().isEmpty()) {
            log.info("Auto-translated [AZ -> EN] success. Value: '{}'", enValue);
            saveOrUpdateTranslation(entityType, entityId, "EN", fieldName, enValue);
        } else {
            log.warn("Auto-translation [AZ -> EN] returned empty or null value. Using fallback: '{}'", originalValueAz);
            saveOrUpdateTranslation(entityType, entityId, "EN", fieldName, originalValueAz);
        }

        // Translate to RU
        String ruValue = translateText(originalValueAz, "ru");
        if (ruValue != null && !ruValue.trim().isEmpty()) {
            log.info("Auto-translated [AZ -> RU] success. Value: '{}'", ruValue);
            saveOrUpdateTranslation(entityType, entityId, "RU", fieldName, ruValue);
        } else {
            log.warn("Auto-translation [AZ -> RU] returned empty or null value. Using fallback: '{}'", originalValueAz);
            saveOrUpdateTranslation(entityType, entityId, "RU", fieldName, originalValueAz);
        }
    }

    private String translateText(String text, String targetLanguage) {
        // Try Google Translate (Ultra-accurate, extremely reliable, free, no keys needed)
        try {
            String googleTranslated = translateWithGoogle(text, targetLanguage);
            if (googleTranslated != null && !googleTranslated.trim().isEmpty()) {
                log.info("Translation successful using Google Translate [AZ -> {}]: '{}' -> '{}'", 
                    targetLanguage.toUpperCase(), text, googleTranslated);
                return googleTranslated;
            }
        } catch (Exception e) {
            log.error("Google Translate failed. Error: {}", e.getMessage());
        }
        return null;
    }

    private String translateWithGoogle(String text, String targetLanguage) {
        try {
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            java.net.URI uri = org.springframework.web.util.UriComponentsBuilder
                .fromUriString("https://translate.googleapis.com/translate_a/single")
                .queryParam("client", "gtx")
                .queryParam("sl", "az")
                .queryParam("tl", targetLanguage.toLowerCase())
                .queryParam("dt", "t")
                .queryParam("q", text)
                .build()
                .toUri();

            log.info("Google Translate Request [AZ -> {}]: '{}'", targetLanguage.toUpperCase(), text);
            String response = restTemplate.getForObject(uri, String.class);
            if (response != null) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.databind.JsonNode rootNode = mapper.readTree(response);
                if (rootNode.isArray() && rootNode.size() > 0) {
                    com.fasterxml.jackson.databind.JsonNode firstArray = rootNode.get(0);
                    if (firstArray.isArray() && firstArray.size() > 0) {
                        com.fasterxml.jackson.databind.JsonNode translationPair = firstArray.get(0);
                        if (translationPair.isArray() && translationPair.size() > 0) {
                            return translationPair.get(0).asText();
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Google Translation API failed for text '{}' to '{}': {}", text, targetLanguage, e.getMessage());
        }
        return null;
    }

    private void saveOrUpdateTranslation(String entityType, String entityId, String languageCode, String fieldName, String fieldValue) {
        String normalizedEntityType = entityType.toUpperCase();
        String normalizedLanguageCode = languageCode.toUpperCase();

        log.info("Database Save: entityType={}, entityId={}, languageCode={}, fieldName={}, fieldValue='{}'", 
            normalizedEntityType, entityId, normalizedLanguageCode, fieldName, fieldValue);

        Translation existing = translationRepository.findByEntityTypeAndEntityIdAndLanguageCodeAndFieldName(
                normalizedEntityType, entityId, normalizedLanguageCode, fieldName
        ).orElse(null);

        if (existing != null) {
            log.info("Updating existing translation record ID={}", existing.getId());
            existing.setFieldValue(fieldValue);
            translationRepository.save(existing);
        } else {
            log.info("Creating new translation record");
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

    private void evictCache(String entityType, String entityId, String fieldName, String languageCode) {
        if (cacheManager != null) {
            try {
                org.springframework.cache.Cache cache = cacheManager.getCache("translations");
                if (cache != null) {
                    String key = entityType + "_" + entityId + "_" + fieldName + "_" + languageCode;
                    cache.evict(key);
                    log.info("Evicted translation cache for key: {}", key);
                }
            } catch (Exception e) {
                log.error("Failed to evict cache: {}", e.getMessage());
            }
        }
    }
}
