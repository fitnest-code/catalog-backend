package az.fitnest.catalog.service.impl;

import az.fitnest.catalog.client.IdentityServiceGrpcClient;
import az.fitnest.catalog.client.OrderServiceGrpcClient;
import az.fitnest.catalog.dto.request.*;
import az.fitnest.catalog.dto.response.*;
import az.fitnest.catalog.exception.BadRequestException;
import az.fitnest.catalog.exception.ForbiddenException;
import az.fitnest.catalog.exception.ResourceNotFoundException;
import az.fitnest.catalog.model.entity.*;
import az.fitnest.catalog.model.enums.GymStatus;
import az.fitnest.catalog.model.enums.GymWorkHourPeriod;
import az.fitnest.catalog.repository.*;
import az.fitnest.catalog.service.*;
import az.fitnest.catalog.util.PhoneUtil;
import az.fitnest.catalog.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
public class GymWriteServiceImpl implements GymWriteService {
    private final GymRepository gymRepository;
    private final SavedGymRepository savedGymRepository;
    private final CategoryRepository categoryRepository;
    private final ReverseGeocodingService reverseGeocodingService;
    private final FileStorageService fileStorageService;
    private final OrderServiceGrpcClient orderServiceGrpcClient;
    private final GymImageRepository gymImageRepository;
    private final SupportedServiceRepository supportedServiceRepository;
    private final IdentityServiceGrpcClient identityServiceGrpcClient;
    private final TrainerRepository trainerRepository;
    private final GymLessonTypeRepository gymLessonTypeRepository;
    private final az.fitnest.catalog.repository.LessonTypeRepository lessonTypeRepository;
    private final TrainerReservationDateRepository trainerReservationDateRepository;
    private final GymAdminRepository gymAdminRepository;
    private final ReservationRepository reservationRepository;
    private final GymTrainerService gymTrainerService;
    private final GymQrCodeService gymQrCodeService;
    private final GymEntranceHistoryRepository gymEntranceHistoryRepository;
    private final az.fitnest.catalog.service.TranslationService translationService;
    private final Executor imageUploadExecutor;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private record RoomImageUploadResult(String roomName, String url) {}
    private record GymAdminCreateResult(GymAdminCreateRequest req, Long userId) {}

    @Caching(evict = {
        @CacheEvict(cacheNames = "main-page-gyms", allEntries = true),
        @CacheEvict(cacheNames = "admin-gyms", allEntries = true)
    })
    public void createGym(GymRequest request) {
        GeocodingResponse geocoding = reverseGeocodingService.reverseGeocode(request.address().latitude(), request.address().longitude());
        saveGymInternal(request, geocoding);
    }

    @Transactional
    protected void saveGymInternal(GymRequest request, GeocodingResponse geocoding) {
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new BadRequestException("INVALID_CATEGORY", "error.invalid_category"));
        Gym gym = new Gym();
        gym.setName(request.name());
        gym.setDescription(request.description());

        Address address = new Address();
        address.setLatitude(request.address().latitude());
        address.setLongitude(request.address().longitude());
        if (geocoding != null) {
            address.setAddressText(geocoding.addressText());
            address.setCity(geocoding.city());
        }
        gym.setAddress(address);

        gym.setPhone(PhoneUtil.normalize(request.phone()));
        gym.setEmail(request.email() != null && request.email().isBlank() ? null : request.email());
        gym.setCategory(category);

        updateWorkHours(gym.getGeneralWorkHours(), request.generalWorkHours());
        updateWorkHours(gym.getWorkHoursWoman(), request.workHoursWoman());
        updateWorkHours(gym.getWorkHoursMan(), request.workHoursMan());

        if (request.restDays() != null) {
            Set<GymWorkHourPeriod> restDays = request.restDays().stream()
                    .flatMap(r -> az.fitnest.catalog.mapper.GymMapper.expandPeriods(r.period()).stream())
                    .collect(java.util.stream.Collectors.toSet());

            validateNoWorkHoursOnRestDays(request.generalWorkHours(), restDays, "general");
            validateNoWorkHoursOnRestDays(request.workHoursWoman(), restDays, "woman");
            validateNoWorkHoursOnRestDays(request.workHoursMan(), restDays, "man");

            gym.setRestDays(restDays);
        }

        gym.setStatus(request.status() != null ? request.status() : GymStatus.ACTIVE);

        Gym saved = gymRepository.save(gym);

        TransactionSynchronizationManager.registerSynchronization(
            new org.springframework.transaction.support.TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    gymQrCodeService.generateAndSaveQrCode(saved.getId());
                }
            }
        );
    }

    @Caching(evict = {
        @CacheEvict(cacheNames = "gym-detail", key = "#gymId"),
        @CacheEvict(cacheNames = "admin-gyms", allEntries = true),
        @CacheEvict(cacheNames = "main-page-gyms", allEntries = true)
    })
    public void updateGym(Long gymId, GymRequest request) {
        GeocodingResponse geocoding = reverseGeocodingService.reverseGeocode(request.address().latitude(), request.address().longitude());
        updateGymInternal(gymId, request, geocoding);
    }

    @Transactional
    protected void updateGymInternal(Long gymId, GymRequest request, GeocodingResponse geocoding) {
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new BadRequestException("INVALID_CATEGORY", "error.invalid_category"));

        Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));

        gym.setName(request.name());
        gym.setDescription(request.description());

        Address address = gym.getAddress();
        if (address == null) {
            address = new Address();
            gym.setAddress(address);
        }
        address.setLatitude(request.address().latitude());
        address.setLongitude(request.address().longitude());
        if (geocoding != null) {
            address.setAddressText(geocoding.addressText());
            address.setCity(geocoding.city());
        }

        gym.setPhone(PhoneUtil.normalize(request.phone()));
        gym.setEmail(request.email() != null && request.email().isBlank() ? null : request.email());
        gym.setCategory(category);

        updateWorkHours(gym.getGeneralWorkHours(), request.generalWorkHours());
        updateWorkHours(gym.getWorkHoursWoman(), request.workHoursWoman());
        updateWorkHours(gym.getWorkHoursMan(), request.workHoursMan());

        if (request.restDays() != null) {
            Set<GymWorkHourPeriod> restDays = request.restDays().stream()
                    .flatMap(r -> az.fitnest.catalog.mapper.GymMapper.expandPeriods(r.period()).stream())
                    .collect(java.util.stream.Collectors.toSet());

            validateNoWorkHoursOnRestDays(request.generalWorkHours(), restDays, "general");
            validateNoWorkHoursOnRestDays(request.workHoursWoman(), restDays, "woman");
            validateNoWorkHoursOnRestDays(request.workHoursMan(), restDays, "man");

            gym.getRestDays().clear();
            gym.getRestDays().addAll(restDays);
        }

        gym.setStatus(request.status() != null ? request.status() : GymStatus.ACTIVE);

        gymRepository.save(gym);

        translationService.autoTranslateAndSave("GYM", gym.getId().toString(), "name", request.name());
        translationService.autoTranslateAndSave("GYM", gym.getId().toString(), "description", request.description());
        if (gym.getAddress() != null) {
            translationService.autoTranslateAndSave("GYM", gym.getId().toString(), "addressText", gym.getAddress().getAddressText());
            translationService.autoTranslateAndSave("GYM", gym.getId().toString(), "city", gym.getAddress().getCity());
        }
    }

    @Transactional
    @CacheEvict(cacheNames = "gym-detail", key = "#gymId")
    public void enableGymSubscription(Long gymId, Long subscriptionId) {
        Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));
        if (!orderServiceGrpcClient.checkPackageExists(subscriptionId)) {
            throw new BadRequestException("PACKAGE_NOT_FOUND", "error.package_not_found");
        }
        gym.getSubscriptions().removeIf(s -> s.getPackageId() != null && s.getPackageId().equals(subscriptionId));
        GymSubscription subscription = new GymSubscription();
        subscription.setGym(gym);
        subscription.setPackageId(subscriptionId);
        subscription.setSupportedServices(new HashSet<>());
        gym.getSubscriptions().add(subscription);
        gymRepository.save(gym);
    }

    @Transactional
    @CacheEvict(cacheNames = "gym-detail", key = "#gymId")
    public void updateGymSubscriptionBenefits(Long gymId, Long packageId, GymSubscriptionBenefitsUpdateRequest request) {
        Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));
        GymSubscription subscription = gym.getSubscriptions().stream()
                .filter(sub -> sub.getPackageId() != null && sub.getPackageId().equals(packageId))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("SUBSCRIPTION_NOT_ENABLED", "error.subscription_not_enabled"));

        if (request.benefitIds() != null) {
            List<SupportedService> services = supportedServiceRepository.findAllById(request.benefitIds()).stream()
                    .filter(s -> s.getGymId() == null || s.getGymId().equals(gymId))
                    .toList();
            subscription.setSupportedServices(new HashSet<>(services));
        }

        gymRepository.save(gym);
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(cacheNames = "gym-detail", key = "#gymId"),
        @CacheEvict(cacheNames = "admin-gyms", allEntries = true),
        @CacheEvict(cacheNames = "main-page-gyms", allEntries = true)
    })
    public void deleteGym(Long gymId) {
        Gym gym = gymRepository.findById(gymId).orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));

        List<String> dependencies = new ArrayList<>();
        if (savedGymRepository.existsByGymId(gymId)) {
            dependencies.add("error.gym_dependency_saved");
        }
        if (gymEntranceHistoryRepository.existsByGymId(gymId)) {
            dependencies.add("error.gym_dependency_entrance_history");
        }
        if (reservationRepository.existsByGymId(gymId)) {
            dependencies.add("error.gym_dependency_reservations");
        }

        if (!dependencies.isEmpty()) {
            throw new az.fitnest.catalog.exception.GymDependencyException(dependencies);
        }

        gymLessonTypeRepository.deleteByGymId(gymId);
        supportedServiceRepository.deleteSubscriptionAssociationsByGymId(gymId);
        supportedServiceRepository.deleteAllByGymId(gymId);
        gymAdminRepository.deleteAllByGymId(gymId);

        List<String> filesToDelete = new ArrayList<>();
        if (gym.getCoverImageUrl() != null) filesToDelete.add(gym.getCoverImageUrl());
        if (gym.getQrCodeUrl() != null) filesToDelete.add(gym.getQrCodeUrl());

        if (gym.getImages() != null) {
            filesToDelete.addAll(gym.getImages().stream().map(GymImage::getUrl).toList());
        }

        if (gym.getTrainers() != null) {
            filesToDelete.addAll(gym.getTrainers().stream().map(Trainer::getPicture).filter(Objects::nonNull).toList());
        }

        if (gym.getRooms() != null) {
            filesToDelete.addAll(gym.getRooms().stream()
                    .flatMap(r -> r.getImages().stream())
                    .map(RoomImage::getPictureUrl)
                    .toList());
        }

        gymRepository.delete(gym);

        fileStorageService.deleteFilesAfterCommit(filesToDelete);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "gym-detail", key = "T(az.fitnest.catalog.util.UserContext).extractUserId(#principal) + '_' + #gymId + '_AZ'"),
            @CacheEvict(cacheNames = "gym-detail", key = "T(az.fitnest.catalog.util.UserContext).extractUserId(#principal) + '_' + #gymId + '_EN'"),
            @CacheEvict(cacheNames = "gym-detail", key = "T(az.fitnest.catalog.util.UserContext).extractUserId(#principal) + '_' + #gymId + '_RU'"),
            @CacheEvict(cacheNames = "main-page-gyms", allEntries = true)
    })
    public boolean toggleSave(Object principal, Long gymId) {
        Long userId = UserContext.extractUserId(principal);
        if (userId == null) throw new ForbiddenException("error.unauthorized", "UNAUTHORIZED");

        Optional<SavedGym> existing = savedGymRepository.findByUserIdAndGymId(userId, gymId);
        if (existing.isPresent()) {
            savedGymRepository.delete(existing.get());
            return false;
        } else {
            Gym gym = gymRepository.findById(gymId).orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));
            SavedGym saved = new SavedGym();
            saved.setUserId(userId);
            saved.setGym(gym);
            savedGymRepository.save(saved);
            return true;
        }
    }

    public CheckInResponse checkIn(Object principal, Long gymId) {
        Long userId = UserContext.extractUserId(principal);
        if (userId == null) throw new ForbiddenException("error.unauthorized", "UNAUTHORIZED");

        Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));

        orderServiceGrpcClient.checkIn(userId, gymId);

        String addressText = gym.getAddress() != null ? gym.getAddress().getAddressText() : null;
        LocalDateTime now = LocalDateTime.now();

        return new CheckInResponse(addressText, now.toLocalDate(), now.toLocalTime());
    }

    @CacheEvict(cacheNames = "gym-images", key = "#gymId")
    public void addRoomImages(Long gymId, List<String> roomNames, List<MultipartFile> files) {
        if (roomNames.size() != files.size()) {
            throw new BadRequestException("INVALID_INPUT", "error.invalid_input");
        }

        List<CompletableFuture<RoomImageUploadResult>> futures = new ArrayList<>();

        for (int i = 0; i < files.size(); i++) {
            MultipartFile originalFile = files.get(i);
            if (originalFile == null || originalFile.isEmpty()) continue;

            String roomName = roomNames.get(i);
            MultipartFile validatedFile = fileStorageService.validateAndWrapImage(originalFile);

            futures.add(CompletableFuture.supplyAsync(() -> {
                String url = fileStorageService.saveFile(validatedFile, "/gyms/rooms");
                return new RoomImageUploadResult(roomName, url);
            }, imageUploadExecutor));
        }

        List<RoomImageUploadResult> results = futures.stream()
                .map(CompletableFuture::join)
                .toList();

        if (!results.isEmpty()) {
            new TransactionTemplate(transactionManager).execute(status -> {
                applyRoomImagesUpdateInternal(gymId, results);
                return null;
            });
        }
    }

    @Transactional
    protected void applyRoomImagesUpdateInternal(Long gymId, List<RoomImageUploadResult> results) {
        Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));

        for (RoomImageUploadResult res : results) {
            Room room = gym.getRooms().stream()
                    .filter(r -> r.getName().equals(res.roomName()))
                    .findFirst()
                    .orElseGet(() -> {
                        Room newRoom = Room.builder()
                                .name(res.roomName())
                                .gym(gym)
                                .build();
                        gym.getRooms().add(newRoom);
                        return newRoom;
                    });

            RoomImage roomImage = RoomImage.builder()
                    .room(room)
                    .pictureUrl(res.url())
                    .build();

            room.getImages().add(roomImage);
        }
        gymRepository.save(gym);
    }

    @Transactional
    @CacheEvict(cacheNames = "gym-images", key = "#gymId")
    public void deleteAllGymRooms(Long gymId) {
        Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));

        if (gym.getRooms() != null && !gym.getRooms().isEmpty()) {
            List<String> roomImageUrls = gym.getRooms().stream()
                    .flatMap(r -> r.getImages().stream())
                    .map(RoomImage::getPictureUrl)
                    .filter(url -> url != null && !url.isBlank())
                    .toList();

            if (!roomImageUrls.isEmpty()) {
                fileStorageService.deleteFilesAfterCommit(roomImageUrls);
            }
            gym.getRooms().clear();
            gymRepository.save(gym);
        }
    }

    @Transactional
    @CacheEvict(cacheNames = "gym-images", key = "#gymId")
    public void deleteGymRoomById(Long gymId, Long roomId) {
        Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));

        Room room = gym.getRooms().stream()
                .filter(r -> r.getId().equals(roomId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("ROOM_NOT_FOUND", "error.room_not_found"));

        List<String> roomImageUrls = room.getImages().stream()
                .map(RoomImage::getPictureUrl)
                .filter(url -> url != null && !url.isBlank())
                .toList();

        if (!roomImageUrls.isEmpty()) {
            fileStorageService.deleteFilesAfterCommit(roomImageUrls);
        }

        gym.getRooms().remove(room);
        gymRepository.save(gym);
    }

    @Transactional
    @CacheEvict(cacheNames = "gym-images", key = "#gymId")
    public void deleteRoomImageById(Long gymId, Long imageId) {
        Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));

        for (Room room : gym.getRooms()) {
            Optional<RoomImage> roomImageOpt = room.getImages().stream()
                    .filter(img -> img.getId().equals(imageId))
                    .findFirst();

            if (roomImageOpt.isPresent()) {
                RoomImage roomImage = roomImageOpt.get();
                if (roomImage.getPictureUrl() != null && !roomImage.getPictureUrl().isBlank()) {
                    fileStorageService.deleteFilesAfterCommit(List.of(roomImage.getPictureUrl()));
                }
                room.getImages().remove(roomImage);
                gymRepository.save(gym);
                return;
            }
        }
        throw new ResourceNotFoundException("ROOM_IMAGE_NOT_FOUND", "error.room_image_not_found");
    }

    @CacheEvict(cacheNames = "gym-detail", key = "#gymId")
    public void updateCoverImage(Long gymId, MultipartFile coverPhoto) {
        MultipartFile validatedFile = fileStorageService.validateAndWrapImage(coverPhoto);
        String url = fileStorageService.saveFile(validatedFile, "/gyms/covers");
        updateCoverImageInternal(gymId, url);
    }

    @Transactional
    protected void updateCoverImageInternal(Long gymId, String url) {
        Gym gym = gymRepository.findById(gymId).orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));
        if (gym.getCoverImageUrl() != null) fileStorageService.deleteFilesAfterCommit(List.of(gym.getCoverImageUrl()));
        gym.setCoverImageUrl(url);
        gymRepository.save(gym);
    }

    @Transactional
    public void deleteAllGyms() {
        List<String> filesToDelete = new ArrayList<>();
        try {
            filesToDelete.addAll(gymRepository.findAllGymRelatedFileUrls());
        } catch (Exception e) {
        }

        gymAdminRepository.deleteAllInBatch();
        savedGymRepository.deleteAllInBatch();

        entityManager.createNativeQuery("DELETE FROM room_images").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM rooms").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM gym_subscriptions").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM reviews").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM trainers").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM gym_images").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM gym_social_links").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM gym_general_work_hours").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM gym_work_hours_woman").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM gym_work_hours_man").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM gym_rest_days").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM gyms").executeUpdate();

        if (!filesToDelete.isEmpty()) {
            fileStorageService.deleteFilesAfterCommit(filesToDelete);
        }
    }

    @Transactional
    @CacheEvict(cacheNames = "gym-detail", key = "#gymId")
    public void deleteAllGymSubscriptions(Long gymId) {
        Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));
        gym.getSubscriptions().clear();
        gymRepository.save(gym);
    }

    @Transactional
    @CacheEvict(cacheNames = "gym-detail", key = "#gymId")
    public void deleteGymSubscriptionById(Long gymId, Long subscriptionId) {
        Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));
        boolean removed = gym.getSubscriptions().removeIf(s -> s.getId().equals(subscriptionId));
        if (!removed) {
            throw new ResourceNotFoundException("SUBSCRIPTION_NOT_FOUND", "error.subscription_not_found");
        }
        gymRepository.save(gym);
    }

    @Transactional
    @CacheEvict(cacheNames = "gym-detail", key = "#gymId")
    public void toggleGymReservation(Long gymId, boolean enabled) {
        Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));
        gym.setIsReservationEnabled(enabled);
        gymRepository.save(gym);
    }

    @Transactional
    @CacheEvict(cacheNames = "admin-gyms", allEntries = true)
    public GymCreateStep1Response createGymStep1(GymCreateStep1Request request) {
        if (request.categoryId() == null) {
            throw new BadRequestException("CATEGORY_REQUIRED", "error.category_required");
        }
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("CATEGORY_NOT_FOUND", "error.category_not_found"));
        Gym gym = new Gym();
        gym.setName(request.name());
        gym.setDescription(request.description());
        gym.setPhone(PhoneUtil.normalize(request.phone()));
        gym.setEmail(request.email() != null && request.email().isBlank() ? null : request.email());
        gym.setCategory(category);
        gym.setStatus(GymStatus.DRAFT);
        gym.setCreationStep(1);
        gym = gymRepository.save(gym);

        translationService.autoTranslateAndSave("GYM", gym.getId().toString(), "name", request.name());
        translationService.autoTranslateAndSave("GYM", gym.getId().toString(), "description", request.description());

        if (request.lessonTypeIds() != null && !request.lessonTypeIds().isEmpty()) {
            List<az.fitnest.catalog.model.entity.LessonType> globalLessonTypes = lessonTypeRepository.findAllById(request.lessonTypeIds());
            int order = 1;
            for (az.fitnest.catalog.model.entity.LessonType glt : globalLessonTypes) {
                az.fitnest.catalog.model.entity.GymLessonType gymLessonType = az.fitnest.catalog.model.entity.GymLessonType.builder()
                        .gym(gym)
                        .name(glt.getName())
                        .category(category)
                        .status("ACTIVE")
                        .sortOrder(order++)
                        .build();
                gymLessonTypeRepository.save(gymLessonType);
            }
        }

        return new GymCreateStep1Response(gym.getId());
    }

    @Transactional
    public void createGymStep2(Long id, List<String> names, List<String> surnames, List<Long> professionIds, List<String> emails, List<String> phones, List<MultipartFile> photos, List<String> lessonTypesPerTrainer) {
        Gym gym = gymRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));
        validateStep(gym, 1);

        gymTrainerService.addTrainers(id, names, surnames, professionIds, emails, phones, photos, lessonTypesPerTrainer);

        updateStep(gym, 1);
    }

    @Transactional
    public void createGymStep3(Long gymId, GymCreateStep2Request request) {
        Gym gym = gymRepository.findById(gymId).orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));
        validateStep(gym, 2);

        Set<GymWorkHourPeriod> restDays = new HashSet<>();
        if (request.restDays() != null) {
            restDays = request.restDays().stream()
                    .flatMap(r -> az.fitnest.catalog.mapper.GymMapper.expandPeriods(r.period()).stream())
                    .collect(java.util.stream.Collectors.toSet());
        }

        validateNoWorkHoursOnRestDays(request.generalWorkHours(), restDays, "general");
        validateNoWorkHoursOnRestDays(request.workHoursWoman(), restDays, "woman");
        validateNoWorkHoursOnRestDays(request.workHoursMan(), restDays, "man");

        updateWorkHours(gym.getGeneralWorkHours(), request.generalWorkHours());
        updateWorkHours(gym.getWorkHoursWoman(), request.workHoursWoman());
        updateWorkHours(gym.getWorkHoursMan(), request.workHoursMan());

        gym.getRestDays().clear();
        gym.getRestDays().addAll(restDays);

        updateStep(gym, 2);
    }

    private void validateNoWorkHoursOnRestDays(Set<GymWorkHourResponse> workHours, Set<GymWorkHourPeriod> restDays, String type) {
        if (workHours == null || restDays.isEmpty()) return;
        for (GymWorkHourResponse wh : workHours) {
            Set<GymWorkHourPeriod> whPeriods = az.fitnest.catalog.mapper.GymMapper.expandPeriods(wh.period());
            for (GymWorkHourPeriod p : whPeriods) {
                if (restDays.contains(p)) {
                    throw new BadRequestException("WORK_HOURS_ON_REST_DAY", "error.work_hours_on_rest_day");
                }
            }
        }
    }

    public void createGymStep4(Long gymId, GymCreateStep3Request request) {
        GeocodingResponse geocoding = reverseGeocodingService.reverseGeocode(request.latitude(), request.longitude());
        createGymStep4Internal(gymId, request, geocoding);
    }

    @Transactional
    protected void createGymStep4Internal(Long gymId, GymCreateStep3Request request, GeocodingResponse geocoding) {
        Gym gym = gymRepository.findById(gymId).orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));
        validateStep(gym, 3);

        Address address = new Address();
        address.setLatitude(request.latitude());
        address.setLongitude(request.longitude());
        if (geocoding != null) {
            address.setAddressText(geocoding.addressText());
            address.setCity(geocoding.city());
        }
        gym.setAddress(address);

        if (address != null) {
            translationService.autoTranslateAndSave("GYM", gym.getId().toString(), "addressText", address.getAddressText());
            translationService.autoTranslateAndSave("GYM", gym.getId().toString(), "city", address.getCity());
        }

        updateStep(gym, 3);
    }

    @Caching(evict = {
        @CacheEvict(cacheNames = "gym-detail", key = "#gymId"),
        @CacheEvict(cacheNames = "gym-images", key = "#gymId")
    })
    public void createGymStep5(Long gymId, MultipartFile coverPhoto, List<String> roomNames, List<MultipartFile> roomPhotos) {
        CompletableFuture<String> coverFuture = null;
        if (coverPhoto != null && !coverPhoto.isEmpty()) {
            MultipartFile validatedCover = fileStorageService.validateAndWrapImage(coverPhoto);
            coverFuture = CompletableFuture.supplyAsync(() -> 
                fileStorageService.saveFile(validatedCover, "/gyms/covers"), 
                imageUploadExecutor
            );
        }

        List<CompletableFuture<RoomImageUploadResult>> roomFutures = new ArrayList<>();
        if (roomNames != null && roomPhotos != null && roomNames.size() == roomPhotos.size() && !roomNames.isEmpty()) {
            for (int i = 0; i < roomPhotos.size(); i++) {
                MultipartFile originalFile = roomPhotos.get(i);
                if (originalFile == null || originalFile.isEmpty()) continue;

                String roomName = roomNames.get(i);
                MultipartFile validatedFile = fileStorageService.validateAndWrapImage(originalFile);

                roomFutures.add(CompletableFuture.supplyAsync(() -> {
                    String url = fileStorageService.saveFile(validatedFile, "/gyms/rooms");
                    return new RoomImageUploadResult(roomName, url);
                }, imageUploadExecutor));
            }
        }

        // Wait for all uploads to complete
        String coverUrl = null;
        if (coverFuture != null) {
            coverUrl = coverFuture.join();
        }

        List<RoomImageUploadResult> roomResults = roomFutures.stream()
                .map(CompletableFuture::join)
                .toList();

        // Save to DB in a single transaction template/call
        final String finalCoverUrl = coverUrl;
        new TransactionTemplate(transactionManager).execute(status -> {
            if (finalCoverUrl != null) {
                updateCoverImageInternal(gymId, finalCoverUrl);
            }
            if (!roomResults.isEmpty()) {
                applyRoomImagesUpdateInternal(gymId, roomResults);
            }
            completeStep5Internal(gymId);
            return null;
        });
    }

    @Transactional
    protected void completeStep5Internal(Long gymId) {
        Gym gym = gymRepository.findById(gymId).orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));
        validateStep(gym, 4);
        updateStep(gym, 4);
    }

    @Transactional
    public void createGymStep6(Long gymId, GymCreateStep6Request request) {
        Gym gym = gymRepository.findById(gymId).orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));
        validateStep(gym, 5);
        updateGymSubscriptionsInternal(gym, request);
        updateStep(gym, 5);
    }

    @Transactional
    @CacheEvict(cacheNames = "gym-detail", allEntries = true)
    public void updateGymSubscriptions(Long gymId, GymCreateStep6Request request) {
        Gym gym = gymRepository.findById(gymId).orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));
        updateGymSubscriptionsInternal(gym, request);
    }

    private void updateGymSubscriptionsInternal(Gym gym, GymCreateStep6Request request) {
        Long gymId = gym.getId();
        gym.getSubscriptions().clear();

        Set<Long> processedPackages = new HashSet<>();

        for (GymCreateStep6SubscriptionRequest subReq : request.subscriptions()) {
            if (!processedPackages.add(subReq.packageId())) {
                continue;
            }
            GymSubscription subscription = new GymSubscription();
            subscription.setGym(gym);
            subscription.setPackageId(subReq.packageId());
            subscription.setDailyPrice(subReq.dailyPrice());
            if (subReq.supportedServicesId() != null && !subReq.supportedServicesId().isEmpty()) {
                List<SupportedService> services = supportedServiceRepository.findAllById(subReq.supportedServicesId()).stream()
                        .filter(s -> s.getGymId() == null || s.getGymId().equals(gymId))
                        .toList();
                subscription.setSupportedServices(new HashSet<>(services));
            }
            gym.getSubscriptions().add(subscription);
        }
        gymRepository.save(gym);
    }

    @Caching(evict = {
        @CacheEvict(cacheNames = {"main-page-gyms", "admin-gyms"}, allEntries = true)
    })
    public void createGymStep7(Long gymId, GymCreateStep7Request request) {
        Gym gym = gymRepository.findById(gymId).orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));

        List<CompletableFuture<GymAdminCreateResult>> futures = request.admins().stream()
                .map(adminReq -> CompletableFuture.supplyAsync(() -> {
                    Long userId = identityServiceGrpcClient.createGymAdmin(
                        adminReq.name(), 
                        adminReq.surname(), 
                        PhoneUtil.normalize(adminReq.phoneNumber()), 
                        adminReq.email(), 
                        adminReq.password()
                    );
                    return new GymAdminCreateResult(adminReq, userId);
                }, imageUploadExecutor))
                .toList();

        List<GymAdminCreateResult> results = futures.stream()
                .map(CompletableFuture::join)
                .toList();

        new TransactionTemplate(transactionManager).execute(status -> {
            Gym freshGym = gymRepository.findById(gymId).orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));
            for (GymAdminCreateResult res : results) {
                String role = (res.req().role() != null && !res.req().role().trim().isEmpty()) ? res.req().role() : "Super admin";
                az.fitnest.catalog.model.entity.GymAdmin saved = gymAdminRepository.save(
                    az.fitnest.catalog.mapper.GymMapper.toAdminEntity(freshGym, res.req(), res.userId(), role)
                );
                
                translationService.autoTranslateAndSave("GymAdmin", saved.getId().toString(), "name", saved.getName());
                translationService.autoTranslateAndSave("GymAdmin", saved.getId().toString(), "surname", saved.getSurname());
            }
            finalizeGymStep7Internal(gymId);
            return null;
        });
    }

    @Override
    @Caching(evict = {
        @CacheEvict(cacheNames = {"main-page-gyms", "admin-gyms"}, allEntries = true)
    })
    public Long createGymComplete(GymCreateCompleteRequest request, MultipartFile coverPhoto,
                                   List<MultipartFile> trainerPhotos, List<MultipartFile> roomPhotos) {
        // ═══════════════════════════════════════════════════════════════
        // Phase 1: Validate all data upfront
        // ═══════════════════════════════════════════════════════════════
        
        // Validate step 1
        GymCreateStep1Request step1 = GymCreateStep1Request.builder()
                .categoryId(request.categoryId())
                .name(request.name())
                .phone(request.phone())
                .description(request.description())
                .email(request.email())
                .lessonTypeIds(request.lessonTypeIds())
                .build();
        validateStep1(step1);

        // Validate step 2 (trainer emails/phones)
        if (request.trainers() != null && !request.trainers().isEmpty()) {
            List<String> emails = request.trainers().stream().map(GymCreateCompleteRequest.TrainerCreateData::email).filter(e -> e != null && !e.isBlank()).toList();
            List<String> phones = request.trainers().stream().map(GymCreateCompleteRequest.TrainerCreateData::phone).filter(p -> p != null && !p.isBlank()).toList();
            validateStep2(emails, phones);
        }

        // Validate step 3 (working hours)
        GymCreateStep2Request step3 = GymCreateStep2Request.builder()
                .generalWorkHours(request.generalWorkHours())
                .workHoursWoman(request.workHoursWoman())
                .workHoursMan(request.workHoursMan())
                .restDays(request.restDays())
                .build();
        validateStep3(step3);

        // Validate step 4 (coordinates)
        GymCreateStep3Request step4 = GymCreateStep3Request.builder()
                .latitude(request.latitude())
                .longitude(request.longitude())
                .build();
        validateStep4(step4);

        // Validate step 5 (images)
        validateStep5(coverPhoto, request.roomNames(), roomPhotos);

        // Validate step 6 (subscriptions)
        GymCreateStep6Request step6 = GymCreateStep6Request.builder()
                .subscriptions(request.subscriptions())
                .build();
        validateStep6(step6);

        // Validate step 7 (admins)
        GymCreateStep7Request step7 = GymCreateStep7Request.builder()
                .admins(request.admins())
                .build();
        validateStep7(step7);

        // ═══════════════════════════════════════════════════════════════
        // Phase 2: Upload all files in parallel (outside transaction)
        // ═══════════════════════════════════════════════════════════════
        
        // Geocoding (external call)
        GeocodingResponse geocoding = reverseGeocodingService.reverseGeocode(request.latitude(), request.longitude());

        // Cover photo upload
        CompletableFuture<String> coverFuture = null;
        if (coverPhoto != null && !coverPhoto.isEmpty()) {
            MultipartFile validatedCover = fileStorageService.validateAndWrapImage(coverPhoto);
            coverFuture = CompletableFuture.supplyAsync(() ->
                fileStorageService.saveFile(validatedCover, "/gyms/covers"), imageUploadExecutor);
        }

        // Trainer photo uploads
        List<CompletableFuture<String>> trainerPhotoFutures = new ArrayList<>();
        if (trainerPhotos != null) {
            for (MultipartFile photo : trainerPhotos) {
                if (photo != null && !photo.isEmpty()) {
                    MultipartFile validated = fileStorageService.validateAndWrapImage(photo);
                    trainerPhotoFutures.add(CompletableFuture.supplyAsync(() ->
                        fileStorageService.saveFile(validated, "/gyms/trainers"), imageUploadExecutor));
                } else {
                    trainerPhotoFutures.add(CompletableFuture.completedFuture(null));
                }
            }
        }

        // Room photo uploads
        List<CompletableFuture<RoomImageUploadResult>> roomFutures = new ArrayList<>();
        if (request.roomNames() != null && roomPhotos != null && request.roomNames().size() == roomPhotos.size()) {
            for (int i = 0; i < roomPhotos.size(); i++) {
                MultipartFile file = roomPhotos.get(i);
                if (file == null || file.isEmpty()) continue;
                String roomName = request.roomNames().get(i);
                MultipartFile validated = fileStorageService.validateAndWrapImage(file);
                roomFutures.add(CompletableFuture.supplyAsync(() -> {
                    String url = fileStorageService.saveFile(validated, "/gyms/rooms");
                    return new RoomImageUploadResult(roomName, url);
                }, imageUploadExecutor));
            }
        }

        // Create gym admin users in identity service (external gRPC call)
        List<CompletableFuture<GymAdminCreateResult>> adminFutures = request.admins().stream()
                .map(adminReq -> CompletableFuture.supplyAsync(() -> {
                    Long userId = identityServiceGrpcClient.createGymAdmin(
                        adminReq.name(), adminReq.surname(),
                        PhoneUtil.normalize(adminReq.phoneNumber()),
                        adminReq.email(), adminReq.password()
                    );
                    return new GymAdminCreateResult(adminReq, userId);
                }, imageUploadExecutor))
                .toList();

        // Wait for all parallel operations to complete
        String finalCoverUrl = coverFuture != null ? coverFuture.join() : null;
        List<String> trainerPhotoUrls = trainerPhotoFutures.stream().map(CompletableFuture::join).toList();
        List<RoomImageUploadResult> roomResults = roomFutures.stream().map(CompletableFuture::join).toList();
        List<GymAdminCreateResult> adminResults = adminFutures.stream().map(CompletableFuture::join).toList();

        // ═══════════════════════════════════════════════════════════════
        // Phase 3: Save everything in a single transaction
        // ═══════════════════════════════════════════════════════════════
        
        Long gymId = new TransactionTemplate(transactionManager).execute(status -> {
            // Step 1: Create Gym entity
            Category category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("CATEGORY_NOT_FOUND", "error.category_not_found"));
            Gym gym = new Gym();
            gym.setName(request.name());
            gym.setDescription(request.description());
            gym.setPhone(PhoneUtil.normalize(request.phone()));
            gym.setEmail(request.email() != null && request.email().isBlank() ? null : request.email());
            gym.setCategory(category);

            // Step 3: Working Hours
            updateWorkHours(gym.getGeneralWorkHours(), request.generalWorkHours());
            updateWorkHours(gym.getWorkHoursWoman(), request.workHoursWoman());
            updateWorkHours(gym.getWorkHoursMan(), request.workHoursMan());

            if (request.restDays() != null) {
                Set<GymWorkHourPeriod> restDays = request.restDays().stream()
                        .flatMap(r -> az.fitnest.catalog.mapper.GymMapper.expandPeriods(r.period()).stream())
                        .collect(Collectors.toSet());
                gym.getRestDays().addAll(restDays);
            }

            // Step 4: Address
            Address address = new Address();
            address.setLatitude(request.latitude());
            address.setLongitude(request.longitude());
            if (geocoding != null) {
                address.setAddressText(geocoding.addressText());
                address.setCity(geocoding.city());
            }
            gym.setAddress(address);

            // Step 5: Cover image
            if (finalCoverUrl != null) {
                gym.setCoverImageUrl(finalCoverUrl);
            }

            // Set status to ACTIVE directly (skipping DRAFT)
            gym.setStatus(GymStatus.ACTIVE);
            gym.setCreationStep(7);

            Gym savedGym = gymRepository.save(gym);
            Long savedGymId = savedGym.getId();

            // Translations for gym
            translationService.autoTranslateAndSave("GYM", savedGymId.toString(), "name", request.name());
            translationService.autoTranslateAndSave("GYM", savedGymId.toString(), "description", request.description());
            if (savedGym.getAddress() != null) {
                translationService.autoTranslateAndSave("GYM", savedGymId.toString(), "addressText", savedGym.getAddress().getAddressText());
                translationService.autoTranslateAndSave("GYM", savedGymId.toString(), "city", savedGym.getAddress().getCity());
            }

            // Step 1: Lesson types
            if (request.lessonTypeIds() != null && !request.lessonTypeIds().isEmpty()) {
                List<az.fitnest.catalog.model.entity.LessonType> globalLessonTypes = lessonTypeRepository.findAllById(request.lessonTypeIds());
                int order = 1;
                for (az.fitnest.catalog.model.entity.LessonType glt : globalLessonTypes) {
                    az.fitnest.catalog.model.entity.GymLessonType gymLessonType = az.fitnest.catalog.model.entity.GymLessonType.builder()
                            .gym(savedGym)
                            .name(glt.getName())
                            .category(category)
                            .status("ACTIVE")
                            .sortOrder(order++)
                            .build();
                    gymLessonTypeRepository.save(gymLessonType);
                }
            }

            // Step 2: Trainers
            if (request.trainers() != null && !request.trainers().isEmpty()) {
                List<String> names = request.trainers().stream().map(GymCreateCompleteRequest.TrainerCreateData::name).toList();
                List<String> surnames = request.trainers().stream().map(GymCreateCompleteRequest.TrainerCreateData::surname).toList();
                List<Long> professionIds = request.trainers().stream().map(GymCreateCompleteRequest.TrainerCreateData::professionId).toList();
                List<String> emails = request.trainers().stream().map(t -> t.email() != null ? t.email() : "").toList();
                List<String> phones = request.trainers().stream().map(t -> t.phone() != null ? t.phone() : "").toList();
                List<String> lessonTypesPerTrainer = request.trainers().stream().map(t -> t.lessonTypeIds() != null ? t.lessonTypeIds() : "").toList();

                // Convert trainer photo URLs to a list that matches trainer order
                // trainerPhotoUrls may have fewer entries if some trainers have no photo
                gymTrainerService.addTrainersWithUrls(savedGymId, names, surnames, professionIds, emails, phones, trainerPhotoUrls, lessonTypesPerTrainer);
            }

            // Step 5: Room images
            if (!roomResults.isEmpty()) {
                for (RoomImageUploadResult res : roomResults) {
                    Room room = savedGym.getRooms().stream()
                            .filter(r -> r.getName().equals(res.roomName()))
                            .findFirst()
                            .orElseGet(() -> {
                                Room newRoom = Room.builder()
                                        .name(res.roomName())
                                        .gym(savedGym)
                                        .build();
                                savedGym.getRooms().add(newRoom);
                                return newRoom;
                            });
                    RoomImage roomImage = RoomImage.builder()
                            .room(room)
                            .pictureUrl(res.url())
                            .build();
                    room.getImages().add(roomImage);
                }
                gymRepository.save(savedGym);
            }

            // Step 6: Subscriptions
            updateGymSubscriptionsInternal(savedGym, step6);

            // Step 7: Admins
            for (GymAdminCreateResult res : adminResults) {
                String role = (res.req().role() != null && !res.req().role().trim().isEmpty()) ? res.req().role() : "Super admin";
                az.fitnest.catalog.model.entity.GymAdmin saved = gymAdminRepository.save(
                    az.fitnest.catalog.mapper.GymMapper.toAdminEntity(savedGym, res.req(), res.userId(), role)
                );
                translationService.autoTranslateAndSave("GymAdmin", saved.getId().toString(), "name", saved.getName());
                translationService.autoTranslateAndSave("GymAdmin", saved.getId().toString(), "surname", saved.getSurname());
            }

            return savedGymId;
        });

        // Phase 4: Post-commit actions
        gymQrCodeService.generateAndSaveQrCode(gymId);

        return gymId;
    }

    @Transactional
    public void addGymAdmin(Long gymId, GymAdminCreateRequest request) {
        if (gymAdminRepository.existsByGymIdAndPhoneNumber(gymId, PhoneUtil.normalize(request.phoneNumber()))) {
            throw new BadRequestException("ADMIN_ALREADY_IN_GYM", "error.admin_already_in_gym");
        }
        Gym gym = gymRepository.findById(gymId).orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));
        Long userId = identityServiceGrpcClient.createGymAdmin(request.name(), request.surname(), PhoneUtil.normalize(request.phoneNumber()), request.email(), request.password());
        String role = (request.role() != null && !request.role().trim().isEmpty()) ? request.role() : "Admin";
        az.fitnest.catalog.model.entity.GymAdmin saved = gymAdminRepository.save(az.fitnest.catalog.mapper.GymMapper.toAdminEntity(gym, request, userId, role));
        
        translationService.autoTranslateAndSave("GymAdmin", saved.getId().toString(), "name", saved.getName());
        translationService.autoTranslateAndSave("GymAdmin", saved.getId().toString(), "surname", saved.getSurname());
    }

    @Transactional
    public void updateGymAdmin(Long gymId, Long adminId, GymAdminUpdateRequest request) {
        az.fitnest.catalog.model.entity.GymAdmin admin = gymAdminRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("ADMIN_NOT_FOUND", "error.admin_not_found"));

        if (!admin.getGym().getId().equals(gymId)) {
            throw new BadRequestException("ADMIN_GYM_MISMATCH", "error.admin_gym_mismatch");
        }

        admin.setName(request.name());
        admin.setSurname(request.surname());
        admin.setPhoneNumber(PhoneUtil.normalize(request.phoneNumber()));
        admin.setEmail(request.email());
        if (request.role() != null && !request.role().trim().isEmpty()) {
            admin.setRole(request.role());
        }

        gymAdminRepository.save(admin);
        
        translationService.autoTranslateAndSave("GymAdmin", admin.getId().toString(), "name", admin.getName());
        translationService.autoTranslateAndSave("GymAdmin", admin.getId().toString(), "surname", admin.getSurname());
    }

    @Transactional
    public void deleteGymAdmin(Long gymId, Long adminId) {
        az.fitnest.catalog.model.entity.GymAdmin admin = gymAdminRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("ADMIN_NOT_FOUND", "error.admin_not_found"));

        if (!admin.getGym().getId().equals(gymId)) {
            throw new BadRequestException("ADMIN_GYM_MISMATCH", "error.admin_gym_mismatch");
        }

        gymAdminRepository.delete(admin);
    }

    @Transactional
    protected void finalizeGymStep7Internal(Long gymId) {
        Gym gym = gymRepository.findById(gymId).orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));
        validateStep(gym, 6);
        gym.setStatus(GymStatus.ACTIVE);
        gym.setCreationStep(7);
        gymRepository.save(gym);
        gymQrCodeService.generateAndSaveQrCode(gym.getId());
    }

    private void validateStep(Gym gym, int requiredStep) {
        if (gym.getStatus() == GymStatus.ACTIVE ||
            gym.getStatus() == GymStatus.INACTIVE) {
            throw new BadRequestException("GYM_NOT_EDITABLE", "error.gym_not_editable_via_steps");
        }
        Integer currentStep = gym.getCreationStep() != null ? gym.getCreationStep() : 1;

        if (requiredStep == 2 && currentStep == 1) {
            return;
        }

        if (currentStep < requiredStep) {
            throw new BadRequestException("INVALID_STEP", "error.invalid_step");
        }
    }

    private void updateStep(Gym gym, int completedStep) {
        Integer currentStep = gym.getCreationStep() != null ? gym.getCreationStep() : 1;
        if (currentStep <= completedStep) {
            gym.setCreationStep(completedStep + 1);
            gymRepository.save(gym);
        }
    }

    @Transactional
    public SupportedServiceResponse createSupportedService(SupportedServiceRequest request) {
        if (supportedServiceRepository.findByNameIgnoreCaseAndGymId(request.name(), request.gymId()).isPresent()) {
            throw new BadRequestException("SERVICE_ALREADY_EXISTS", "error.service_already_exists");
        }
        SupportedService service = new SupportedService();
        service.setName(request.name());
        service.setGymId(request.gymId());
        service = supportedServiceRepository.save(service);
        
        translationService.autoTranslateAndSave("SupportedService", service.getId().toString(), "name", service.getName());
        
        return new SupportedServiceResponse(service.getId(), service.getName(), service.getGymId());
    }

    @Transactional
    @org.springframework.cache.annotation.CacheEvict(cacheNames = {"gymDetails", "admin-gyms"}, allEntries = true)
    public void deleteSupportedService(Long id) {
        supportedServiceRepository.deleteSubscriptionAssociations(id);
        supportedServiceRepository.deleteById(id);
    }

    @Override
    public GeocodingResponse reverseGeocode(Double lat, Double lng) {
        return reverseGeocodingService.reverseGeocode(lat, lng);
    }

    @Override
    public java.util.List<GeocodingResponse> forwardGeocode(String query) {
        return reverseGeocodingService.forwardGeocode(query);
    }

    @Transactional
    @CacheEvict(cacheNames = "admin-gyms", allEntries = true)
    public void toggleGymStatus(Long gymId, boolean enabled) {
        Gym gym = gymRepository.findById(gymId).orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));
        gym.setStatus(enabled ? GymStatus.ACTIVE : GymStatus.INACTIVE);
        gymRepository.save(gym);
    }

    private void updateWorkHours(Set<GymWorkHour> target, Set<GymWorkHourResponse> source) {
        target.clear();
        if (source != null) {
            target.addAll(az.fitnest.catalog.mapper.GymMapper.toWorkHours(source));
        }
    }

    @Override
    @Transactional
    @org.springframework.cache.annotation.CacheEvict(cacheNames = {"gymDetails", "admin-gyms", "nearestGyms"}, allEntries = true)
    public void updateGymInfo(Long gymId, az.fitnest.catalog.dto.request.GymInfoUpdateRequest request) {
        Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));

        if (request.categoryId() != null) {
            Category category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("CATEGORY_NOT_FOUND", "error.category_not_found"));
            gym.setCategory(category);
        }

        if (request.name() != null) gym.setName(request.name());
        if (request.description() != null) gym.setDescription(request.description());
        if (request.phone() != null) gym.setPhone(request.phone());
        if (request.email() != null) gym.setEmail(request.email().isBlank() ? null : request.email());

        if (gym.getAddress() == null) {
            gym.setAddress(new Address());
        }

        if (request.city() != null) gym.getAddress().setCity(request.city());
        if (request.address() != null) gym.getAddress().setAddressText(request.address());
        if (request.latitude() != null) gym.getAddress().setLatitude(request.latitude());
        if (request.longitude() != null) gym.getAddress().setLongitude(request.longitude());

        gymRepository.save(gym);

        if (request.name() != null) {
            translationService.autoTranslateAndSave("GYM", gym.getId().toString(), "name", request.name());
        }
        if (request.description() != null) {
            translationService.autoTranslateAndSave("GYM", gym.getId().toString(), "description", request.description());
        }
        if (gym.getAddress() != null) {
            if (request.address() != null) {
                translationService.autoTranslateAndSave("GYM", gym.getId().toString(), "addressText", gym.getAddress().getAddressText());
            }
            if (request.city() != null) {
                translationService.autoTranslateAndSave("GYM", gym.getId().toString(), "city", gym.getAddress().getCity());
            }
        }
    }

    @Override
    @Transactional
    public void updateReservationStatusAdmin(Long reservationId, az.fitnest.catalog.model.enums.ReservationStatus status, String reason) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("RESERVATION_NOT_FOUND", "error.reservation_not_found"));

        reservation.setStatus(status);
        if (status == az.fitnest.catalog.model.enums.ReservationStatus.CANCELLED || status == az.fitnest.catalog.model.enums.ReservationStatus.REJECTED) {
            reservation.setCancelReasonText(reason);
            reservation.setCancelledAt(LocalDateTime.now());
        } else if (status == az.fitnest.catalog.model.enums.ReservationStatus.APPROVED) {
            reservation.setApprovedAt(LocalDateTime.now());
        }

        reservationRepository.save(reservation);
    }

    @Override
    @Transactional
    public void addLessonHourAdmin(Long gymId, az.fitnest.catalog.dto.request.LessonHourRequest request) {
        Trainer trainer = trainerRepository.findById(request.trainerId())
                .orElseThrow(() -> new ResourceNotFoundException("TRAINER_NOT_FOUND", "error.trainer_not_found"));

        GymLessonType lessonType = gymLessonTypeRepository.findById(request.lessonTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("LESSON_TYPE_NOT_FOUND", "error.lesson_type_not_found"));

        TrainerReservationDate trd = TrainerReservationDate.builder()
                .gymId(gymId)
                .trainer(trainer)
                .classType(lessonType)
                .date(request.date())
                .startTime(request.startTime())
                .endTime(request.endTime())
                .emptySpaces(request.maxSlots())
                .status(az.fitnest.catalog.model.enums.SessionStatus.OPEN)
                .build();

        trainerReservationDateRepository.save(trd);

        gymRepository.findById(gymId).ifPresent(gym -> {
            if (!Boolean.TRUE.equals(gym.getIsReservationEnabled())) {
                gym.setIsReservationEnabled(true);
                gymRepository.save(gym);
            }
        });
    }

    @Override
    @Transactional
    public void deleteLessonHourAdmin(Long lessonHourId) {
        trainerReservationDateRepository.findById(lessonHourId).ifPresent(session -> {
            Long gymId = session.getGymId();
            trainerReservationDateRepository.deleteById(lessonHourId);
            if (gymId != null && trainerReservationDateRepository.findByGymId(gymId).isEmpty()) {
                gymRepository.findById(gymId).ifPresent(gym -> {
                    if (Boolean.TRUE.equals(gym.getIsReservationEnabled())) {
                        gym.setIsReservationEnabled(false);
                        gymRepository.save(gym);
                    }
                });
            }
        });
    }

    @Override
    public void validateStep1(GymCreateStep1Request request) {
        if (request.categoryId() == null) {
            throw new BadRequestException("CATEGORY_REQUIRED", "error.category_required");
        }
        categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("CATEGORY_NOT_FOUND", "error.category_not_found"));

        if (request.lessonTypeIds() != null && !request.lessonTypeIds().isEmpty()) {
            List<az.fitnest.catalog.model.entity.LessonType> globalLessonTypes = lessonTypeRepository.findAllById(request.lessonTypeIds());
            if (globalLessonTypes.size() != request.lessonTypeIds().size()) {
                throw new BadRequestException("INVALID_LESSON_TYPES", "error.invalid_lesson_types");
            }
        }
    }

    @Override
    public void validateStep2(List<String> emails, List<String> phones) {
        if (emails != null) {
            for (String email : emails) {
                if (trainerRepository.existsByEmail(email)) {
                    throw new BadRequestException("TRAINER_EMAIL_EXISTS", "error.trainer_email_exists");
                }
            }
        }
        if (phones != null) {
            for (String phone : phones) {
                String normalized = PhoneUtil.normalize(phone);
                if (trainerRepository.existsByPhone(normalized)) {
                    throw new BadRequestException("TRAINER_PHONE_EXISTS", "error.trainer_phone_exists");
                }
            }
        }
    }

    @Override
    public void validateStep3(GymCreateStep2Request request) {
        Set<GymWorkHourPeriod> restDays = new HashSet<>();
        if (request.restDays() != null) {
            restDays = request.restDays().stream()
                    .flatMap(r -> az.fitnest.catalog.mapper.GymMapper.expandPeriods(r.period()).stream())
                    .collect(java.util.stream.Collectors.toSet());
        }

        validateNoWorkHoursOnRestDays(request.generalWorkHours(), restDays, "general");
        validateNoWorkHoursOnRestDays(request.workHoursWoman(), restDays, "woman");
        validateNoWorkHoursOnRestDays(request.workHoursMan(), restDays, "man");
    }

    @Override
    public void validateStep4(GymCreateStep3Request request) {
        if (request.latitude() == null || request.longitude() == null) {
            throw new BadRequestException("COORDINATES_REQUIRED", "error.coordinates_required");
        }
    }

    @Override
    public void validateStep5(MultipartFile coverPhoto, List<String> roomNames, List<MultipartFile> roomPhotos) {
        if (coverPhoto != null && !coverPhoto.isEmpty()) {
            fileStorageService.validateAndWrapImage(coverPhoto);
        }
        if (roomPhotos != null) {
            for (MultipartFile file : roomPhotos) {
                if (file != null && !file.isEmpty()) {
                    fileStorageService.validateAndWrapImage(file);
                }
            }
        }
    }

    @Override
    public void validateStep6(GymCreateStep6Request request) {
        if (request.subscriptions() == null || request.subscriptions().isEmpty()) {
            throw new BadRequestException("SUBSCRIPTION_REQUIRED", "error.subscription_required");
        }
        List<CompletableFuture<Boolean>> futures = request.subscriptions().stream()
                .map(subReq -> CompletableFuture.supplyAsync(
                        () -> orderServiceGrpcClient.checkPackageExists(subReq.packageId()),
                        imageUploadExecutor
                ))
                .toList();

        List<Boolean> results = futures.stream().map(CompletableFuture::join).toList();
        if (results.contains(false)) {
            throw new BadRequestException("PACKAGE_NOT_FOUND", "error.package_not_found");
        }
    }

    @Override
    public void validateStep7(GymCreateStep7Request request) {
        log.info("validateStep7 Service - Validating admins count: {}", request.admins() != null ? request.admins().size() : 0);
        if (request.admins() == null || request.admins().isEmpty()) {
            throw new BadRequestException("ADMIN_REQUIRED", "error.admin_required");
        }
        java.util.Set<String> phoneNumbers = new java.util.HashSet<>();
        for (GymAdminCreateRequest adminReq : request.admins()) {
            String normalizedPhone = PhoneUtil.normalize(adminReq.phoneNumber());

            if (adminReq.email() != null && !adminReq.email().isBlank()) {
                if (!adminReq.email().matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$")) {
                    throw new BadRequestException("INVALID_EMAIL_FORMAT", "error.invalid_email_format");
                }
            }

            if (!phoneNumbers.add(normalizedPhone)) {
                throw new BadRequestException("DUPLICATE_ADMIN_PHONE", "error.duplicate_admin_phone");
            }
        }
    }
}
