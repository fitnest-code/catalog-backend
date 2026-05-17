package az.fitnest.catalog.service;

import az.fitnest.catalog.dto.request.LessonTypeRequest;
import az.fitnest.catalog.dto.response.LessonTypeResponse;

import java.util.List;

public interface LessonTypeAdminService {
    LessonTypeResponse createLessonType(LessonTypeRequest request);

    List<LessonTypeResponse> getAllLessonTypes();

    void deleteLessonType(Long id);
}
