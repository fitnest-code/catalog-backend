package az.fitnest.catalog.service.impl;

import az.fitnest.catalog.client.UserServiceGrpcClient;
import az.fitnest.catalog.dto.*;
import az.fitnest.catalog.dto.request.*;
import az.fitnest.catalog.dto.response.*;
import az.fitnest.catalog.dto.response.GymTrainerResponse;
import az.fitnest.catalog.dto.PaginatedResponse;
import az.fitnest.catalog.dto.request.TrainerRequest;
import az.fitnest.catalog.dto.response.ProfessionResponse;
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
public class GymTrainerServiceImpl implements az.fitnest.catalog.service.GymTrainerService {

    private final GymRepository gymRepository;
    private final TrainerRepository trainerRepository;
    private final ProfessionRepository professionRepository;
    private final FileStorageService fileStorageService;
    private final TranslationService translationService;
    private final UserServiceGrpcClient userServiceGrpcClient;
    private final TrainerReservationDateRepository trainerReservationDateRepository;
    private final GymLessonTypeRepository gymLessonTypeRepository;

    @Transactional(readOnly = true)
    public PaginatedResponse<GymTrainerResponse> getTrainers(Long gymId, int page, int pageSize, String sortDir) {
        if (!gymRepository.existsById(gymId)) {
            throw new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found");
        }
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Page<Trainer> trainerPage = trainerRepository.findByGymId(gymId, pageable(page, pageSize, Sort.by(direction, "id")));
        String userLanguage = resolveUserLanguage();
        List<GymTrainerResponse> items = trainerPage.getContent().stream()
                .map(t -> toGymTrainerDto(t, userLanguage))
                .collect(Collectors.toList());

        return PaginatedResponse.<GymTrainerResponse>builder()
                .items(items)
                .total(trainerPage.getTotalElements())
                .page(page)
                .pageSize(pageSize)
                .build();
    }

    @Transactional
    @CacheEvict(cacheNames = {"gym-detail", "main-page-gyms", "admin-gyms"}, allEntries = true)
    public void addTrainer(Long gymId, TrainerRequest request) {
        az.fitnest.catalog.model.entity.Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));

        Trainer trainer = new Trainer();
        updateTrainerFromRequest(trainer, request);

        gym.getTrainers().add(trainer);
        gymRepository.save(gym);
    }

    @Transactional
    @CacheEvict(cacheNames = {"gym-detail", "main-page-gyms", "admin-gyms"}, allEntries = true)
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
    @CacheEvict(cacheNames = {"gym-detail", "main-page-gyms", "admin-gyms"}, allEntries = true)
    public void deleteTrainer(Long gymId, Long trainerId) {
        Trainer trainerToDelete = trainerRepository.findById(trainerId)
                .orElseThrow(() -> new ResourceNotFoundException("TRAINER_NOT_FOUND", "error.trainer_not_found"));
        if (!gymId.equals(trainerToDelete.getGymId())) {
            throw new ResourceNotFoundException("TRAINER_NOT_FOUND", "error.trainer_not_found");
        }

        if (trainerToDelete.getPicture() != null && !trainerToDelete.getPicture().isBlank()) {
            fileStorageService.deleteFilesAfterCommit(java.util.List.of(trainerToDelete.getPicture()));
        }
        trainerRepository.delete(trainerToDelete);
    }

    private void updateTrainerFromRequest(Trainer trainer, TrainerRequest request) {
        trainer.setName(request.name());
        trainer.setSurname(request.surname());

        Profession profession = professionRepository.findById(request.professionId())
                .orElseThrow(() -> new ResourceNotFoundException("PROFESSION_NOT_FOUND", "error.profession_not_found"));
        trainer.setProfession(profession);

        trainer.setPhone(az.fitnest.catalog.util.PhoneUtil.normalize(request.phone()));
        trainer.setEmail(request.email());
    }

    public void updateTrainerPhoto(Long gymId, Long trainerId, MultipartFile file) {
        MultipartFile validatedFile = fileStorageService.validateAndWrapImage(file);

        Trainer trainer = trainerRepository.findById(trainerId)
                .orElseThrow(() -> new ResourceNotFoundException("TRAINER_NOT_FOUND", "error.trainer_not_found"));
        if (!gymId.equals(trainer.getGymId())) {
            throw new ResourceNotFoundException("TRAINER_NOT_FOUND", "error.trainer_not_found");
        }

        String fsId = fileStorageService.saveFile(validatedFile, "/trainers", trainer.getPicture());
        String newUrl = "/api/v1/media/stream/" + fsId;

        updateTrainerPhotoInternal(trainerId, newUrl);
    }

    @Transactional
    @CacheEvict(cacheNames = {"gym-detail", "main-page-gyms", "admin-gyms"}, allEntries = true)
    protected void updateTrainerPhotoInternal(Long trainerId, String newUrl) {
        Trainer trainer = trainerRepository.findById(trainerId).get();
        trainer.setPicture(newUrl);
        trainerRepository.save(trainer);
    }

    private GymTrainerResponse toGymTrainerDto(Trainer t, String language) {
        ProfessionResponse professionDto = null;
        if (t.getProfession() != null) {
            String localizedName = translationService.getTranslatedValue("PROFESSION", String.valueOf(t.getProfession().getId()), "name", language);
            if (localizedName == null || localizedName.isEmpty()) {
                localizedName = t.getProfession().getName();
            }
            professionDto = ProfessionResponse.builder()
                    .id(t.getProfession().getId())
                    .name(localizedName)
                    .build();
        }

        return GymTrainerResponse.builder()
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
    @CacheEvict(cacheNames = {"gym-detail", "main-page-gyms", "admin-gyms"}, allEntries = true)
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
    public void addTrainerAvailability(Long gymId, Long trainerId, Long lessonId, az.fitnest.catalog.dto.request.TrainerAvailabilityRequest request) {
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

    public void addTrainers(Long gymId, List<String> names, List<String> surnames, List<Long> professionIds,
                           List<String> emails, List<String> phones, List<MultipartFile> photos) {
        if (names == null) return;

        java.util.List<String> photoUrls = new java.util.ArrayList<>();
        if (photos != null) {
            for (MultipartFile photo : photos) {
                if (photo != null && !photo.isEmpty()) {
                    MultipartFile validated = fileStorageService.validateAndWrapImage(photo);
                    String fsId = fileStorageService.saveFile(validated, "/trainers");
                    photoUrls.add("/api/v1/media/stream/" + fsId);
                } else {
                    photoUrls.add(null);
                }
            }
        }

        addTrainersInternal(gymId, names, surnames, professionIds, emails, phones, photoUrls);
    }

    @Transactional
    @CacheEvict(cacheNames = {"gym-detail", "main-page-gyms", "admin-gyms"}, allEntries = true)
    protected void addTrainersInternal(Long gymId, List<String> names, List<String> surnames, List<Long> professionIds,
                                      List<String> emails, List<String> phones, List<String> photoUrls) {
        az.fitnest.catalog.model.entity.Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));

        for (int i = 0; i < names.size(); i++) {
            Trainer trainer = new Trainer();
            trainer.setName(names.get(i));

            if (surnames != null && i < surnames.size()) {
                trainer.setSurname(surnames.get(i));
            }
            if (emails != null && i < emails.size()) {
                trainer.setEmail(emails.get(i));
            }
            if (phones != null && i < phones.size()) {
                trainer.setPhone(az.fitnest.catalog.util.PhoneUtil.normalize(phones.get(i)));
            }

            if (professionIds != null && i < professionIds.size()) {
                Profession profession = professionRepository.findById(professionIds.get(i))
                        .orElseThrow(() -> new ResourceNotFoundException("PROFESSION_NOT_FOUND", "error.profession_not_found"));
                trainer.setProfession(profession);
            }

            if (photoUrls != null && i < photoUrls.size()) {
                trainer.setPicture(photoUrls.get(i));
            }

            gym.getTrainers().add(trainer);
        }
        gymRepository.save(gym);
    }
}
