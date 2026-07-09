package az.fitnest.catalog.service;

import az.fitnest.catalog.dto.response.GymLessonTypeResponse;

import java.util.List;

public interface GymLessonTypeService {
    List<GymLessonTypeResponse> getLessonTypes(Long gymId);
}
