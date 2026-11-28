package az.fitnest.catalog.service;

import az.fitnest.catalog.dto.request.CategoryDetailV3;
import az.fitnest.catalog.dto.request.CreateTranslationRequest;
import az.fitnest.catalog.dto.request.LocalizedServiceNameRequest;
import az.fitnest.catalog.model.entity.SupportedService;
import az.fitnest.catalog.repository.SupportedServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Persists the optional EN/RU values of admin-written free text.
 * AZ stays on the entity row (gyms.description, gym_descriptions.description,
 * supported_services.name), so absent or blank translations still fall back to AZ.
 */
@Component
@RequiredArgsConstructor
public class GymTranslationWriter {

    private static final String GYM = "GYM";
    private static final String SUPPORTED_SERVICE = "SUPPORTEDSERVICE";
    private static final String DESCRIPTION = "description";
    private static final String NAME = "name";
    private static final String EN = "EN";
    private static final String RU = "RU";

    private final TranslationService translationService;
    private final SupportedServiceRepository supportedServiceRepository;

    public void saveDescriptions(Long gymId, String descriptionEn, String descriptionRu,
                                 List<CategoryDetailV3> categoryDetails) {
        String entityId = gymId.toString();
        List<CreateTranslationRequest> requests = new ArrayList<>();
        Set<String> fields = new HashSet<>();
        add(requests, fields, GYM, entityId, DESCRIPTION, EN, descriptionEn);
        add(requests, fields, GYM, entityId, DESCRIPTION, RU, descriptionRu);

        if (categoryDetails != null) {
            for (CategoryDetailV3 detail : categoryDetails) {
                if (detail == null || detail.categoryId() == null) {
                    continue;
                }
                String field = DESCRIPTION + "_" + detail.categoryId();
                add(requests, fields, GYM, entityId, field, EN, detail.descriptionEn());
                add(requests, fields, GYM, entityId, field, RU, detail.descriptionRu());
            }
        }
        translationService.saveOrUpdateTranslations(requests);
    }

    public void saveCustomServiceNames(Long gymId, List<LocalizedServiceNameRequest> customServices) {
        if (customServices == null || customServices.isEmpty()) {
            return;
        }
        // One query for all gym-specific services; predefined reference services are skipped.
        Map<String, SupportedService> byName = supportedServiceRepository.findAllByGymId(gymId).stream()
                .filter(service -> service.getName() != null)
                .collect(Collectors.toMap(
                        service -> service.getName().trim().toLowerCase(Locale.ROOT),
                        Function.identity(),
                        (first, second) -> first));

        List<CreateTranslationRequest> requests = new ArrayList<>();
        Set<String> fields = new HashSet<>();
        for (LocalizedServiceNameRequest service : customServices) {
            if (service == null || service.name() == null || service.name().isBlank()) {
                continue;
            }
            SupportedService entity = byName.get(service.name().trim().toLowerCase(Locale.ROOT));
            if (entity == null) {
                continue;
            }
            String entityId = entity.getId().toString();
            add(requests, fields, SUPPORTED_SERVICE, entityId, NAME, EN, service.nameEn());
            add(requests, fields, SUPPORTED_SERVICE, entityId, NAME, RU, service.nameRu());
        }
        translationService.saveOrUpdateTranslations(requests);
    }

    private static void add(List<CreateTranslationRequest> requests, Set<String> fields, String entityType,
                            String entityId, String fieldName, String languageCode, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        // One upsert per (entity, field, language), even if the same value arrives twice in a wizard payload.
        if (!fields.add(entityType + "|" + entityId + "|" + fieldName + "|" + languageCode)) {
            return;
        }
        requests.add(new CreateTranslationRequest(entityType, entityId, languageCode, fieldName, value));
    }
}
