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

    @org.springframework.beans.factory.annotation.Value("${LIBRETRANSLATE_URL:http://10.0.0.4:5000}")
    private String libreTranslateUrl;

    @Override
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
        log.info("LibreTranslate API Request: url='{}/translate', text='{}', source='az', target='{}'", 
            libreTranslateUrl, text, targetLanguage);
        try {
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED);

            org.springframework.util.LinkedMultiValueMap<String, String> map = new org.springframework.util.LinkedMultiValueMap<>();
            map.add("q", text);
            map.add("source", "az");
            map.add("target", targetLanguage.toLowerCase());

            org.springframework.http.HttpEntity<org.springframework.util.LinkedMultiValueMap<String, String>> request = 
                new org.springframework.http.HttpEntity<>(map, headers);

            String requestUrl = libreTranslateUrl + "/translate";
            org.springframework.http.ResponseEntity<java.util.Map> response = restTemplate.postForEntity(
                requestUrl,
                request,
                java.util.Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String translatedText = (String) response.getBody().get("translatedText");
                log.info("LibreTranslate API Response: HTTP status={}, translatedText='{}'", 
                    response.getStatusCode(), translatedText);
                return translatedText;
            } else {
                log.warn("LibreTranslate API Response returned non-success status code: HTTP status={}, body={}", 
                    response.getStatusCode(), response.getBody());
            }
        } catch (Exception e) {
            log.error("LibreTranslate API Request FAILED: url='{}/translate', text='{}', targetLanguage='{}'. Error: {}", 
                libreTranslateUrl, text, targetLanguage, e.getMessage(), e);
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
    }
}
