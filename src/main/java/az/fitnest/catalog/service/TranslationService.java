package az.fitnest.catalog.service;

public interface TranslationService {
    String getTranslatedValue(String entityType, String entityId, String fieldName, String languageCode);
}

