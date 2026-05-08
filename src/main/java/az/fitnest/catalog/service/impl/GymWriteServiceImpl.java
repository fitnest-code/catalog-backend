package az.fitnest.catalog.service.impl;

import az.fitnest.catalog.dto.response.GeocodingResponse;
import az.fitnest.catalog.dto.*;
import az.fitnest.catalog.dto.request.*;
import az.fitnest.catalog.dto.response.*;

import az.fitnest.catalog.dto.request.GymRequest;
import az.fitnest.catalog.dto.response.CheckInResponse;
import az.fitnest.catalog.exception.BadRequestException;
import az.fitnest.catalog.exception.ResourceNotFoundException;
import az.fitnest.catalog.model.entity.Address;
import az.fitnest.catalog.model.entity.Category;
import az.fitnest.catalog.model.entity.Gym;
import az.fitnest.catalog.model.entity.GymImage;
import az.fitnest.catalog.model.entity.GymSubscription;
import az.fitnest.catalog.model.entity.GymSubscriptionBenefit;
import az.fitnest.catalog.model.entity.Trainer;
import az.fitnest.catalog.model.enums.GymStatus;
import az.fitnest.catalog.repository.CategoryRepository;
import az.fitnest.catalog.repository.GymRepository;
import az.fitnest.catalog.repository.SavedGymRepository;
import az.fitnest.catalog.client.OrderServiceGrpcClient;
import az.fitnest.catalog.service.FileStorageService;
import az.fitnest.catalog.service.ReverseGeocodingService;
import az.fitnest.catalog.service.GymTrainerService;
import az.fitnest.catalog.service.GymQrCodeService;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import az.fitnest.catalog.util.ByteArrayMultipartFile;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.apache.tika.Tika;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
public class GymWriteServiceImpl implements az.fitnest.catalog.service.GymWriteService {

    private final GymRepository gymRepository;
    private final SavedGymRepository savedGymRepository;
    private final CategoryRepository categoryRepository;
    private final ReverseGeocodingService reverseGeocodingService;
    private final FileStorageService fileStorageService;
    private final OrderServiceGrpcClient orderServiceGrpcClient;
    private final az.fitnest.catalog.repository.GymImageRepository gymImageRepository;
    private final az.fitnest.catalog.repository.SupportedServiceRepository supportedServiceRepository;
    private final az.fitnest.catalog.client.IdentityServiceGrpcClient identityServiceGrpcClient;
    private final az.fitnest.catalog.repository.GymAdminRepository gymAdminRepository;
    private final GymTrainerService gymTrainerService;
    private final GymQrCodeService gymQrCodeService;
    private final java.util.concurrent.Executor qrcodeExecutor;

    private record RoomImageUploadResult(String roomName, String url) {}

    private final java.util.Map<String, java.util.Set<az.fitnest.catalog.model.enums.GymWorkHourPeriod>> periodCache = new java.util.concurrent.ConcurrentHashMap<>();

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
        if (request.categoryIds() == null || request.categoryIds().isEmpty()) {
            throw new BadRequestException("CATEGORY_REQUIRED", "error.category_required");
        }
        List<Category> categories = categoryRepository.findAllById(request.categoryIds());
        if (categories.size() != request.categoryIds().size()) {
            throw new BadRequestException("INVALID_CATEGORIES", "error.invalid_categories");
        }
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

        gym.setPhone(request.phone());
        gym.setEmail(request.email());
        gym.setCategories(new HashSet<>(categories));

        gym.setGeneralWorkHours(mapWorkHours(request.generalWorkHours()));
        gym.setWorkHoursWoman(mapWorkHours(request.workHoursWoman()));
        gym.setWorkHoursMan(mapWorkHours(request.workHoursMan()));

        if (request.restDays() != null) {
            Set<az.fitnest.catalog.model.enums.GymWorkHourPeriod> restDays = request.restDays().stream()
                    .flatMap(r -> expandPeriods(r.period()).stream())
                    .collect(java.util.stream.Collectors.toSet());

            validateNoWorkHoursOnRestDays(request.generalWorkHours(), restDays, "general");
            validateNoWorkHoursOnRestDays(request.workHoursWoman(), restDays, "woman");
            validateNoWorkHoursOnRestDays(request.workHoursMan(), restDays, "man");

            gym.setRestDays(restDays);
        }

        gym.setStatus(request.status() != null ? request.status() : GymStatus.ACTIVE);

        Gym saved = gymRepository.save(gym);

        gymQrCodeService.generateAndSaveQrCode(saved.getId());
    }

    @CacheEvict(cacheNames = "gym-detail", key = "#gymId")
    public void updateGym(Long gymId, GymRequest request) {
        GeocodingResponse geocoding = reverseGeocodingService.reverseGeocode(request.address().latitude(), request.address().longitude());
        updateGymInternal(gymId, request, geocoding);
    }

    @Transactional
    protected void updateGymInternal(Long gymId, GymRequest request, GeocodingResponse geocoding) {
        if (request.categoryIds() == null || request.categoryIds().isEmpty()) {
            throw new BadRequestException("CATEGORY_REQUIRED", "error.category_required");
        }
        List<Category> categories = categoryRepository.findAllById(request.categoryIds());
        if (categories.size() != request.categoryIds().size()) {
            throw new BadRequestException("INVALID_CATEGORIES", "error.invalid_categories");
        }
        Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));

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

        gym.setPhone(request.phone());
        gym.setEmail(request.email());
        gym.setCategories(new HashSet<>(categories));

        updateWorkHours(gym.getGeneralWorkHours(), request.generalWorkHours());
        updateWorkHours(gym.getWorkHoursWoman(), request.workHoursWoman());
        updateWorkHours(gym.getWorkHoursMan(), request.workHoursMan());

        if (request.restDays() != null) {
            Set<az.fitnest.catalog.model.enums.GymWorkHourPeriod> restDays = request.restDays().stream()
                    .flatMap(r -> expandPeriods(r.period()).stream())
                    .collect(java.util.stream.Collectors.toSet());

            validateNoWorkHoursOnRestDays(request.generalWorkHours(), restDays, "general");
            validateNoWorkHoursOnRestDays(request.workHoursWoman(), restDays, "woman");
            validateNoWorkHoursOnRestDays(request.workHoursMan(), restDays, "man");

            gym.getRestDays().clear();
            gym.getRestDays().addAll(restDays);
        }

        gym.setStatus(request.status() != null ? request.status() : GymStatus.ACTIVE);

        gymRepository.save(gym);
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
        subscription.setSupportedServices(new java.util.HashSet<>());
        gym.getSubscriptions().add(subscription);
        gymRepository.save(gym);
    }

    @Transactional
    @CacheEvict(cacheNames = "gym-detail", key = "#gymId")
    public void updateGymSubscriptionBenefits(Long gymId, Long packageId, az.fitnest.catalog.dto.request.GymSubscriptionBenefitsUpdateRequest request) {
        Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));
        GymSubscription subscription = gym.getSubscriptions().stream()
                .filter(sub -> sub.getPackageId() != null && sub.getPackageId().equals(packageId))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("SUBSCRIPTION_NOT_ENABLED", "error.subscription_not_enabled"));

        if (request.benefitIds() != null) {
            List<az.fitnest.catalog.model.entity.SupportedService> services = supportedServiceRepository.findAllById(request.benefitIds()).stream()
                    .filter(s -> s.getGymId() == null || s.getGymId().equals(gymId))
                    .toList();
            subscription.setSupportedServices(new java.util.HashSet<>(services));
        }

        gymRepository.save(gym);
    }

    @Transactional
    @CacheEvict(cacheNames = "gym-detail", key = "#gymId")
    public void deleteGym(Long gymId) {
        Gym gym = gymRepository.findById(gymId).orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));

        List<String> filesToDelete = new java.util.ArrayList<>();
        if (gym.getCoverImageUrl() != null) filesToDelete.add(gym.getCoverImageUrl());
        if (gym.getQrCodeUrl() != null) filesToDelete.add(gym.getQrCodeUrl());

        if (gym.getImages() != null) {
            filesToDelete.addAll(gym.getImages().stream().map(GymImage::getUrl).toList());
        }

        if (gym.getTrainers() != null) {
            filesToDelete.addAll(gym.getTrainers().stream().map(Trainer::getPicture).filter(java.util.Objects::nonNull).toList());
        }

        gymAdminRepository.deleteAllByGymId(gymId);

        if (gym.getRooms() != null) {
            filesToDelete.addAll(gym.getRooms().stream()
                    .flatMap(r -> r.getImages().stream())
                    .map(az.fitnest.catalog.model.entity.RoomImage::getPictureUrl)
                    .toList());
        }

        gymRepository.delete(gym);

        fileStorageService.deleteFilesAfterCommit(filesToDelete);
    }

    @Transactional
    public boolean toggleSave(Object principal, Long gymId) {
        Long userId = az.fitnest.catalog.util.UserContext.extractUserId(principal);
        if (userId == null) throw new az.fitnest.catalog.exception.ForbiddenException("error.unauthorized", "UNAUTHORIZED");

        java.util.Optional<az.fitnest.catalog.model.entity.SavedGym> existing = savedGymRepository.findByUserIdAndGymId(userId, gymId);
        if (existing.isPresent()) {
            savedGymRepository.delete(existing.get());
            return false;
        } else {
            Gym gym = gymRepository.findById(gymId).orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));
            az.fitnest.catalog.model.entity.SavedGym saved = new az.fitnest.catalog.model.entity.SavedGym();
            saved.setUserId(userId);
            saved.setGym(gym);
            savedGymRepository.save(saved);
            return true;
        }
    }

    public CheckInResponse checkIn(Object principal, Long gymId) {
        Long userId = az.fitnest.catalog.util.UserContext.extractUserId(principal);
        if (userId == null) throw new az.fitnest.catalog.exception.ForbiddenException("error.unauthorized", "UNAUTHORIZED");

        Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));

        orderServiceGrpcClient.checkIn(userId, gymId);

        String addressText = gym.getAddress() != null ? gym.getAddress().getAddressText() : null;
        LocalDateTime now = LocalDateTime.now();

        return new CheckInResponse(addressText, now.toLocalDate(), now.toLocalTime());
    }

    private void safeDeleteFile(String url) {
        fileStorageService.deleteFileAsync(url);
    }

    @CacheEvict(cacheNames = "gym-images", key = "#gymId")
    public void addRoomImages(Long gymId, List<String> roomNames, List<MultipartFile> files) {
        if (roomNames.size() != files.size()) {
            throw new BadRequestException("INVALID_INPUT", "error.invalid_input");
        }

        java.util.List<java.util.concurrent.CompletableFuture<RoomImageUploadResult>> futures = new java.util.ArrayList<>();

        for (int i = 0; i < files.size(); i++) {
            MultipartFile originalFile = files.get(i);
            if (originalFile == null || originalFile.isEmpty()) continue;

            String roomName = roomNames.get(i);
            MultipartFile validatedFile = fileStorageService.validateAndWrapImage(originalFile);

            futures.add(java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                String fsId = fileStorageService.saveFile(validatedFile, "/gyms/rooms");
                return new RoomImageUploadResult(roomName, "/api/v1/media/stream/" + fsId);
            }));
        }

        java.util.List<RoomImageUploadResult> results = futures.stream()
                .map(java.util.concurrent.CompletableFuture::join)
                .toList();

        if (!results.isEmpty()) {
            applyRoomImagesUpdateInternal(gymId, results);
        }
    }

    @Transactional
    protected void applyRoomImagesUpdateInternal(Long gymId, List<RoomImageUploadResult> results) {
        Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));

        for (RoomImageUploadResult res : results) {
            az.fitnest.catalog.model.entity.Room room = gym.getRooms().stream()
                    .filter(r -> r.getName().equals(res.roomName()))
                    .findFirst()
                    .orElseGet(() -> {
                        az.fitnest.catalog.model.entity.Room newRoom = az.fitnest.catalog.model.entity.Room.builder()
                                .name(res.roomName())
                                .gym(gym)
                                .build();
                        gym.getRooms().add(newRoom);
                        return newRoom;
                    });

            az.fitnest.catalog.model.entity.RoomImage roomImage = az.fitnest.catalog.model.entity.RoomImage.builder()
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
                    .map(az.fitnest.catalog.model.entity.RoomImage::getPictureUrl)
                    .filter(url -> url != null && !url.isBlank())
                    .toList();

            if (!roomImageUrls.isEmpty()) {
                try {
                    fileStorageService.deleteFiles(roomImageUrls);
                } catch (Exception e) {
                    log.error("Failed to delete room images for gym {}: {}", gymId, e.getMessage());
                }
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

        az.fitnest.catalog.model.entity.Room room = gym.getRooms().stream()
                .filter(r -> r.getId().equals(roomId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("ROOM_NOT_FOUND", "error.room_not_found"));

        List<String> roomImageUrls = room.getImages().stream()
                .map(az.fitnest.catalog.model.entity.RoomImage::getPictureUrl)
                .filter(url -> url != null && !url.isBlank())
                .toList();

        if (!roomImageUrls.isEmpty()) {
            try {
                fileStorageService.deleteFiles(roomImageUrls);
            } catch (Exception e) {
                log.error("Failed to delete room images for room {}: {}", roomId, e.getMessage());
            }
        }

        gym.getRooms().remove(room);
        gymRepository.save(gym);
    }

    @Transactional
    @CacheEvict(cacheNames = "gym-images", key = "#gymId")
    public void deleteRoomImageById(Long gymId, Long imageId) {
        Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));

        for (az.fitnest.catalog.model.entity.Room room : gym.getRooms()) {
            java.util.Optional<az.fitnest.catalog.model.entity.RoomImage> roomImageOpt = room.getImages().stream()
                    .filter(img -> img.getId().equals(imageId))
                    .findFirst();

            if (roomImageOpt.isPresent()) {
                az.fitnest.catalog.model.entity.RoomImage roomImage = roomImageOpt.get();
                if (roomImage.getPictureUrl() != null && !roomImage.getPictureUrl().isBlank()) {
                    safeDeleteFile(roomImage.getPictureUrl());
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
        String fsId = fileStorageService.saveFile(validatedFile, "/gyms/covers");
        String url = "/api/v1/media/stream/" + fsId;
        updateCoverImageInternal(gymId, url);
    }

    @Transactional
    protected void updateCoverImageInternal(Long gymId, String url) {
        Gym gym = gymRepository.findById(gymId).orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));
        if (gym.getCoverImageUrl() != null) safeDeleteFile(gym.getCoverImageUrl());
        gym.setCoverImageUrl(url);
        gymRepository.save(gym);
    }

    @Transactional
    public void deleteAllGyms() {
        List<String> filesToDelete = new java.util.ArrayList<>();
        try {
            filesToDelete.addAll(gymRepository.findAllCoverImageUrls());
            filesToDelete.addAll(gymRepository.findAllQrCodeUrls());
            filesToDelete.addAll(gymImageRepository.findAllUrls());
            filesToDelete.addAll(gymRepository.findAllTrainerPictureUrls());
            filesToDelete.addAll(gymRepository.findAllRoomImageUrls());
        } catch (Exception e) {
            log.error("Failed to gather URLs for mass deletion: {}", e.getMessage());
        }

        gymAdminRepository.deleteAllInBatch();
        savedGymRepository.deleteAllInBatch();
        gymRepository.truncateAllGymData();

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
    public az.fitnest.catalog.dto.response.GymCreateStep1Response createGymStep1(az.fitnest.catalog.dto.request.GymCreateStep1Request request) {
        if (request.categoryId() == null) {
            throw new BadRequestException("CATEGORY_REQUIRED", "error.category_required");
        }
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("CATEGORY_NOT_FOUND", "error.category_not_found"));
        Gym gym = new Gym();
        gym.setName(request.name());
        gym.setDescription(request.description());
        gym.setPhone(request.phone());
        gym.setEmail(request.email());
        gym.setCategories(new HashSet<>(List.of(category)));
        gym.setStatus(GymStatus.DRAFT);
        gym.setCreationStep(1);
        gym = gymRepository.save(gym);
        return new az.fitnest.catalog.dto.response.GymCreateStep1Response(gym.getId());
    }

    @Transactional
    public void createGymStep2(Long id, List<String> names, List<String> surnames, List<Long> professionIds, List<String> emails, List<String> phones, List<MultipartFile> photos) {
        Gym gym = gymRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));
        validateStep(gym, 1);

        gymTrainerService.addTrainers(id, names, surnames, professionIds, emails, phones, photos);

        updateStep(gym, 1);
    }

    @Transactional
    @CacheEvict(cacheNames = "gym-detail", key = "#gymId")
    public void deleteTrainer(Long gymId, Long trainerId) {
        Gym gym = gymRepository.findById(gymId).orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));
        Trainer trainer = gym.getTrainers().stream().filter(t -> t.getId().equals(trainerId)).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("TRAINER_NOT_FOUND", "error.trainer_not_found"));
        if (trainer.getPicture() != null) safeDeleteFile(trainer.getPicture());
        gym.getTrainers().remove(trainer);
        gymRepository.save(gym);
    }

    @Transactional
    public void createGymStep3(Long gymId, az.fitnest.catalog.dto.request.GymCreateStep2Request request) {
        Gym gym = gymRepository.findById(gymId).orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));
        validateStep(gym, 2);

        java.util.Set<az.fitnest.catalog.model.enums.GymWorkHourPeriod> restDays = new java.util.HashSet<>();
        if (request.restDays() != null) {
            restDays = request.restDays().stream()
                    .flatMap(r -> expandPeriods(r.period()).stream())
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

    private void validateNoWorkHoursOnRestDays(java.util.Set<az.fitnest.catalog.dto.response.GymWorkHourResponse> workHours, java.util.Set<az.fitnest.catalog.model.enums.GymWorkHourPeriod> restDays, String type) {
        if (workHours == null || restDays.isEmpty()) return;
        for (az.fitnest.catalog.dto.response.GymWorkHourResponse wh : workHours) {
            java.util.Set<az.fitnest.catalog.model.enums.GymWorkHourPeriod> whPeriods = expandPeriods(wh.period());
            for (az.fitnest.catalog.model.enums.GymWorkHourPeriod p : whPeriods) {
                if (restDays.contains(p)) {
                    throw new BadRequestException("WORK_HOURS_ON_REST_DAY", "error.work_hours_on_rest_day");
                }
            }
        }
    }

    public void createGymStep4(Long gymId, az.fitnest.catalog.dto.request.GymCreateStep3Request request) {
        GeocodingResponse geocoding = reverseGeocodingService.reverseGeocode(request.latitude(), request.longitude());
        createGymStep4Internal(gymId, request, geocoding);
    }

    @Transactional
    protected void createGymStep4Internal(Long gymId, az.fitnest.catalog.dto.request.GymCreateStep3Request request, GeocodingResponse geocoding) {
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

        updateStep(gym, 3);
    }

    public void createGymStep5(Long gymId, MultipartFile coverPhoto, List<String> roomNames, List<MultipartFile> roomPhotos) {
        if (coverPhoto != null && !coverPhoto.isEmpty()) {
            updateCoverImage(gymId, coverPhoto);
        }
        if (roomNames != null && roomPhotos != null && roomNames.size() == roomPhotos.size() && !roomNames.isEmpty()) {
            addRoomImages(gymId, roomNames, roomPhotos);
        }
        completeStep5Internal(gymId);
    }

    @Transactional
    protected void completeStep5Internal(Long gymId) {
        Gym gym = gymRepository.findById(gymId).orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));
        validateStep(gym, 4);
        updateStep(gym, 4);
    }

    @Transactional
    public void createGymStep6(Long gymId, az.fitnest.catalog.dto.request.GymCreateStep6Request request) {
        Gym gym = gymRepository.findById(gymId).orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));
        validateStep(gym, 5);
        gym.getSubscriptions().clear();

        java.util.Set<Long> processedPackages = new java.util.HashSet<>();

        for (az.fitnest.catalog.dto.request.GymCreateStep6SubscriptionRequest subReq : request.subscriptions()) {
            if (!processedPackages.add(subReq.packageId())) {
                continue;
            }
            GymSubscription subscription = new GymSubscription();
            subscription.setGym(gym);
            subscription.setPackageId(subReq.packageId());
            subscription.setDailyPrice(subReq.dailyPrice());
            if (subReq.supportedServicesId() != null && !subReq.supportedServicesId().isEmpty()) {
                List<az.fitnest.catalog.model.entity.SupportedService> services = supportedServiceRepository.findAllById(subReq.supportedServicesId()).stream()
                        .filter(s -> s.getGymId() == null || s.getGymId().equals(gymId))
                        .toList();
                subscription.setSupportedServices(new HashSet<>(services));
            }
            gym.getSubscriptions().add(subscription);
        }
        updateStep(gym, 5);
    }

    @Caching(evict = {
        @CacheEvict(cacheNames = {"main-page-gyms", "admin-gyms"}, allEntries = true)
    })
    public void createGymStep7(Long gymId, az.fitnest.catalog.dto.request.GymCreateStep7Request request) {
        for (az.fitnest.catalog.dto.request.GymAdminCreateRequest adminReq : request.admins()) {
            identityServiceGrpcClient.createGymAdmin(adminReq.name(), adminReq.surname(), adminReq.phoneNumber(), adminReq.email(), adminReq.password());
            saveAdminInternal(gymId, adminReq);
        }
        finalizeGymStep7Internal(gymId);
    }

    @Transactional
    protected void saveAdminInternal(Long gymId, az.fitnest.catalog.dto.request.GymAdminCreateRequest adminReq) {
        Gym gym = gymRepository.findById(gymId).orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));
        az.fitnest.catalog.model.entity.GymAdmin admin = new az.fitnest.catalog.model.entity.GymAdmin();
        admin.setName(adminReq.name());
        admin.setSurname(adminReq.surname());
        admin.setPhoneNumber(adminReq.phoneNumber());
        admin.setEmail(adminReq.email());
        admin.setGym(gym);
        gymAdminRepository.save(admin);
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
        if (gym.getStatus() == az.fitnest.catalog.model.enums.GymStatus.ACTIVE ||
            gym.getStatus() == az.fitnest.catalog.model.enums.GymStatus.INACTIVE) {
            throw new BadRequestException("GYM_NOT_EDITABLE", "error.gym_not_editable_via_steps");
        }
        Integer currentStep = gym.getCreationStep() != null ? gym.getCreationStep() : 1;
        if (currentStep < requiredStep) {
            throw new BadRequestException("INVALID_STEP", "error.invalid_step");
        }
    }

    private void updateStep(Gym gym, int completedStep) {
        Integer currentStep = gym.getCreationStep() != null ? gym.getCreationStep() : 1;
        if (currentStep == completedStep) {
            gym.setCreationStep(completedStep + 1);
            gymRepository.save(gym);
        }
    }

    @Transactional
    public void createSupportedService(az.fitnest.catalog.dto.request.SupportedServiceRequest request) {
        az.fitnest.catalog.model.entity.SupportedService service = new az.fitnest.catalog.model.entity.SupportedService();
        service.setName(request.name());
        service.setGymId(request.gymId());
        supportedServiceRepository.save(service);
    }

    @Transactional
    public void deleteSupportedService(Long id) {
        supportedServiceRepository.deleteById(id);
    }

    @Override
    public GeocodingResponse reverseGeocode(Double lat, Double lng) {
        return reverseGeocodingService.reverseGeocode(lat, lng);
    }

    @Transactional
    public void toggleGymStatus(Long gymId, boolean enabled) {
        Gym gym = gymRepository.findById(gymId).orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));
        gym.setStatus(enabled ? GymStatus.ACTIVE : GymStatus.INACTIVE);
        gymRepository.save(gym);
    }

    private Address mapAddress(az.fitnest.catalog.dto.response.AddressResponse dto) {
        if (dto == null) return null;
        Address address = new Address();
        address.setLatitude(dto.latitude());
        address.setLongitude(dto.longitude());
        GeocodingResponse geocoding = reverseGeocodingService.reverseGeocode(dto.latitude(), dto.longitude());
        if (geocoding != null) {
            address.setAddressText(geocoding.addressText());
            address.setCity(geocoding.city());
        }
        return address;
    }

    private Set<az.fitnest.catalog.model.entity.GymWorkHour> mapWorkHours(Set<az.fitnest.catalog.dto.response.GymWorkHourResponse> dtos) {
        if (dtos == null) return new HashSet<>();
        return dtos.stream()
                .flatMap(dto -> {
                    if (dto.period() == null) throw new BadRequestException("INVALID_PERIOD", "error.invalid_period");
                    return expandPeriods(dto.period()).stream()
                            .map(p -> new az.fitnest.catalog.model.entity.GymWorkHour(p, dto.from(), dto.to()));
                })
                .collect(java.util.stream.Collectors.toSet());
    }

    private java.util.Set<az.fitnest.catalog.model.enums.GymWorkHourPeriod> expandPeriods(String periodStr) {
        if (periodStr == null || periodStr.isBlank()) return java.util.Collections.emptySet();

        String upper = periodStr.toUpperCase().trim();
        return periodCache.computeIfAbsent(upper, k -> {
            if (k.contains("-")) {
                String[] parts = k.split("-");
                if (parts.length == 2) {
                    try {
                        az.fitnest.catalog.model.enums.GymWorkHourPeriod start = az.fitnest.catalog.model.enums.GymWorkHourPeriod.valueOf(parts[0].trim());
                        az.fitnest.catalog.model.enums.GymWorkHourPeriod end = az.fitnest.catalog.model.enums.GymWorkHourPeriod.valueOf(parts[1].trim());

                        java.util.Set<az.fitnest.catalog.model.enums.GymWorkHourPeriod> result = new java.util.HashSet<>();
                        int startIdx = start.ordinal();
                        int endIdx = end.ordinal();

                        if (startIdx <= endIdx) {
                            for (int i = startIdx; i <= endIdx; i++) {
                                result.add(az.fitnest.catalog.model.enums.GymWorkHourPeriod.values()[i]);
                            }
                        } else {
                            for (int i = startIdx; i < 7; i++) {
                                result.add(az.fitnest.catalog.model.enums.GymWorkHourPeriod.values()[i]);
                            }
                            for (int i = 0; i <= endIdx; i++) {
                                result.add(az.fitnest.catalog.model.enums.GymWorkHourPeriod.values()[i]);
                            }
                        }
                        return java.util.Collections.unmodifiableSet(result);
                    } catch (Exception e) {
                        log.error("Failed to expand period range: {}", k, e);
                        throw new BadRequestException("INVALID_PERIOD_RANGE", "error.invalid_period_range");
                    }
                }
            }

            try {
                return java.util.Set.of(az.fitnest.catalog.model.enums.GymWorkHourPeriod.valueOf(k));
            } catch (IllegalArgumentException e) {
                log.error("Invalid work hour period: {}", k);
                throw new BadRequestException("INVALID_PERIOD", "error.invalid_period");
            }
        });
    }

    private void updateWorkHours(Set<az.fitnest.catalog.model.entity.GymWorkHour> target, Set<az.fitnest.catalog.dto.response.GymWorkHourResponse> source) {
        target.clear();
        if (source != null) {
            target.addAll(mapWorkHours(source));
        }
    }
}
