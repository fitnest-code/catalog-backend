package az.fitnest.catalog.service;

import az.fitnest.catalog.dto.request.CreateTranslationRequest;
import az.fitnest.catalog.dto.*;
import az.fitnest.catalog.dto.request.*;
import az.fitnest.catalog.dto.response.*;
import az.fitnest.catalog.model.entity.Translation;
import java.util.List;

public interface TranslationService {
    String getTranslatedValue(String entityType, String entityId, String fieldName, String languageCode);
    Translation createTranslation(CreateTranslationRequest request);
    void deleteTranslation(Long id);
    List<Translation> getTranslations(String entityType, String entityId, String fieldName, String languageCode);
    void autoTranslateAndSave(String entityType, String entityId, String fieldName, String originalValueAz);
}
