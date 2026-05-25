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
    private final org.springframework.web.client.RestTemplate restTemplate;

    public TranslationServiceImpl(TranslationRepository translationRepository, org.springframework.cache.CacheManager cacheManager) {
        this.translationRepository = translationRepository;
        this.cacheManager = cacheManager;
        org.springframework.http.client.SimpleClientHttpRequestFactory factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(1000);
        factory.setReadTimeout(1500);
        this.restTemplate = new org.springframework.web.client.RestTemplate(factory);
    }

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
            existing.setFieldValue(request.fieldValue());
            return translationRepository.save(existing);
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
        String normalizedEntityType = entityType != null ? entityType.toUpperCase() : null;
        return translationRepository.findTranslationsByFilters(normalizedEntityType, entityId, fieldName, languageCode);
    }

    @Override
    @org.springframework.scheduling.annotation.Async
    @org.springframework.cache.annotation.CacheEvict(value = "translations", allEntries = true)
    public void autoTranslateAndSave(String entityType, String entityId, String fieldName, String originalValueAz) {
        if (originalValueAz == null || originalValueAz.trim().isEmpty()) {
            return;
        }

        // Translate to EN
        String enValue = translateText(originalValueAz, "en");
        if (enValue != null && !enValue.trim().isEmpty()) {
            saveOrUpdateTranslation(entityType, entityId, "EN", fieldName, enValue);
        } else {
            saveOrUpdateTranslation(entityType, entityId, "EN", fieldName, originalValueAz);
        }

        // Translate to RU
        String ruValue = translateText(originalValueAz, "ru");
        if (ruValue != null && !ruValue.trim().isEmpty()) {
            saveOrUpdateTranslation(entityType, entityId, "RU", fieldName, ruValue);
        } else {
            saveOrUpdateTranslation(entityType, entityId, "RU", fieldName, originalValueAz);
        }
    }

    private String translateText(String text, String targetLanguage) {
        try {
            String googleTranslated = translateWithGoogle(text, targetLanguage);
            if (googleTranslated != null && !googleTranslated.trim().isEmpty()) {
                return googleTranslated;
            }
        } catch (Exception e) {
        }
        return null;
    }

    private String translateWithGoogle(String text, String targetLanguage) {
        try {
            java.net.URI uri = org.springframework.web.util.UriComponentsBuilder
                .fromUriString("https://translate.googleapis.com/translate_a/single")
                .queryParam("client", "gtx")
                .queryParam("sl", "az")
                .queryParam("tl", targetLanguage.toLowerCase())
                .queryParam("dt", "t")
                .queryParam("q", text)
                .build()
                .toUri();

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
        }
        return null;
    }

    private void saveOrUpdateTranslation(String entityType, String entityId, String languageCode, String fieldName, String fieldValue) {
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
