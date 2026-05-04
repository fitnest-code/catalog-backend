package az.fitnest.catalog.service.impl;

import az.fitnest.catalog.dto.request.GymLessonTypeRequest;
import az.fitnest.catalog.dto.*;
import az.fitnest.catalog.dto.request.*;
import az.fitnest.catalog.dto.response.*;
import az.fitnest.catalog.dto.response.GymLessonTypeResponse;
import az.fitnest.catalog.exception.BadRequestException;
import az.fitnest.catalog.exception.ResourceNotFoundException;
import az.fitnest.catalog.repository.CategoryRepository;
import az.fitnest.catalog.repository.GymLessonTypeRepository;
import az.fitnest.catalog.repository.GymRepository;
import az.fitnest.catalog.model.entity.Category;
import az.fitnest.catalog.model.entity.Gym;
import az.fitnest.catalog.model.entity.GymLessonType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GymLessonTypeServiceImpl implements az.fitnest.catalog.service.GymLessonTypeService {

    private final GymRepository gymRepository;
    private final GymLessonTypeRepository gymLessonTypeRepository;
    private final CategoryRepository categoryRepository;

    @Transactional
    public GymLessonTypeResponse addLessonType(Long gymId, Long categoryId, GymLessonTypeRequest request) {
        Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));

        if (gymLessonTypeRepository.existsByGymIdAndName(gymId, request.getName().trim())) {
            throw new BadRequestException("LESSON_TYPE_ALREADY_EXISTS", "error.lesson_type_already_exists");
        }

        Category category = null;
        if (categoryId != null) {
            category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new ResourceNotFoundException("CATEGORY_NOT_FOUND", "error.category_not_found"));
        }

        GymLessonType lessonType = GymLessonType.builder()
                .gym(gym)
                .category(category)
                .name(request.getName().trim())
                .build();

        lessonType = gymLessonTypeRepository.save(lessonType);
        return mapToResponse(lessonType);
    }

    @Transactional(readOnly = true)
    public List<GymLessonTypeResponse> getLessonTypes(Long gymId) {
        if (!gymRepository.existsById(gymId)) {
            throw new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found");
        }
        return gymLessonTypeRepository.findByGymId(gymId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public GymLessonTypeResponse updateLessonType(Long gymId, Long lessonTypeId, Long categoryId, GymLessonTypeRequest request) {
        GymLessonType lessonType = gymLessonTypeRepository.findById(lessonTypeId)
                .orElseThrow(() -> new ResourceNotFoundException("LESSON_TYPE_NOT_FOUND", "error.lesson_type_not_found"));

        if (!lessonType.getGym().getId().equals(gymId)) {
            throw new ResourceNotFoundException("LESSON_TYPE_NOT_FOUND", "error.lesson_type_not_found");
        }

        String newName = request.getName().trim();
        if (!newName.equals(lessonType.getName()) && gymLessonTypeRepository.existsByGymIdAndName(gymId, newName)) {
            throw new BadRequestException("LESSON_TYPE_ALREADY_EXISTS", "error.lesson_type_already_exists");
        }

        if (categoryId != null) {
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new ResourceNotFoundException("CATEGORY_NOT_FOUND", "error.category_not_found"));
            lessonType.setCategory(category);
        }

        lessonType.setName(newName);
        lessonType = gymLessonTypeRepository.save(lessonType);
        return mapToResponse(lessonType);
    }

    @Transactional
    public void deleteLessonType(Long gymId, Long lessonTypeId) {
        GymLessonType lessonType = gymLessonTypeRepository.findById(lessonTypeId)
                .orElseThrow(() -> new ResourceNotFoundException("LESSON_TYPE_NOT_FOUND", "error.lesson_type_not_found"));

        if (!lessonType.getGym().getId().equals(gymId)) {
            throw new ResourceNotFoundException("LESSON_TYPE_NOT_FOUND", "error.lesson_type_not_found");
        }

        gymLessonTypeRepository.delete(lessonType);
    }

    private GymLessonTypeResponse mapToResponse(GymLessonType lessonType) {
        return GymLessonTypeResponse.builder()
                .id(lessonType.getId())
                .gymId(lessonType.getGym().getId())
                .categoryId(lessonType.getCategory() != null ? lessonType.getCategory().getId() : null)
                .name(lessonType.getName())
                .build();
    }
}
