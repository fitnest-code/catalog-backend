package az.fitnest.catalog.service.impl;

import az.fitnest.catalog.dto.response.GymLessonTypeResponse;
import az.fitnest.catalog.exception.ResourceNotFoundException;
import az.fitnest.catalog.model.entity.Category;
import az.fitnest.catalog.model.entity.Gym;
import az.fitnest.catalog.model.entity.LessonType;
import az.fitnest.catalog.repository.GymRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GymLessonTypeServiceImpl implements az.fitnest.catalog.service.GymLessonTypeService {

    private final GymRepository gymRepository;

    @Transactional(readOnly = true)
    public List<GymLessonTypeResponse> getLessonTypes(Long gymId) {
        Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));

        // Lesson types are not stored per gym: every lesson type of every category
        // assigned to the gym is available.
        Map<Long, GymLessonTypeResponse> byLessonTypeId = new LinkedHashMap<>();
        for (Category category : gym.getCategories()) {
            if (category.getLessonTypes() == null) continue;
            for (LessonType lessonType : category.getLessonTypes()) {
                byLessonTypeId.putIfAbsent(lessonType.getId(), GymLessonTypeResponse.builder()
                        .id(lessonType.getId())
                        .gymId(gymId)
                        .categoryId(category.getId())
                        .name(lessonType.getName())
                        .build());
            }
        }
        return new ArrayList<>(byLessonTypeId.values());
    }
}
