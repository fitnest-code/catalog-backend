package az.fitnest.catalog.service;

import az.fitnest.catalog.dto.request.LessonTypeRequest;
import az.fitnest.catalog.dto.response.LessonTypeResponse;

import java.util.List;

public interface LessonTypeAdminService {
    LessonTypeResponse createLessonType(LessonTypeRequest request);

    List<LessonTypeResponse> getAllLessonTypes();

    LessonTypeResponse getLessonTypeById(Long id);

    LessonTypeResponse updateLessonType(Long id, LessonTypeRequest request);

    void deleteLessonType(Long id);
}
