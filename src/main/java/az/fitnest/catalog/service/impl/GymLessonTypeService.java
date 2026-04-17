package az.fitnest.catalog.service.impl;

import az.fitnest.catalog.dto.GymLessonTypeRequest;
import az.fitnest.catalog.dto.GymLessonTypeResponse;
import az.fitnest.catalog.exception.BadRequestException;
import az.fitnest.catalog.exception.ResourceNotFoundException;
import az.fitnest.catalog.model.entity.Gym;
import az.fitnest.catalog.model.entity.GymLessonType;
import az.fitnest.catalog.repository.GymLessonTypeRepository;
import az.fitnest.catalog.repository.GymRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GymLessonTypeService {

    private final GymRepository gymRepository;
    private final GymLessonTypeRepository gymLessonTypeRepository;

    @Transactional
    public GymLessonTypeResponse addLessonType(Long gymId, GymLessonTypeRequest request) {
        Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));

        if (gymLessonTypeRepository.existsByGymIdAndName(gymId, request.getName().trim())) {
            throw new BadRequestException("LESSON_TYPE_ALREADY_EXISTS", "error.lesson_type_already_exists");
        }

        GymLessonType lessonType = GymLessonType.builder()
                .gym(gym)
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
    public GymLessonTypeResponse updateLessonType(Long gymId, Long lessonTypeId, GymLessonTypeRequest request) {
        GymLessonType lessonType = gymLessonTypeRepository.findById(lessonTypeId)
                .orElseThrow(() -> new ResourceNotFoundException("LESSON_TYPE_NOT_FOUND", "error.lesson_type_not_found"));

        if (!lessonType.getGym().getId().equals(gymId)) {
            throw new ResourceNotFoundException("LESSON_TYPE_NOT_FOUND", "error.lesson_type_not_found");
        }

        String newName = request.getName().trim();
        if (!newName.equals(lessonType.getName()) && gymLessonTypeRepository.existsByGymIdAndName(gymId, newName)) {
            throw new BadRequestException("LESSON_TYPE_ALREADY_EXISTS", "error.lesson_type_already_exists");
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
                .name(lessonType.getName())
                .build();
    }
}
