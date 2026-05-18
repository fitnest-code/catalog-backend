package az.fitnest.catalog.service.impl;

import az.fitnest.catalog.dto.request.LessonTypeRequest;
import az.fitnest.catalog.dto.response.LessonTypeResponse;
import az.fitnest.catalog.model.entity.LessonType;
import az.fitnest.catalog.repository.CategoryRepository;
import az.fitnest.catalog.repository.LessonTypeRepository;
import az.fitnest.catalog.service.LessonTypeAdminService;
import az.fitnest.catalog.service.TranslationService;
import az.fitnest.catalog.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LessonTypeAdminServiceImpl implements LessonTypeAdminService {

    private final LessonTypeRepository lessonTypeRepository;
    private final CategoryRepository categoryRepository;
    private final TranslationService translationService;

    @Override
    @Transactional
    public LessonTypeResponse createLessonType(LessonTypeRequest request) {
        LessonType lessonType = LessonType.builder()
                .name(request.getName().trim())
                .build();
        lessonType = lessonTypeRepository.save(lessonType);

        translationService.autoTranslateAndSave("LessonType", String.valueOf(lessonType.getId()), "name", lessonType.getName());

        return new LessonTypeResponse(lessonType.getId(), lessonType.getName());
    }

    @Override
    @Transactional(readOnly = true)
    public List<LessonTypeResponse> getAllLessonTypes() {
        String lang = resolveUserLanguage();
        return lessonTypeRepository.findAll().stream()
                .map(lt -> {
                    String localized = translationService.getTranslatedValue("LessonType", String.valueOf(lt.getId()), "name", lang);
                    return new LessonTypeResponse(lt.getId(), localized != null ? localized : lt.getName());
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteLessonType(Long id) {
        lessonTypeRepository.findById(id).ifPresent(lessonType -> {
            if (lessonType.getCategories() != null && !lessonType.getCategories().isEmpty()) {
                lessonType.getCategories().forEach(category -> {
                    if (category.getLessonTypes() != null) {
                        category.getLessonTypes().remove(lessonType);
                    }
                });
                    categoryRepository.saveAll(lessonType.getCategories());
            }
            lessonTypeRepository.delete(lessonType);
        });
    }

    private String resolveUserLanguage() {
        try {
            String localeLang = org.springframework.context.i18n.LocaleContextHolder.getLocale().getLanguage()
                    .toUpperCase();
            if (localeLang.equals("EN") || localeLang.equals("RU")) {
                return localeLang;
            }
        } catch (Exception ignored) {
        }
        return "AZ";
    }
}
