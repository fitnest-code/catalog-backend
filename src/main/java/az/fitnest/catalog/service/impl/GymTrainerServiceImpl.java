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
import az.fitnest.catalog.client.CachedUser;
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
    private final az.fitnest.catalog.repository.LessonTypeRepository lessonTypeRepository;
    private final java.util.concurrent.Executor imageUploadExecutor;

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

    @Transactional(readOnly = true)
    @Override
    public PaginatedResponse<GymTrainerResponseV2> getTrainersV2(Long gymId, int page, int pageSize, String sortDir) {
        if (!gymRepository.existsById(gymId)) {
            throw new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found");
        }
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Page<Trainer> trainerPage = trainerRepository.findByGymId(gymId, pageable(page, pageSize, Sort.by(direction, "id")));
        String userLanguage = resolveUserLanguage();
        List<GymTrainerResponseV2> items = trainerPage.getContent().stream()
                .map(t -> toGymTrainerDtoV2(t, userLanguage))
                .collect(Collectors.toList());

        return PaginatedResponse.<GymTrainerResponseV2>builder()
                .items(items)
                .total(trainerPage.getTotalElements())
                .page(page)
                .pageSize(pageSize)
                .build();
    }

    @Transactional
    @CacheEvict(cacheNames = {"gym-detail", "main-page-gyms", "gym-listings", "admin-gyms"}, allEntries = true)
    public void addTrainer(Long gymId, TrainerRequest request, MultipartFile photo) {
        az.fitnest.catalog.model.entity.Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));

        Trainer trainer = new Trainer();
        updateTrainerFromRequest(gymId, trainer, request);

        if (photo != null && !photo.isEmpty()) {
            MultipartFile validated = fileStorageService.validateAndWrapImage(photo);
            trainer.setPicture(fileStorageService.saveFile(validated, "/trainers"));
        }

        gym.getTrainers().add(trainer);
        gymRepository.save(gym);

        if (trainer.getId() != null) {
            translationService.autoTranslateAndSave("Trainer", trainer.getId().toString(), "name", trainer.getName());
            if (trainer.getSurname() != null) {
                translationService.autoTranslateAndSave("Trainer", trainer.getId().toString(), "surname", trainer.getSurname());
            }
        }
    }

    @Transactional
    @CacheEvict(cacheNames = {"gym-detail", "main-page-gyms", "gym-listings", "admin-gyms"}, allEntries = true)
    public void updateTrainer(Long gymId, Long trainerId, TrainerRequest request, MultipartFile photo) {
        Trainer trainer = trainerRepository.findById(trainerId)
                .orElseThrow(() -> new ResourceNotFoundException("TRAINER_NOT_FOUND", "error.trainer_not_found"));
        if (!gymId.equals(trainer.getGymId())) {
            throw new ResourceNotFoundException("TRAINER_NOT_FOUND", "error.trainer_not_found");
        }

        updateTrainerFromRequest(gymId, trainer, request);

        if (photo != null && !photo.isEmpty()) {
            MultipartFile validated = fileStorageService.validateAndWrapImage(photo);
            trainer.setPicture(fileStorageService.saveFile(validated, "/trainers", trainer.getPicture()));
        }

        trainerRepository.save(trainer);

        translationService.autoTranslateAndSave("Trainer", trainer.getId().toString(), "name", trainer.getName());
        if (trainer.getSurname() != null) {
            translationService.autoTranslateAndSave("Trainer", trainer.getId().toString(), "surname", trainer.getSurname());
        }
    }

    @Transactional
    @CacheEvict(cacheNames = {"gym-detail", "main-page-gyms", "gym-listings", "admin-gyms"}, allEntries = true)
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

    private void updateTrainerFromRequest(Long gymId, Trainer trainer, TrainerRequest request) {
        trainer.setName(request.name());
        trainer.setSurname(request.surname());

        if (request.professionId() != null) {
            Profession profession = professionRepository.findById(request.professionId())
                    .orElseThrow(() -> new ResourceNotFoundException("PROFESSION_NOT_FOUND", "error.profession_not_found"));
            trainer.setProfession(profession);
        } else {
            trainer.setProfession(null);
        }

        trainer.setPhone(az.fitnest.catalog.util.PhoneUtil.normalize(request.phone()));
        trainer.setEmail(request.email());

        if (request.lessonTypeIds() != null) {
            trainer.getEnabledLessonTypes().clear();
            trainer.getEnabledLessonTypes().addAll(resolveGymLessonTypes(gymId, request.lessonTypeIds()));
        }
    }

    /**
     * Lesson types selected in the UI come from the gym's category catalog (global LessonType ids),
     * not from previously-saved GymLessonType rows, which may not exist yet for this gym. Reuse an
     * existing GymLessonType by name if one is already saved for this gym (also accepts a raw
     * GymLessonType id for backward compatibility), otherwise create one from the matching category's
     * LessonType so the trainer can be linked to it.
     */
    private List<GymLessonType> resolveGymLessonTypes(Long gymId, List<Long> lessonTypeIds) {
        if (lessonTypeIds.isEmpty()) {
            return List.of();
        }

        az.fitnest.catalog.model.entity.Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));

        List<GymLessonType> existingGymLessons = gymLessonTypeRepository.findByGymId(gymId);
        java.util.Map<Long, GymLessonType> byId = existingGymLessons.stream()
                .collect(Collectors.toMap(GymLessonType::getId, gl -> gl));
        java.util.Map<String, GymLessonType> byName = existingGymLessons.stream()
                .collect(Collectors.toMap(gl -> gl.getName().toLowerCase(), gl -> gl, (a, b) -> a));

        java.util.Set<az.fitnest.catalog.model.entity.Category> gymCategories = new java.util.HashSet<>();
        if (gym.getMainCategories() != null) {
            gymCategories.addAll(gym.getMainCategories());
        }
        if (gym.getSubCategories() != null) {
            gymCategories.addAll(gym.getSubCategories());
        }

        java.util.Map<Long, az.fitnest.catalog.model.entity.LessonType> globalById = lessonTypeRepository.findAllById(lessonTypeIds).stream()
                .collect(Collectors.toMap(az.fitnest.catalog.model.entity.LessonType::getId, lt -> lt));

        List<GymLessonType> resolved = new java.util.ArrayList<>();
        for (Long id : lessonTypeIds) {
            GymLessonType existing = byId.get(id);
            if (existing != null) {
                resolved.add(existing);
                continue;
            }

            az.fitnest.catalog.model.entity.LessonType global = globalById.get(id);
            if (global == null) {
                continue;
            }

            GymLessonType byNameMatch = byName.get(global.getName().toLowerCase());
            if (byNameMatch != null) {
                resolved.add(byNameMatch);
                continue;
            }

            az.fitnest.catalog.model.entity.Category owningCategory = gymCategories.stream()
                    .filter(c -> c.getLessonTypes() != null && c.getLessonTypes().contains(global))
                    .findFirst()
                    .orElse(null);
            GymLessonType created = gymLessonTypeRepository.save(GymLessonType.builder()
                    .gym(gym)
                    .name(global.getName())
                    .category(owningCategory)
                    .status("ACTIVE")
                    .sortOrder(0)
                    .build());
            byName.put(created.getName().toLowerCase(), created);
            resolved.add(created);
        }
        return resolved;
    }

    public void updateTrainerPhoto(Long gymId, Long trainerId, MultipartFile file) {
        MultipartFile validatedFile = fileStorageService.validateAndWrapImage(file);

        Trainer trainer = trainerRepository.findById(trainerId)
                .orElseThrow(() -> new ResourceNotFoundException("TRAINER_NOT_FOUND", "error.trainer_not_found"));
        if (!gymId.equals(trainer.getGymId())) {
            throw new ResourceNotFoundException("TRAINER_NOT_FOUND", "error.trainer_not_found");
        }

        String newUrl = fileStorageService.saveFile(validatedFile, "/trainers", trainer.getPicture());
        updateTrainerPhotoInternal(trainerId, newUrl);
    }

    @Transactional
    @CacheEvict(cacheNames = {"gym-detail", "main-page-gyms", "gym-listings", "admin-gyms"}, allEntries = true)
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

        java.util.List<Long> lessonTypeIds = new java.util.ArrayList<>();
        if (t.getEnabledLessonTypes() != null) {
            java.util.List<az.fitnest.catalog.model.entity.LessonType> globalLessons = lessonTypeRepository.findAll();
            for (az.fitnest.catalog.model.entity.GymLessonType gl : t.getEnabledLessonTypes()) {
                globalLessons.stream()
                    .filter(glt -> glt.getName().equalsIgnoreCase(gl.getName()))
                    .findFirst()
                    .ifPresentOrElse(
                        glt -> lessonTypeIds.add(glt.getId()),
                        () -> lessonTypeIds.add(gl.getId())
                    );
            }
        }

        String localizedName = translationService.getTranslatedValue("Trainer", String.valueOf(t.getId()), "name", language);
        if (localizedName == null || localizedName.isEmpty()) {
            localizedName = t.getName();
        }
        String localizedSurname = translationService.getTranslatedValue("Trainer", String.valueOf(t.getId()), "surname", language);
        if (localizedSurname == null || localizedSurname.isEmpty()) {
            localizedSurname = t.getSurname();
        }

        return GymTrainerResponse.builder()
                .trainer_id(t.getId() != null ? t.getId().toString() : null)
                .name(localizedName)
                .surname(localizedSurname)
                .profession(professionDto)
                .picture(t.getPicture())
                .phone(t.getPhone())
                .email(t.getEmail())
                .lessonTypeIds(lessonTypeIds)
                .build();
    }

    private GymTrainerResponseV2 toGymTrainerDtoV2(Trainer t, String language) {
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

        java.util.List<Long> lessonTypeIds = new java.util.ArrayList<>();
        java.util.List<Long> categoryIds = new java.util.ArrayList<>();
        if (t.getEnabledLessonTypes() != null) {
            java.util.List<az.fitnest.catalog.model.entity.LessonType> globalLessons = lessonTypeRepository.findAll();
            for (az.fitnest.catalog.model.entity.GymLessonType gl : t.getEnabledLessonTypes()) {
                globalLessons.stream()
                    .filter(glt -> glt.getName().equalsIgnoreCase(gl.getName()))
                    .findFirst()
                    .ifPresentOrElse(
                        glt -> lessonTypeIds.add(glt.getId()),
                        () -> lessonTypeIds.add(gl.getId())
                    );
                if (gl.getCategory() != null) {
                    categoryIds.add(gl.getCategory().getId());
                }
            }
        }

        java.util.List<Long> distinctCategoryIds = categoryIds.stream().distinct().collect(Collectors.toList());

        String localizedName = translationService.getTranslatedValue("Trainer", String.valueOf(t.getId()), "name", language);
        if (localizedName == null || localizedName.isEmpty()) {
            localizedName = t.getName();
        }
        String localizedSurname = translationService.getTranslatedValue("Trainer", String.valueOf(t.getId()), "surname", language);
        if (localizedSurname == null || localizedSurname.isEmpty()) {
            localizedSurname = t.getSurname();
        }

        return GymTrainerResponseV2.builder()
                .trainer_id(t.getId() != null ? t.getId().toString() : null)
                .name(localizedName)
                .surname(localizedSurname)
                .profession(professionDto)
                .picture(t.getPicture())
                .phone(t.getPhone())
                .email(t.getEmail())
                .lessonTypeIds(lessonTypeIds)
                .categoryIds(distinctCategoryIds)
                .build();
    }

    private Pageable pageable(int page, int size, Sort sort) {
        int safePage = Math.max(page, 1) - 1;
        int safeSize = Math.max(1, Math.min(size, 100));
        return PageRequest.of(safePage, safeSize, sort);
    }

    private String resolveUserLanguage() {
        return az.fitnest.catalog.util.UserContext.getUserLanguage();
    }

    private String resolveUserLanguage(Long userId) {
        return az.fitnest.catalog.util.UserContext.getUserLanguage();
    }

    @Transactional
    @CacheEvict(cacheNames = {"gym-detail", "main-page-gyms", "gym-listings", "admin-gyms"}, allEntries = true)
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
                           List<String> emails, List<String> phones, List<MultipartFile> photos, List<String> lessonTypesPerTrainer) {
        if (names == null) return;

        List<java.util.concurrent.CompletableFuture<String>> futures = new java.util.ArrayList<>();
        if (photos != null) {
            for (MultipartFile photo : photos) {
                if (photo != null && !photo.isEmpty()) {
                    MultipartFile validated = fileStorageService.validateAndWrapImage(photo);
                    futures.add(java.util.concurrent.CompletableFuture.supplyAsync(() -> 
                        fileStorageService.saveFile(validated, "/trainers"), 
                        imageUploadExecutor
                    ));
                } else {
                    futures.add(java.util.concurrent.CompletableFuture.completedFuture(null));
                }
            }
        }

        List<String> photoUrls = futures.stream()
                .map(java.util.concurrent.CompletableFuture::join)
                .toList();

        addTrainersInternal(gymId, names, surnames, professionIds, emails, phones, photoUrls, lessonTypesPerTrainer);
    }

    @Transactional
    @CacheEvict(cacheNames = {"gym-detail", "main-page-gyms", "gym-listings", "admin-gyms"}, allEntries = true)
    protected void addTrainersInternal(Long gymId, List<String> names, List<String> surnames, List<Long> professionIds,
                                      List<String> emails, List<String> phones, List<String> photoUrls, List<String> lessonTypesPerTrainer) {
        az.fitnest.catalog.model.entity.Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));

        List<GymLessonType> gymLessons = gymLessonTypeRepository.findByGymId(gymId);

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

            if (professionIds != null && i < professionIds.size() && professionIds.get(i) != null && professionIds.get(i) > 0) {
                Profession profession = professionRepository.findById(professionIds.get(i))
                        .orElseThrow(() -> new ResourceNotFoundException("PROFESSION_NOT_FOUND", "error.profession_not_found"));
                trainer.setProfession(profession);
            }

            if (photoUrls != null && i < photoUrls.size()) {
                trainer.setPicture(photoUrls.get(i));
            }

            if (lessonTypesPerTrainer != null && i < lessonTypesPerTrainer.size()) {
                String str = lessonTypesPerTrainer.get(i);
                if (str != null && !str.isBlank()) {
                    for (String item : str.split(",")) {
                        String trimmed = item.trim();
                        if (trimmed.isEmpty()) continue;
                        gymLessons.stream()
                            .filter(gl -> trimmed.equalsIgnoreCase(gl.getName()) || trimmed.equals(String.valueOf(gl.getId())))
                            .findFirst()
                            .ifPresent(trainer.getEnabledLessonTypes()::add);
                    }
                }
            }

            gym.getTrainers().add(trainer);
        }
        gymRepository.save(gym);

        for (Trainer trainer : gym.getTrainers()) {
            if (trainer.getId() != null) {
                translationService.autoTranslateAndSave("Trainer", trainer.getId().toString(), "name", trainer.getName());
                if (trainer.getSurname() != null) {
                    translationService.autoTranslateAndSave("Trainer", trainer.getId().toString(), "surname", trainer.getSurname());
                }
            }
        }
    }

    @Override
    public void addTrainersWithUrls(Long gymId, List<String> names, List<String> surnames, List<Long> professionIds,
                                    List<String> emails, List<String> phones, List<String> photoUrls, List<String> lessonTypesPerTrainer) {
        addTrainersInternal(gymId, names, surnames, professionIds, emails, phones, photoUrls, lessonTypesPerTrainer);
    }
}
