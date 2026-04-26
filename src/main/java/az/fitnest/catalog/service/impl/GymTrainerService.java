package az.fitnest.catalog.service.impl;

import az.fitnest.catalog.client.UserServiceGrpcClient;
import az.fitnest.catalog.dto.GymTrainerDto;
import az.fitnest.catalog.dto.PaginatedResponse;
import az.fitnest.catalog.dto.TrainerRequest;
import az.fitnest.catalog.exception.ResourceNotFoundException;
import az.fitnest.catalog.model.entity.Profession;
import az.fitnest.catalog.model.entity.Trainer;
import az.fitnest.catalog.repository.GymRepository;
import az.fitnest.catalog.repository.ProfessionRepository;
import az.fitnest.catalog.repository.TrainerRepository;
import az.fitnest.catalog.repository.GymLessonTypeRepository;
import az.fitnest.catalog.repository.TrainerReservationDateRepository;
import az.fitnest.catalog.service.FileStorageService;
import az.fitnest.catalog.model.entity.GymLessonType;
import az.fitnest.catalog.service.TranslationService;
import az.fitnest.catalog.util.UserContext;
import az.fitnest.user.grpc.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GymTrainerService {

    private final GymRepository gymRepository;
    private final TrainerRepository trainerRepository;
    private final ProfessionRepository professionRepository;
    private final FileStorageService fileStorageService;
    private final TranslationService translationService;
    private final UserServiceGrpcClient userServiceGrpcClient;
    private final TrainerReservationDateRepository trainerReservationDateRepository;
    private final GymLessonTypeRepository gymLessonTypeRepository;

    @Transactional(readOnly = true)
    public PaginatedResponse<GymTrainerDto> getTrainers(Long gymId, int page, int pageSize, String sortDir) {
        if (!gymRepository.existsById(gymId)) {
            throw new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found");
        }
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Page<Trainer> trainerPage = trainerRepository.findByGymId(gymId, pageable(page, pageSize, Sort.by(direction, "id")));
        String userLanguage = resolveUserLanguage();
        List<GymTrainerDto> items = trainerPage.getContent().stream()
                .map(t -> toGymTrainerDto(t, userLanguage))
                .collect(Collectors.toList());

        return PaginatedResponse.<GymTrainerDto>builder()
                .items(items)
                .total(trainerPage.getTotalElements())
                .page(page)
                .pageSize(pageSize)
                .build();
    }

    @Transactional
    @CacheEvict(cacheNames = "gyms", key = "#gymId")
    public void addTrainer(Long gymId, TrainerRequest request) {
        az.fitnest.catalog.model.entity.Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));

        Trainer trainer = new Trainer();
        updateTrainerFromRequest(trainer, request);

        gym.getTrainers().add(trainer);
        gymRepository.save(gym);
    }

    @Transactional
    @CacheEvict(cacheNames = "gyms", key = "#gymId")
    public void updateTrainer(Long gymId, Long trainerId, TrainerRequest request) {
        Trainer trainer = trainerRepository.findById(trainerId)
                .orElseThrow(() -> new ResourceNotFoundException("TRAINER_NOT_FOUND", "error.trainer_not_found"));
        if (!gymId.equals(trainer.getGymId())) {
            throw new ResourceNotFoundException("TRAINER_NOT_FOUND", "error.trainer_not_found");
        }

        updateTrainerFromRequest(trainer, request);
        trainerRepository.save(trainer);
    }

    @Transactional
    @CacheEvict(cacheNames = "gyms", key = "#gymId")
    public void deleteTrainer(Long gymId, Long trainerId) {
        Trainer trainerToDelete = trainerRepository.findById(trainerId)
                .orElseThrow(() -> new ResourceNotFoundException("TRAINER_NOT_FOUND", "error.trainer_not_found"));
        if (!gymId.equals(trainerToDelete.getGymId())) {
            throw new ResourceNotFoundException("TRAINER_NOT_FOUND", "error.trainer_not_found");
        }

        if (trainerToDelete.getPicture() != null && !trainerToDelete.getPicture().isBlank()) {
            safeDeleteFile(trainerToDelete.getPicture());
        }
        trainerRepository.delete(trainerToDelete);
    }

    private void updateTrainerFromRequest(Trainer trainer, TrainerRequest request) {
        trainer.setName(request.name());
        trainer.setSurname(request.surname());

        Profession profession = professionRepository.findById(request.professionId())
                .orElseThrow(() -> new ResourceNotFoundException("PROFESSION_NOT_FOUND", "error.profession_not_found"));
        trainer.setProfession(profession);

        trainer.setPhone(request.phone());
        trainer.setEmail(request.email());
    }

    @Transactional
    @CacheEvict(cacheNames = "gyms", key = "#gymId")
    public void updateTrainerPhoto(Long gymId, Long trainerId, MultipartFile file) {
        Trainer trainer = trainerRepository.findById(trainerId)
                .orElseThrow(() -> new ResourceNotFoundException("TRAINER_NOT_FOUND", "error.trainer_not_found"));
        if (!gymId.equals(trainer.getGymId())) {
            throw new ResourceNotFoundException("TRAINER_NOT_FOUND", "error.trainer_not_found");
        }

        String fsId = fileStorageService.saveFile(file, "/trainers", trainer.getPicture());
        trainer.setPicture("/api/v1/media/stream/" + fsId);
        trainerRepository.save(trainer);
    }

    private void safeDeleteFile(String url) {
        try {
            fileStorageService.deleteFile(url);
        } catch (Exception e) {
        }
    }

    private GymTrainerDto toGymTrainerDto(Trainer t, String language) {
        az.fitnest.catalog.dto.ProfessionDto professionDto = null;
        if (t.getProfession() != null) {
            String localizedName = translationService.getTranslatedValue("PROFESSION", String.valueOf(t.getProfession().getId()), "name", language);
            if (localizedName == null || localizedName.isEmpty()) {
                localizedName = t.getProfession().getName();
            }
            professionDto = az.fitnest.catalog.dto.ProfessionDto.builder()
                    .id(t.getProfession().getId())
                    .name(localizedName)
                    .build();
        }

        return GymTrainerDto.builder()
                .trainer_id(t.getId() != null ? t.getId().toString() : null)
                .name(t.getName())
                .surname(t.getSurname())
                .profession(professionDto)
                .picture(t.getPicture())
                .phone(t.getPhone())
                .email(t.getEmail())
                .build();
    }

    private Pageable pageable(int page, int size, Sort sort) {
        int safePage = Math.max(page, 1) - 1;
        int safeSize = Math.max(1, Math.min(size, 100));
        return PageRequest.of(safePage, safeSize, sort);
    }

    private String resolveUserLanguage() {
        Long userId = UserContext.getCurrentUserId();
        return resolveUserLanguage(userId);
    }

    private String resolveUserLanguage(Long userId) {
        if (userId != null) {
            try {
                UserResponse user = userServiceGrpcClient.getUserById(userId);
                if (user != null && user.getLanguage() != null && !user.getLanguage().isEmpty()) {
                    return user.getLanguage();
                }
            } catch (Exception ignored) {
            }
        }
        return "AZ";
    }

    @Transactional
    public void toggleTrainerReservation(Long gymId, Long trainerId, boolean enabled, Long lessonId) {
        Trainer trainer = trainerRepository.findById(trainerId)
                .orElseThrow(() -> new ResourceNotFoundException("TRAINER_NOT_FOUND", "error.trainer_not_found"));
        if (!gymId.equals(trainer.getGymId())) {
            throw new ResourceNotFoundException("TRAINER_NOT_FOUND", "error.trainer_not_found");
        }

        if (lessonId != null) {
            GymLessonType lesson = gymLessonTypeRepository.findById(lessonId)
                    .orElseThrow(() -> new ResourceNotFoundException("LESSON_TYPE_NOT_FOUND", "error.lesson_type_not_found"));
            if (enabled) {
                trainer.getEnabledLessonTypes().add(lesson);
            } else {
                trainer.getEnabledLessonTypes().remove(lesson);
            }
        }

        trainer.setIsReservationEnabled(enabled);
        trainerRepository.save(trainer);
    }

    @Transactional
    public void addTrainerAvailability(Long gymId, Long trainerId, Long lessonId, az.fitnest.catalog.dto.TrainerAvailabilityRequest request) {
        Trainer trainer = trainerRepository.findById(trainerId)
                .orElseThrow(() -> new ResourceNotFoundException("TRAINER_NOT_FOUND", "error.trainer_not_found"));
        if (!gymId.equals(trainer.getGymId())) {
            throw new ResourceNotFoundException("TRAINER_NOT_FOUND", "error.trainer_not_found");
        }

        GymLessonType lessonType = gymLessonTypeRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("LESSON_TYPE_NOT_FOUND", "error.lesson_type_not_found"));

        if (request.getStartTime().isAfter(request.getEndTime()) || request.getStartTime().equals(request.getEndTime())) {
            throw new az.fitnest.catalog.exception.BadRequestException("INVALID_TIME_RANGE", "error.invalid_time_range");
        }

        if (trainerReservationDateRepository.existsOverlappingAvailability(trainerId, request.getDate(), request.getStartTime(), request.getEndTime())) {
            throw new az.fitnest.catalog.exception.BadRequestException("TRAINER_AVAILABILITY_OVERLAP", "error.trainer_availability_overlap");
        }

        az.fitnest.catalog.model.entity.TrainerReservationDate availability = az.fitnest.catalog.model.entity.TrainerReservationDate.builder()
                .trainer(trainer)
                .classType(lessonType)
                .gymId(gymId)
                .date(request.getDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .emptySpaces(request.getEmptySpaces())
                .build();
        trainerReservationDateRepository.save(availability);
    }
}
