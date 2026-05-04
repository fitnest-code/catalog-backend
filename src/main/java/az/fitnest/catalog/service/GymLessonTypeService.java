package az.fitnest.catalog.service;

import az.fitnest.catalog.dto.GymLessonTypeRequest;
import az.fitnest.catalog.dto.GymLessonTypeResponse;
import java.util.List;

public interface GymLessonTypeService {
    GymLessonTypeResponse addLessonType(Long gymId, Long categoryId, GymLessonTypeRequest request);
    List<GymLessonTypeResponse> getLessonTypes(Long gymId);
    GymLessonTypeResponse updateLessonType(Long gymId, Long lessonTypeId, Long categoryId, GymLessonTypeRequest request);
    void deleteLessonType(Long gymId, Long lessonTypeId);
}
