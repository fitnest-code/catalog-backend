package az.fitnest.catalog.service.impl;

import az.fitnest.catalog.model.entity.Translation;
import az.fitnest.catalog.repository.TranslationRepository;
import az.fitnest.catalog.service.TranslationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TranslationServiceImpl implements TranslationService {
    private final TranslationRepository translationRepository;

    @Override
    public String getTranslatedValue(String entityType, String entityId, String fieldName, String languageCode) {
        String value = translationRepository
                .findAll()
                .stream()
                .filter(t -> t.getEntityType().equals(entityType)
                        && t.getEntityId().equals(entityId)
                        && t.getLanguageCode().equalsIgnoreCase(languageCode)
                        && t.getFieldName().equals(fieldName))
                .map(Translation::getFieldValue)
                .findFirst()
                .orElse(null);
        if (value != null && !value.isEmpty()) {
            return value;
        }
        return translationRepository
                .findAll()
                .stream()
                .filter(t -> t.getEntityType().equals(entityType)
                        && t.getEntityId().equals(entityId)
                        && t.getLanguageCode().equalsIgnoreCase("AZ")
                        && t.getFieldName().equals(fieldName))
                .map(Translation::getFieldValue)
                .findFirst()
                .orElse("");
    }
}
