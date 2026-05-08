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

    private static final java.util.Set<String> ALLOWED_MIME_TYPES = java.util.Set.of("image/jpeg", "image/png", "image/webp");
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    @Transactional
    @Caching(evict = {
        @CacheEvict(cacheNames = "main-page-gyms", allEntries = true),
        @CacheEvict(cacheNames = "admin-gyms", allEntries = true)
    })
    public void createGym(GymRequest request) {
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

        if (request.address() != null) {
            Address address = new Address();
            Double lat = request.address().latitude();
            Double lng = request.address().longitude();
            address.setLatitude(lat);
            address.setLongitude(lng);
            GeocodingResponse geocoding = reverseGeocodingService.reverseGeocode(lat, lng);
            if (geocoding != null) {
                address.setAddressText(geocoding.addressText());
                address.setCity(geocoding.city());
            }
            gym.setAddress(address);
        }
        gym.setPhone(request.phone());
        gym.setEmail(request.email());
        gym.setCategories(new HashSet<>(categories));

        if (request.generalWorkHours() != null) {
            Set<az.fitnest.catalog.model.entity.GymWorkHour> generalWorkHours = request.generalWorkHours().stream()
                    .map(dto -> {
                        if (dto.period() == null) throw new BadRequestException("INVALID_PERIOD", "error.invalid_period");
                        return new az.fitnest.catalog.model.entity.GymWorkHour(az.fitnest.catalog.model.enums.GymWorkHourPeriod.valueOf(dto.period()), dto.from(), dto.to());
                    })
                    .collect(java.util.stream.Collectors.toSet());
            gym.setGeneralWorkHours(generalWorkHours);
        }

        if (request.workHoursWoman() != null) {
            Set<az.fitnest.catalog.model.entity.GymWorkHour> workHoursWoman = request.workHoursWoman().stream()
                    .map(dto -> {
                        if (dto.period() == null) throw new BadRequestException("INVALID_PERIOD", "error.invalid_period");
                        return new az.fitnest.catalog.model.entity.GymWorkHour(az.fitnest.catalog.model.enums.GymWorkHourPeriod.valueOf(dto.period()), dto.from(), dto.to());
                    })
                    .collect(java.util.stream.Collectors.toSet());
            gym.setWorkHoursWoman(workHoursWoman);
        }

        if (request.workHoursMan() != null) {
            Set<az.fitnest.catalog.model.entity.GymWorkHour> workHoursMan = request.workHoursMan().stream()
                    .map(dto -> {
                        if (dto.period() == null) throw new BadRequestException("INVALID_PERIOD", "error.invalid_period");
                        return new az.fitnest.catalog.model.entity.GymWorkHour(az.fitnest.catalog.model.enums.GymWorkHourPeriod.valueOf(dto.period()), dto.from(), dto.to());
                    })
                    .collect(java.util.stream.Collectors.toSet());
            gym.setWorkHoursMan(workHoursMan);
        }

        if (request.restDays() != null) {
            Set<az.fitnest.catalog.model.enums.GymWorkHourPeriod> restDays = request.restDays().stream()
                    .map(r -> az.fitnest.catalog.model.enums.GymWorkHourPeriod.valueOf(r.period().toUpperCase()))
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

    @Transactional
    @CacheEvict(cacheNames = "gym-detail", key = "#gymId")
    public void updateGym(Long gymId, GymRequest request) {
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

        if (request.address() != null) {
            Address address = new Address();
            Double lat = request.address().latitude();
            Double lng = request.address().longitude();
            address.setLatitude(lat);
            address.setLongitude(lng);
            GeocodingResponse geocoding = reverseGeocodingService.reverseGeocode(lat, lng);
            if (geocoding != null) {
                address.setAddressText(geocoding.addressText());
                address.setCity(geocoding.city());
            }
            gym.setAddress(address);
        }
        gym.setPhone(request.phone());
        gym.setEmail(request.email());
        gym.setCategories(new HashSet<>(categories));
        gym.getGeneralWorkHours().clear();
        if (request.generalWorkHours() != null) {
            Set<az.fitnest.catalog.model.entity.GymWorkHour> newGeneralWorkHours = request.generalWorkHours().stream()
                    .map(dto -> {
                        if (dto.period() == null) throw new BadRequestException("INVALID_PERIOD", "error.invalid_period");
                        return new az.fitnest.catalog.model.entity.GymWorkHour(az.fitnest.catalog.model.enums.GymWorkHourPeriod.valueOf(dto.period()), dto.from(), dto.to());
                    })
                    .collect(java.util.stream.Collectors.toSet());
            gym.getGeneralWorkHours().addAll(newGeneralWorkHours);
        }

        gym.getWorkHoursWoman().clear();
        if (request.workHoursWoman() != null) {
            Set<az.fitnest.catalog.model.entity.GymWorkHour> newWorkHoursWoman = request.workHoursWoman().stream()
                    .map(dto -> {
                        if (dto.period() == null) throw new BadRequestException("INVALID_PERIOD", "error.invalid_period");
                        return new az.fitnest.catalog.model.entity.GymWorkHour(az.fitnest.catalog.model.enums.GymWorkHourPeriod.valueOf(dto.period()), dto.from(), dto.to());
                    })
                    .collect(java.util.stream.Collectors.toSet());
            gym.getWorkHoursWoman().addAll(newWorkHoursWoman);
        }

        gym.getWorkHoursMan().clear();
        if (request.workHoursMan() != null) {
            Set<az.fitnest.catalog.model.entity.GymWorkHour> newWorkHoursMan = request.workHoursMan().stream()
                    .map(dto -> {
                        if (dto.period() == null) throw new BadRequestException("INVALID_PERIOD", "error.invalid_period");
                        return new az.fitnest.catalog.model.entity.GymWorkHour(az.fitnest.catalog.model.enums.GymWorkHourPeriod.valueOf(dto.period()), dto.from(), dto.to());
                    })
                    .collect(java.util.stream.Collectors.toSet());
            gym.getWorkHoursMan().addAll(newWorkHoursMan);
        }

        if (request.restDays() != null) {
            Set<az.fitnest.catalog.model.enums.GymWorkHourPeriod> restDays = request.restDays().stream()
                    .map(r -> az.fitnest.catalog.model.enums.GymWorkHourPeriod.valueOf(r.period().toUpperCase()))
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
            List<az.fitnest.catalog.model.entity.SupportedService> services = supportedServiceRepository.findAllById(request.benefitIds());
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
    public boolean toggleSave(Long userId, Long gymId) {
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

    @Transactional
    public CheckInResponse checkIn(Long userId, Long gymId) {
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

    @Transactional
    @CacheEvict(cacheNames = "gym-images", key = "#gymId")
    public void addRoomImages(Long gymId, List<String> roomNames, List<MultipartFile> files) {
        Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));

        if (roomNames.size() != files.size()) {
            throw new BadRequestException("INVALID_INPUT", "error.invalid_input");
        }

        for (int i = 0; i < files.size(); i++) {
            MultipartFile originalFile = files.get(i);
            if (originalFile == null || originalFile.isEmpty()) {
                continue;
            }

            String roomName = roomNames.get(i);
            MultipartFile validatedFile = fileStorageService.validateAndWrapImage(originalFile);

            String fsId = fileStorageService.saveFile(validatedFile, "/gyms/rooms");
            String url = "/api/v1/media/stream/" + fsId;

            az.fitnest.catalog.model.entity.Room room = gym.getRooms().stream()
                    .filter(r -> r.getName().equals(roomName))
                    .findFirst()
                    .orElseGet(() -> {
                        az.fitnest.catalog.model.entity.Room newRoom = az.fitnest.catalog.model.entity.Room.builder()
                                .name(roomName)
                                .gym(gym)
                                .build();
                        gym.getRooms().add(newRoom);
                        return newRoom;
                    });

            az.fitnest.catalog.model.entity.RoomImage roomImage = az.fitnest.catalog.model.entity.RoomImage.builder()
                    .room(room)
                    .pictureUrl(url)
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
                } catch (Exception e) {}
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
            } catch (Exception e) {}
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

    @Transactional
    @CacheEvict(cacheNames = "gym-detail", key = "#gymId")
    public void updateCoverImage(Long gymId, MultipartFile coverPhoto) {
        MultipartFile validatedFile = fileStorageService.validateAndWrapImage(coverPhoto);
        Gym gym = gymRepository.findById(gymId).orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));
        if (gym.getCoverImageUrl() != null) safeDeleteFile(gym.getCoverImageUrl());
        String fsId = fileStorageService.saveFile(validatedFile, "/gyms/covers");
        gym.setCoverImageUrl("/api/v1/media/stream/" + fsId);
        gymRepository.save(gym);
    }

    @Transactional
    public void deleteAllGyms() {
        List<Gym> gyms = gymRepository.findAll();
        for (Gym gym : gyms) {
            deleteGym(gym.getId());
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

    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) return "unnamed";
        return filename.replaceAll("[^a-zA-Z0-9.\\-_]", "_");
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
                    .map(r -> az.fitnest.catalog.model.enums.GymWorkHourPeriod.valueOf(r.period().toUpperCase()))
                    .collect(java.util.stream.Collectors.toSet());
        }

        validateNoWorkHoursOnRestDays(request.generalWorkHours(), restDays, "general");
        validateNoWorkHoursOnRestDays(request.workHoursWoman(), restDays, "woman");
        validateNoWorkHoursOnRestDays(request.workHoursMan(), restDays, "man");

        gym.getGeneralWorkHours().clear();
        if (request.generalWorkHours() != null) {
            gym.setGeneralWorkHours(request.generalWorkHours().stream().map(dto -> new az.fitnest.catalog.model.entity.GymWorkHour(az.fitnest.catalog.model.enums.GymWorkHourPeriod.valueOf(dto.period().toUpperCase()), dto.from(), dto.to())).collect(java.util.stream.Collectors.toSet()));
        }
        gym.getWorkHoursWoman().clear();
        if (request.workHoursWoman() != null) {
            gym.setWorkHoursWoman(request.workHoursWoman().stream().map(dto -> new az.fitnest.catalog.model.entity.GymWorkHour(az.fitnest.catalog.model.enums.GymWorkHourPeriod.valueOf(dto.period().toUpperCase()), dto.from(), dto.to())).collect(java.util.stream.Collectors.toSet()));
        }
        gym.getWorkHoursMan().clear();
        if (request.workHoursMan() != null) {
            gym.setWorkHoursMan(request.workHoursMan().stream().map(dto -> new az.fitnest.catalog.model.entity.GymWorkHour(az.fitnest.catalog.model.enums.GymWorkHourPeriod.valueOf(dto.period().toUpperCase()), dto.from(), dto.to())).collect(java.util.stream.Collectors.toSet()));
        }

        gym.getRestDays().clear();
        gym.getRestDays().addAll(restDays);

        updateStep(gym, 2);
    }

    private void validateNoWorkHoursOnRestDays(java.util.Set<az.fitnest.catalog.dto.response.GymWorkHourResponse> workHours, java.util.Set<az.fitnest.catalog.model.enums.GymWorkHourPeriod> restDays, String type) {
        if (workHours == null || restDays.isEmpty()) return;
        for (az.fitnest.catalog.dto.response.GymWorkHourResponse wh : workHours) {
            if (restDays.contains(az.fitnest.catalog.model.enums.GymWorkHourPeriod.valueOf(wh.period().toUpperCase()))) {
                throw new BadRequestException("WORK_HOURS_ON_REST_DAY", "error.work_hours_on_rest_day");
            }
        }
    }

    @Transactional
    public void createGymStep4(Long gymId, az.fitnest.catalog.dto.request.GymCreateStep3Request request) {
        Gym gym = gymRepository.findById(gymId).orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));
        validateStep(gym, 3);
        Address address = new Address();
        address.setLatitude(request.latitude());
        address.setLongitude(request.longitude());
        GeocodingResponse geocoding = reverseGeocodingService.reverseGeocode(request.latitude(), request.longitude());
        if (geocoding != null) {
            address.setAddressText(geocoding.addressText());
            address.setCity(geocoding.city());
        }
        gym.setAddress(address);
        updateStep(gym, 3);
    }

    @Transactional
    @CacheEvict(cacheNames = "gym-images", key = "#gymId")
    public void createGymStep5(Long gymId, MultipartFile coverPhoto, List<String> roomNames, List<MultipartFile> roomPhotos) {
        Gym gym = gymRepository.findById(gymId).orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));
        validateStep(gym, 4);
        updateCoverImage(gymId, coverPhoto);
        if (roomNames != null && roomPhotos != null && roomNames.size() == roomPhotos.size() && !roomNames.isEmpty()) {
            addRoomImages(gymId, roomNames, roomPhotos);
        }
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
            subscription.setPrice(subReq.price());
            if (subReq.supportedServicesId() != null && !subReq.supportedServicesId().isEmpty()) {
                List<az.fitnest.catalog.model.entity.SupportedService> services = supportedServiceRepository.findAllById(subReq.supportedServicesId());
                subscription.setSupportedServices(new HashSet<>(services));
            }
            gym.getSubscriptions().add(subscription);
        }
        updateStep(gym, 5);
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(cacheNames = {"main-page-gyms", "admin-gyms"}, allEntries = true)
    })
    public void createGymStep7(Long gymId, az.fitnest.catalog.dto.request.GymCreateStep7Request request) {
        Gym gym = gymRepository.findById(gymId).orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));
        validateStep(gym, 6);

        for (az.fitnest.catalog.dto.request.GymAdminCreateRequest adminReq : request.admins()) {
            identityServiceGrpcClient.createGymAdmin(adminReq.name(), adminReq.surname(), adminReq.phoneNumber(), adminReq.email(), adminReq.password());

            az.fitnest.catalog.model.entity.GymAdmin admin = new az.fitnest.catalog.model.entity.GymAdmin();
            admin.setName(adminReq.name());
            admin.setSurname(adminReq.surname());
            admin.setPhoneNumber(adminReq.phoneNumber());
            admin.setEmail(adminReq.email());
            admin.setGym(gym);
            gymAdminRepository.save(admin);
        }

        gym.setStatus(GymStatus.ACTIVE);
        gym.setCreationStep(7); // Completed
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

    @Override
    @Transactional
    public void toggleGymStatus(Long gymId, boolean enabled) {
        Gym gym = gymRepository.findById(gymId).orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));
        gym.setStatus(enabled ? GymStatus.ACTIVE : GymStatus.INACTIVE);
        gymRepository.save(gym);
    }
}
