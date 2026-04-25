package az.fitnest.catalog.service.impl;

import az.fitnest.catalog.dto.admin.*;
import az.fitnest.catalog.exception.BadRequestException;
import az.fitnest.catalog.exception.ConflictException;
import az.fitnest.catalog.exception.ResourceNotFoundException;
import az.fitnest.catalog.mapper.AdminPanelGymMapper;
import az.fitnest.catalog.model.entity.*;
import az.fitnest.catalog.model.enums.AdminPanelGymStatus;
import az.fitnest.catalog.model.enums.GymAdminRole;
import az.fitnest.catalog.model.enums.GymAdminStatus;
import az.fitnest.catalog.model.enums.RatingStatus;
import az.fitnest.catalog.repository.*;
import az.fitnest.catalog.service.AdminPanelReverseGeocodingService;
import az.fitnest.catalog.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminPanelGymWriteService {

    private static final List<String> ALLOWED_TYPES = List.of("image/jpeg", "image/jpg", "image/png");
    private static final long MAX_SIZE = 2 * 1024 * 1024;

    private final AdminPanelGymSubscriptionRepository gymSubscriptionRepository;
    private final AdminPanelReverseGeocodingService reverseGeocodingService;
    private final AdminPanelWorkingHourRepository workingHourRepository;
    private final SubscriptionTypeRepository subscriptionTypeRepository;
    private final GymServiceItemRepository gymServiceItemRepository;
    private final GymAdminPanelRepository gymAdminPanelRepository;
    private final AdminPanelGymAdminRepository adminRepository;
    private final ServiceTypeRepository serviceTypeRepository;
    private final GymRatingRepository gymRatingRepository;
    private final AdminPanelGymMapper adminPanelGymMapper;
    private final FileStorageService fileStorageService;
    private final TrainerRepository trainerRepository;
    private final LocationService locationService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AdminPanelGymResponse createGymForAdmin(AdminPanelCreateGymRequest request) {
        GymAdminPanel saved = gymAdminPanelRepository.save(adminPanelGymMapper.toEntity(request));
        return adminPanelGymMapper.toCreateResponse(saved);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = {"gym-detail", "main-page-gyms"}, allEntries = true)
    })
    public void updateGymStatus(Long gymId, AdminPanelUpdateGymStatusRequest request) {
        GymAdminPanel gym = gymAdminPanelRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));

        AdminPanelGymStatus newStatus = AdminPanelGymStatus.valueOf(request.status().name());

        if (gym.getStatus() == newStatus) {
            throw new ConflictException("SAME_STATUS", "error.same_status");
        }

        if (newStatus == AdminPanelGymStatus.ACTIVE) {
            validateActivationRequirements(gym);
        }

        gym.setStatus(newStatus);
        gymAdminPanelRepository.save(gym);
    }

    private void validateActivationRequirements(GymAdminPanel gym) {
        List<String> missing = new ArrayList<>();

        if (!StringUtils.hasText(gym.getName()))
            missing.add("name");

        if (!StringUtils.hasText(gym.getPhone()))
            missing.add("phoneNumber");

        if (gym.getAddress() == null || !StringUtils.hasText(gym.getAddress().getAddressText()))
            missing.add("address");

        if (!StringUtils.hasText(gym.getCoverImageUrl()))
            missing.add("coverImage");

        boolean hasWorkingHours = gym.getWorkingHours() != null
                && !gym.getWorkingHours().isEmpty();
        if (!hasWorkingHours)
            missing.add("ən azı 1 iş saatı");

        if (!missing.isEmpty()) {
            throw new BadRequestException(
                    "ACTIVATION_REQUIREMENTS_NOT_MET",
                    "Tamamlanmalıdır: " + String.join(", ", missing)
            );
        }
    }

    @Transactional
    public void deleteGym(Long gymId) {
        GymAdminPanel gym = gymAdminPanelRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));

        if (gym.getSubscriptions() != null && !gym.getSubscriptions().isEmpty()) {
            throw new ConflictException("GYM_HAS_ACTIVE_SUBSCRIPTIONS", "error.gym_has_active_subscriptions");
        }

        gym.setStatus(AdminPanelGymStatus.DELETED);
        gym.setDeletedAt(LocalDateTime.now());
        gymAdminPanelRepository.save(gym);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = {"gym-detail", "main-page-gyms"}, allEntries = true)
    })
    public void updateGeneralInfo(Long gymId, GeneralInfoRequest request) {
        if ((request.latitude() == null) != (request.longitude() == null)) {
            throw new BadRequestException("INVALID_LOCATION", "error.lat_lng_must_be_together");
        }

        GymAdminPanel gym = gymAdminPanelRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));

        AdminPanelGeocodingResponse geocoding = reverseGeocodingService
                .reverseGeocode(request.latitude(), request.longitude());

        adminPanelGymMapper.updateGeneralInfo(gym, request, geocoding);

        if (request.latitude() != null) {
            locationService.resolveAndSetLocation(gym.getAddress(), geocoding);
        }

        gymAdminPanelRepository.save(gym);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = {"gym-detail", "main-page-gyms"}, allEntries = true),
            @CacheEvict(cacheNames = "gym-images", key = "#gymId")
    })
    public CoverImageResponse uploadCoverImage(Long gymId, MultipartFile file) {
        validateImageFile(file);

        GymAdminPanel gym = gymAdminPanelRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));

        String fsId = fileStorageService.saveFile(file, "/gyms/covers", gym.getCoverImageUrl());
        String url = "/api/v1/media/stream/" + fsId;

        gym.setCoverImageUrl(url);
        gymAdminPanelRepository.save(gym);

        return new CoverImageResponse(url);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = {"gym-detail", "main-page-gyms"}, allEntries = true),
            @CacheEvict(cacheNames = "gym-images", key = "#gymId")
    })
    public void deleteCoverImage(Long gymId) {
        GymAdminPanel gym = gymAdminPanelRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));

        if (!StringUtils.hasText(gym.getCoverImageUrl())) {
            throw new ResourceNotFoundException("COVER_IMAGE_NOT_FOUND", "error.cover_image_not_found");
        }

        fileStorageService.deleteFile(gym.getCoverImageUrl());
        gym.setCoverImageUrl(null);
        gymAdminPanelRepository.save(gym);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = {"gym-detail", "main-page-gyms"}, allEntries = true),
            @CacheEvict(cacheNames = "gym-images", key = "#gymId")
    })
    public void addGalleryImage(Long gymId, MultipartFile file, Integer sortOrder) {
        validateImageFile(file);

        GymAdminPanel gym = gymAdminPanelRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));

        String fsId = fileStorageService.saveFile(file, "/gyms/gallery");
        String url = "/api/v1/media/stream/" + fsId;

        int order = sortOrder != null ? sortOrder :
                gym.getImages().stream()
                .mapToInt(i -> i.getSortOrder() != null ? i.getSortOrder() : 0)
                .max()
                .orElse(0) + 1;

        AdminPanelGymImage image = AdminPanelGymImage.builder()
                .url(url)
                .sortOrder(order)
                .build();

        gym.getImages().add(image);
        gymAdminPanelRepository.save(gym);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = {"gym-detail", "main-page-gyms"}, allEntries = true),
            @CacheEvict(cacheNames = "gym-images", key = "#gymId")
    })
    public void deleteGalleryImage(Long gymId, Long imageId) {
        GymAdminPanel gym = gymAdminPanelRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));

        AdminPanelGymImage image = gym.getImages().stream()
                .filter(i -> i.getId().equals(imageId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("IMAGE_NOT_FOUND", "error.image_not_found"));

        fileStorageService.deleteFile(image.getUrl());
        gym.getImages().remove(image);
        gymAdminPanelRepository.save(gym);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = {"gym-detail", "main-page-gyms"}, allEntries = true),
            @CacheEvict(cacheNames = "gym-images", key = "#gymId")
    })
    public void updateImageOrder(Long gymId, UpdateImageOrderRequest request) {
        GymAdminPanel gym = gymAdminPanelRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));

        Map<Long, AdminPanelGymImage> imageMap = gym.getImages().stream()
                .collect(Collectors.toMap(AdminPanelGymImage::getId, i -> i));

        for (UpdateImageOrderRequest.ImageOrderItem item : request.images()) {
            AdminPanelGymImage image = imageMap.get(item.id());
            if (image == null) {
                throw new ResourceNotFoundException("IMAGE_NOT_FOUND", "error.image_not_found");
            }
            image.setSortOrder(item.sortOrder());
        }

        gymAdminPanelRepository.save(gym);
    }

    @Transactional
    public WorkingHourDto addWorkingHour(Long gymId, WorkingHourRequest request) {
        gymAdminPanelRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));

        if (workingHourRepository.existsByGymIdAndDayOfWeek(gymId, request.dayOfWeek())) {
            throw new ConflictException("WORKING_HOUR_ALREADY_EXISTS", "error.working_hour_already_exists");
        }

        validateWorkingHourRequest(request);

        AdminPanelWorkingHour wh = AdminPanelWorkingHour.builder()
                .gym(gymAdminPanelRepository.getReferenceById(gymId))
                .dayOfWeek(request.dayOfWeek())
                .openTime(parseTime(request.openTime()))
                .closeTime(parseTime(request.closeTime()))
                .isClosed(request.isClosed())
                .build();

        return adminPanelGymMapper.toWorkingHourDto(workingHourRepository.save(wh));
    }

    @Transactional
    public void updateWorkingHour(Long gymId, Long workingHourId, WorkingHourRequest request) {
        AdminPanelWorkingHour wh = workingHourRepository.findByIdAndGymId(workingHourId, gymId)
                .orElseThrow(() -> new ResourceNotFoundException("WORKING_HOUR_NOT_FOUND", "error.working_hour_not_found"));

        validateWorkingHourRequest(request);
        adminPanelGymMapper.updateWorkingHour(wh, request);

        workingHourRepository.save(wh);
    }

    @Transactional
    public void deleteWorkingHour(Long gymId, Long workingHourId) {
        AdminPanelWorkingHour wh = workingHourRepository.findByIdAndGymId(workingHourId, gymId)
                .orElseThrow(() -> new ResourceNotFoundException("WORKING_HOUR_NOT_FOUND", "error.working_hour_not_found"));

        workingHourRepository.delete(wh);
    }

    @Transactional
    public TrainerDetailDto addTrainer(Long gymId, AdminPanelTrainerRequest request, MultipartFile file) {
        gymAdminPanelRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));

        if (trainerRepository.existsByPhoneAndGymId(request.phoneNumber(), gymId)) {
            throw new ConflictException("TRAINER_PHONE_EXISTS", "error.trainer_phone_exists");
        }

        String imageUrl = uploadImageIfPresent(file);

        Trainer trainer = Trainer.builder()
                .gymId(gymId)
                .firstName(request.firstName())
                .lastName(request.lastName())
                .specialization(request.specialization())
                .phone(request.phoneNumber())
                .email(request.email())
                .profileImageUrl(imageUrl)
                .build();

        return adminPanelGymMapper.toDetailDto(trainerRepository.save(trainer));
    }

    @Transactional
    public void updateTrainer(Long gymId, Long trainerId, AdminPanelTrainerRequest request, MultipartFile file) {
        Trainer trainer = trainerRepository.findByIdAndGymId(trainerId, gymId)
                .orElseThrow(() -> new ResourceNotFoundException("TRAINER_NOT_FOUND", "error.trainer_not_found"));

        if (!trainer.getPhone().equals(request.phoneNumber()) &&
                trainerRepository.existsByPhoneAndGymId(request.phoneNumber(), gymId)) {
            throw new ConflictException("TRAINER_PHONE_EXISTS", "error.trainer_phone_exists");
        }

        adminPanelGymMapper.updateTrainer(trainer, request);

        if (file != null && !file.isEmpty()) {
            validateImageFile(file);
            String fsId = fileStorageService.saveFile(file, "/trainers", trainer.getProfileImageUrl());
            trainer.setProfileImageUrl("/api/v1/media/stream/" + fsId);
        }

        trainerRepository.save(trainer);
    }

    @Transactional
    public void deleteTrainer(Long gymId, Long trainerId) {
        Trainer trainer = trainerRepository.findByIdAndGymId(trainerId, gymId)
                .orElseThrow(() -> new ResourceNotFoundException("TRAINER_NOT_FOUND", "error.trainer_not_found"));

        if (StringUtils.hasText(trainer.getProfileImageUrl())) {
            fileStorageService.deleteFile(trainer.getProfileImageUrl());
        }

        trainerRepository.delete(trainer);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = {"gym-detail", "main-page-gyms"}, allEntries = true)
    })
    public void updateGymSubscriptions(Long gymId, UpdateGymSubscriptionRequest request) {
        gymAdminPanelRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));

        List<SubscriptionType> types = subscriptionTypeRepository
                .findAllById(request.subscriptionTypeIds());
        if (types.size() != request.subscriptionTypeIds().size()) {
            throw new ResourceNotFoundException("INVALID_SUBSCRIPTION_TYPE", "error.invalid_subscription_type");
        }

        gymSubscriptionRepository.deleteAllByGymId(gymId);
        List<AdminPanelGymSubscription> newSubs = request.subscriptionTypeIds().stream()
                .map(typeId -> AdminPanelGymSubscription.builder()
                        .id(gymId)
                        .subscriptionTypeId(typeId)
                        .isAvailable(true)
                        .build())
                .toList();
        gymSubscriptionRepository.saveAll(newSubs);
    }

    @Transactional
    @CacheEvict(cacheNames = "service-types", allEntries = true)
    public ServiceTypeDto createServiceType(CreateServiceTypeRequest request) {
        if (serviceTypeRepository.existsByNameIgnoreCase(request.name())) {
            throw new ConflictException("SERVICE_TYPE_EXISTS", "error.service_type_exists");
        }
        ServiceType saved = serviceTypeRepository.save(
                ServiceType.builder().name(request.name()).build()
        );
        return new ServiceTypeDto(saved.getId(), saved.getName());
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = {"gym-detail", "main-page-gyms"}, allEntries = true)
    })
    public void updateGymServices(Long gymId, UpdateGymServiceRequest request) {
        gymAdminPanelRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));

        List<ServiceType> types = serviceTypeRepository
                .findAllById(request.serviceTypeIds());
        if (types.size() != request.serviceTypeIds().size()) {
            throw new ResourceNotFoundException("INVALID_SERVICE_TYPE", "error.invalid_service_type");
        }

        gymServiceItemRepository.deleteAllByGymId(gymId);
        List<GymServiceItem> newServices = request.serviceTypeIds().stream()
                .map(typeId -> GymServiceItem.builder()
                        .id(gymId)
                        .serviceTypeId(typeId)
                        .isAvailable(true)
                        .build())
                .toList();
        gymServiceItemRepository.saveAll(newServices);
    }

    @Transactional
    public GymAdminListDto createAdmin(Long gymId, CreateGymAdminRequest request) {
        gymAdminPanelRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));

        if (adminRepository.existsByEmail(request.email())) {
            throw new ConflictException("EMAIL_EXISTS", "error.email_exists");
        }
        if (adminRepository.existsByPhoneNumber(request.phoneNumber())) {
            throw new ConflictException("PHONE_EXISTS", "error.phone_exists");
        }

        AdminPanelGymAdmin admin = AdminPanelGymAdmin.builder()
                .gym(gymAdminPanelRepository.getReferenceById(gymId))
                .firstName(request.firstName())
                .lastName(request.lastName())
                .phoneNumber(request.phoneNumber())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(request.role())
                .status(GymAdminStatus.ACTIVE)
                .build();

        return adminPanelGymMapper.toAdminListDto(adminRepository.save(admin));
    }

    @Transactional
    public void updateAdmin(Long gymId, Long adminId, UpdateGymAdminRequest request) {
        AdminPanelGymAdmin admin = adminRepository.findByIdAndGymId(adminId, gymId)
                .orElseThrow(() -> new ResourceNotFoundException("ADMIN_NOT_FOUND", "error.admin_not_found"));

        if (!admin.getEmail().equals(request.email()) && adminRepository.existsByEmail(request.email())) {
            throw new ConflictException("EMAIL_EXISTS", "error.email_exists");
        }
        if (!admin.getPhoneNumber().equals(request.phoneNumber()) && adminRepository.existsByPhoneNumber(request.phoneNumber())) {
            throw new ConflictException("PHONE_EXISTS", "error.phone_exists");
        }

        adminPanelGymMapper.updateGymAdmin(admin, request);

        adminRepository.save(admin);
    }

    @Transactional
    public void deleteAdmin(Long gymId, Long adminId) {
        AdminPanelGymAdmin admin = adminRepository.findByIdAndGymId(adminId, gymId)
                .orElseThrow(() -> new ResourceNotFoundException("ADMIN_NOT_FOUND", "error.admin_not_found"));

        long adminCount = adminRepository.countByGymId(gymId);
        if (adminCount <= 1) {
            throw new ConflictException("LAST_ADMIN", "error.last_admin_cannot_be_deleted");
        }

        isSoleSuperAdmin(gymId, admin);

        adminRepository.delete(admin);
    }

    @Transactional
    public void resetPassword(Long gymId, Long adminId, ResetPasswordRequest request) {
        AdminPanelGymAdmin admin = adminRepository.findByIdAndGymId(adminId, gymId)
                .orElseThrow(() -> new ResourceNotFoundException("ADMIN_NOT_FOUND", "error.admin_not_found"));

        admin.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        adminRepository.save(admin);
    }

    @Transactional
    public void approveRating(Long gymId, Long ratingId, ModerationRequest request) {
        GymRating rating = findPendingRating(gymId, ratingId);

        adminPanelGymMapper.moderateRating(rating, RatingStatus.APPROVED, request);

        gymRatingRepository.save(rating);
    }

    @Transactional
    public void rejectRating(Long gymId, Long ratingId, ModerationRequest request) {
        GymRating rating = findPendingRating(gymId, ratingId);

        adminPanelGymMapper.moderateRating(rating, RatingStatus.REJECTED, request);

        gymRatingRepository.save(rating);
    }

    @Transactional
    public void deleteRating(Long gymId, Long ratingId) {
        GymRating rating = gymRatingRepository.findByIdAndGymId(ratingId, gymId)
                .orElseThrow(() -> new ResourceNotFoundException("RATING_NOT_FOUND", "error.rating_not_found"));

        gymRatingRepository.delete(rating);
    }

    private GymRating findPendingRating(Long gymId, Long ratingId) {
        GymRating rating = gymRatingRepository.findByIdAndGymId(ratingId, gymId)
                .orElseThrow(() -> new ResourceNotFoundException("RATING_NOT_FOUND", "error.rating_not_found"));
        if (rating.getStatus() != RatingStatus.PENDING) {
            throw new ConflictException("RATING_NOT_PENDING", "error.rating_not_pending");
        }
        return rating;
    }

    private void isSoleSuperAdmin(Long gymId, AdminPanelGymAdmin admin) {
        boolean isSoleSuperAdmin = admin.getRole() == GymAdminRole.SUPER_ADMIN
                && !adminRepository.existsByGymIdAndRole(gymId, GymAdminRole.SUPER_ADMIN);
        if (isSoleSuperAdmin) {
            throw new ConflictException("SOLE_SUPER_ADMIN", "error.sole_super_admin_cannot_be_deleted");
        }
    }

    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("FILE_REQUIRED", "error.file_required");
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new BadRequestException("INVALID_FILE_TYPE", "error.invalid_file_type");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BadRequestException("FILE_TOO_LARGE", "error.file_too_large");
        }
    }

    private void validateWorkingHourRequest(WorkingHourRequest request) {
        if (!request.isClosed()) {
            if (!StringUtils.hasText(request.openTime()) || !StringUtils.hasText(request.closeTime())) {
                throw new BadRequestException("TIME_REQUIRED", "error.open_close_time_required");
            }
            LocalTime open = parseTime(request.openTime());
            LocalTime close = parseTime(request.closeTime());
            if (!open.isBefore(close)) {
                throw new BadRequestException("INVALID_TIME_RANGE", "error.open_time_must_be_before_close");
            }
        }
    }

    private LocalTime parseTime(String time) {
        if (!StringUtils.hasText(time)) return null;
        try {
            return LocalTime.parse(time);
        } catch (Exception e) {
            throw new BadRequestException("INVALID_TIME_FORMAT", "error.invalid_time_format");
        }
    }

    private String uploadImageIfPresent(MultipartFile file) {
        if (file == null || file.isEmpty()) return null;
        validateImageFile(file);
        String fsId = fileStorageService.saveFile(file, "/trainers");
        return "/api/v1/media/stream/" + fsId;
    }

}
