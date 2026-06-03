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

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    @org.springframework.beans.factory.annotation.Autowired
    @org.springframework.context.annotation.Lazy
    private TranslationServiceImpl self;

    @org.springframework.beans.factory.annotation.Autowired
    private TranslationEntityResolver translationEntityResolver;

    public TranslationServiceImpl(TranslationRepository translationRepository, org.springframework.cache.CacheManager cacheManager) {
        this.translationRepository = translationRepository;
        this.cacheManager = cacheManager;
        org.springframework.http.client.SimpleClientHttpRequestFactory factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(1000);
        factory.setReadTimeout(1500);
        this.restTemplate = new org.springframework.web.client.RestTemplate(factory);
        this.restTemplate.getMessageConverters().add(0, new org.springframework.http.converter.StringHttpMessageConverter(java.nio.charset.StandardCharsets.UTF_8));
    }

    @Override
    @org.springframework.cache.annotation.Cacheable(value = "translations", key = "#entityType + '_' + #entityId + '_' + #fieldName + '_' + #languageCode")
    public String getTranslatedValue(String entityType, String entityId, String fieldName, String languageCode) {
        if (languageCode == null || languageCode.equalsIgnoreCase("AZ")) {
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

        try {
            Class<?> entityClass = translationEntityResolver.getEntityClass(entityType);
            if (entityClass != null) {
                Object entity = null;
                try {
                    Long longId = Long.parseLong(entityId);
                    entity = entityManager.find(entityClass, longId);
                } catch (NumberFormatException e) {
                    entity = entityManager.find(entityClass, entityId);
                }

                if (entity != null) {
                    String originalValueAz = translationEntityResolver.extractFieldValue(entity, fieldName);
                    if (originalValueAz != null && !originalValueAz.trim().isEmpty()) {
                        String translatedValue = translateText(originalValueAz, languageCode.toLowerCase());
                        if (translatedValue != null && !translatedValue.trim().isEmpty()) {
                            self.saveOrUpdateTranslation(entityType, entityId, languageCode, fieldName, translatedValue);
                            return translatedValue;
                        }
                    }
                }
            }
        } catch (Exception e) {
        }

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
        return translationRepository.findAll().stream()
                .filter(t -> entityType == null || t.getEntityType().equals(entityType))
                .filter(t -> entityId == null || t.getEntityId().equals(entityId))
                .filter(t -> fieldName == null || t.getFieldName().equals(fieldName))
                .filter(t -> languageCode == null || t.getLanguageCode().equalsIgnoreCase(languageCode))
                .toList();
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

    @Override
    public String translateText(String text, String targetLanguage) {
        // Try Google Translate (Ultra-accurate, extremely reliable, free, no keys needed)
        try {
            String googleTranslated = translateWithGoogle(text, targetLanguage);
            if (googleTranslated != null && !googleTranslated.trim().isEmpty()) {
                return googleTranslated;
            }
        } catch (Exception e) {
        }
        return null;
    }

    private String sanitizeHtml(String text) {
        if (text == null) return null;
        if (!text.contains("<") && !text.contains(">")) {
            return text;
        }
        try {
            String clean = text.replaceAll("(?i)<head[^>]*?>[\\s\\S]*?</head>", "");
            clean = clean.replaceAll("(?i)<style[^>]*?>[\\s\\S]*?</style>", "");
            clean = clean.replaceAll("(?i)<script[^>]*?>[\\s\\S]*?</script>", "");
            clean = clean.replaceAll("<[^>]*>", " ");
            clean = clean.replaceAll("\\s+", " ").trim();
            return org.apache.commons.text.StringEscapeUtils.unescapeHtml4(clean);
        } catch (Exception e) {
            return text;
        }
    }

    private String translateWithGoogle(String text, String targetLanguage) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        text = sanitizeHtml(text);
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
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
                        StringBuilder sb = new StringBuilder();
                        for (com.fasterxml.jackson.databind.JsonNode segment : firstArray) {
                            if (segment.isArray() && segment.size() > 0) {
                                sb.append(segment.get(0).asText());
                            }
                        }
                        if (sb.length() > 0) {
                            return sb.toString();
                        }
                    }
                }
            }
        } catch (Exception e) {
        }
        return null;
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
