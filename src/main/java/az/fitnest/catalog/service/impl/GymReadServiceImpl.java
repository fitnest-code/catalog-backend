package az.fitnest.catalog.service.impl;

import az.fitnest.catalog.model.entity.Category;
import az.fitnest.catalog.model.entity.RoomImage;

import az.fitnest.catalog.dto.*;
import az.fitnest.catalog.dto.request.*;
import az.fitnest.catalog.dto.response.*;
import az.fitnest.catalog.dto.response.CategoryResponse;
import az.fitnest.catalog.dto.response.ProfessionResponse;
import az.fitnest.catalog.mapper.GymMapper;
import az.fitnest.catalog.exception.ResourceNotFoundException;
import az.fitnest.catalog.exception.BadRequestException;
import az.fitnest.catalog.exception.ForbiddenException;
import az.fitnest.catalog.exception.UnauthorizedException;
import az.fitnest.catalog.model.entity.Address;
import az.fitnest.catalog.model.entity.Gym;
import az.fitnest.catalog.model.entity.SavedGym;
import az.fitnest.catalog.model.entity.GymEntranceHistory;
import az.fitnest.catalog.repository.GymEntranceHistoryRepository;
import az.fitnest.catalog.repository.GymImageRepository;
import az.fitnest.catalog.repository.GymRepository;
import az.fitnest.catalog.repository.ReviewRepository;
import az.fitnest.catalog.repository.TrainerReservationDateRepository;
import az.fitnest.catalog.repository.TrainerRepository;
import az.fitnest.catalog.repository.CategoryRepository;
import az.fitnest.catalog.repository.ReservationRepository;
import az.fitnest.catalog.repository.TranslationRepository;
import az.fitnest.catalog.service.TranslationService;
import az.fitnest.catalog.util.PlatformUtil;
import az.fitnest.catalog.util.UserContext;
import az.fitnest.catalog.client.OrderServiceGrpcClient;
import az.fitnest.catalog.client.UserServiceGrpcClient;
import az.fitnest.catalog.model.entity.Reservation;
import az.fitnest.catalog.model.enums.ReservationStatus;
import az.fitnest.catalog.client.CachedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GymReadServiceImpl implements az.fitnest.catalog.service.GymReadService {
    private final GymRepository gymRepository;
    private final az.fitnest.catalog.repository.SavedGymRepository savedGymRepository;
    private final GymImageRepository gymImageRepository;
    private final TrainerRepository trainerRepository;
    private final ReviewRepository reviewRepository;
    private final TrainerReservationDateRepository trainerReservationDateRepository;
    private final ReservationRepository reservationRepository;
    private final OrderServiceGrpcClient orderServiceGrpcClient;
    private final UserServiceGrpcClient userServiceGrpcClient;
    private final org.springframework.context.MessageSource messageSource;
    private final CategoryRepository categoryRepository;
    private final TranslationService translationService;
    private final TranslationRepository translationRepository;
    private final GymEntranceHistoryRepository gymEntranceHistoryRepository;
    private final az.fitnest.catalog.repository.SupportedServiceRepository supportedServiceRepository;
    private final az.fitnest.catalog.repository.GymAdminRepository gymAdminRepository;
    private final az.fitnest.catalog.repository.GymLessonTypeRepository gymLessonTypeRepository;
    private final az.fitnest.catalog.service.GymQrCodeService gymQrCodeService;
    private final az.fitnest.catalog.client.StorageGrpcClient storageGrpcClient;
    private final java.util.concurrent.Executor taskExecutor;

    @org.springframework.beans.factory.annotation.Autowired
    @org.springframework.context.annotation.Lazy
    private GymReadServiceImpl self;

    private final java.util.Map<Long, List<Long>> eligibleSubscriptionIdsCache = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Map<Long, az.fitnest.order.grpc.PackageNameInfo> packageInfoCache = new java.util.concurrent.ConcurrentHashMap<>();

    private String resolveUserLanguage() {
        Long userId = UserContext.getCurrentUserId();
        return getUserLanguage(userId);
    }

    public List<az.fitnest.catalog.dto.response.SupportedServiceResponse> getAllSupportedServices(Long gymId) {
        List<az.fitnest.catalog.model.entity.SupportedService> services = gymId == null
                ? supportedServiceRepository.findAllByGymIdIsNull()
                : supportedServiceRepository.findAllByGymId(gymId);
        String userLanguage = resolveUserLanguage();
        return services.stream()
                .map(s -> {
                    String localizedName = translationService.getTranslatedValue(
                            "SUPPORTEDSERVICE", s.getId().toString(), "name", userLanguage);
                    String name = (localizedName != null && !localizedName.isEmpty()) ? localizedName : s.getName();
                    return new az.fitnest.catalog.dto.response.SupportedServiceResponse(s.getId(), name, s.getGymId(), s.getIconUrl());
                })
                .toList();
    }

    public String getUserLanguage(Long userId) {
        return az.fitnest.catalog.util.UserContext.getUserLanguage();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "gym-detail", key = "#userId + '_' + #gymId + '_' + T(az.fitnest.catalog.util.UserContext).getUserLanguage()")
    public GymDetailResponse getGymDetail(Long userId, Long gymId) {
        Gym gym = gymRepository.findWithDetailsById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));

        CompletableFuture<Boolean> isSavedFuture = CompletableFuture.supplyAsync(() -> {
            if (userId != null) {
                return savedGymRepository.findByUserIdAndGymId(userId, gymId).isPresent();
            }
            return false;
        }, taskExecutor);

        CompletableFuture<List<GymTrainerResponse>> trainerDtosFuture = CompletableFuture
                .supplyAsync(() -> trainerRepository.findByGymId(gymId, PageRequest.of(0, 5, Sort.by("id")))
                        .getContent().stream()
                        .map(GymMapper::toTrainerDto)
                        .toList(), taskExecutor);

        CompletableFuture<List<GymReviewResponse>> recentReviewsFuture = CompletableFuture
                .supplyAsync(() -> {
                    List<az.fitnest.catalog.model.entity.Review> reviews = reviewRepository
                            .findByGymId(gymId, PageRequest.of(0, 3, Sort.by(Sort.Direction.DESC, "createdDate")))
                            .getContent();

                    return reviews.stream()
                            .map(r -> CompletableFuture.supplyAsync(() -> {
                                String fullName = "User " + r.getUserId();
                                String avatarUrl = null;
                                try {
                                    if (r.getUserId() != null) {
                                        CachedUser user = userServiceGrpcClient.getUserById(r.getUserId());
                                        if (user != null) {
                                            fullName = user.getFirstName() + " " + user.getLastName();
                                            avatarUrl = user.getProfileImageUrl();
                                        }
                                    }
                                } catch (Exception e) {
                                }
                                String originalStatus = r.getStatus() != null ? r.getStatus().name() : null;
                                String userLanguage = resolveUserLanguage();
                                String translatedStatus = (originalStatus != null) ? translationService.getTranslatedValue("REVIEW_STATUS", originalStatus, "name", userLanguage) : null;
                                if (translatedStatus == null) {
                                    translatedStatus = originalStatus;
                                }
                                return GymMapper.toReviewDto(r, fullName, avatarUrl, translatedStatus);
                            }, taskExecutor))
                            .toList();
                }, taskExecutor)
                .thenCompose(futures -> CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                        .thenApply(v -> futures.stream().map(CompletableFuture::join).toList()));

        CompletableFuture<List<GymWorkHourResponse>> generalWorkHoursFuture = CompletableFuture.supplyAsync(() -> {
            String userLang = getUserLanguage(userId);
            return GymMapper.toGroupedWorkHourDtos(gymRepository.findGeneralWorkHoursByGymId(gymId), userLang);
        }, taskExecutor);

        CompletableFuture<List<GymRoomResponse>> roomsFuture = CompletableFuture.supplyAsync(() -> {
            if (gym.getRooms() == null) return new ArrayList<>();
            String userLang = getUserLanguage(userId);
            return gym.getRooms().stream().map(room -> {
                String localizedRoomName = translationService.getTranslatedValue("ROOM", room.getId().toString(), "name", userLang);
                if (localizedRoomName == null || localizedRoomName.isEmpty()) localizedRoomName = room.getName();

                List<String> imageUrls = room.getImages() != null
                        ? room.getImages().stream().map(RoomImage::getPictureUrl).toList()
                        : java.util.Collections.emptyList();

                return GymRoomResponse.builder()
                        .id(room.getId())
                        .room_name(localizedRoomName)
                        .urls(imageUrls)
                        .build();
            }).toList();
        }, taskExecutor);

        CompletableFuture.allOf(isSavedFuture, trainerDtosFuture, recentReviewsFuture, generalWorkHoursFuture, roomsFuture).join();

        String userLanguage = getUserLanguage(userId);
        boolean isSaved = isSavedFuture.join();
        List<GymReviewResponse> recentReviews = recentReviewsFuture.join();
        List<GymWorkHourResponse> generalWorkHours = generalWorkHoursFuture.join();
        List<GymRoomResponse> rooms = roomsFuture.join();

        CategoryResponse categoryDto = null;
        if (gym.getCategory() != null) {
            Category c = gym.getCategory();
            String localizedCatName = translationService.getTranslatedValue("CATEGORY", c.getId().toString(), "name", userLanguage);
            categoryDto = CategoryResponse.builder()
                    .id(c.getId())
                    .name(localizedCatName != null && !localizedCatName.isEmpty() ? localizedCatName : c.getName())
                    .photoUrl(c.getPhotoUrl())
                    .iconUrl(c.getIconUrl())
                    .coverImageUrl(c.getPhotoUrl())
                    .build();
        }

        List<GymTrainerResponse> trainerDtos = trainerDtosFuture.join().stream().<GymTrainerResponse>map(t -> {
            String localizedTrainerName = translationService.getTranslatedValue("Trainer", t.trainer_id(), "name", userLanguage);
            if (localizedTrainerName == null || localizedTrainerName.isEmpty()) {
                localizedTrainerName = t.name();
            }
            String localizedTrainerSurname = translationService.getTranslatedValue("Trainer", t.trainer_id(), "surname", userLanguage);
            if (localizedTrainerSurname == null || localizedTrainerSurname.isEmpty()) {
                localizedTrainerSurname = t.surname();
            }

            ProfessionResponse profDto = t.profession();
            if (t.profession() != null && t.profession().id() != null) {
                String localizedProfession = translationService.getTranslatedValue("PROFESSION",
                        t.profession().id().toString(), "name", userLanguage);
                if (localizedProfession != null && !localizedProfession.isEmpty()) {
                    profDto = ProfessionResponse.builder()
                            .id(t.profession().id())
                            .name(localizedProfession)
                            .build();
                }
            }

            return GymTrainerResponse.builder()
                    .trainer_id(t.trainer_id())
                    .name(localizedTrainerName)
                    .surname(localizedTrainerSurname)
                    .profession(profDto)
                    .picture(t.picture())
                    .phone(t.phone())
                    .email(t.email())
                    .build();
        }).collect(Collectors.toList());

        List<GymWorkHourResponse> workHoursWoman = null;
        if (gym.getWorkHoursWoman() != null && !gym.getWorkHoursWoman().isEmpty()) {
            workHoursWoman = GymMapper.toGroupedWorkHourDtos(gym.getWorkHoursWoman(), userLanguage);
        }
        List<GymWorkHourResponse> workHoursMan = null;
        if (gym.getWorkHoursMan() != null && !gym.getWorkHoursMan().isEmpty()) {
            workHoursMan = GymMapper.toGroupedWorkHourDtos(gym.getWorkHoursMan(), userLanguage);
        }
        if (generalWorkHours != null && generalWorkHours.isEmpty())
            generalWorkHours = null;

        List<GymPlanItemResponse> supportedSubscriptions = new java.util.ArrayList<>();
        try {
            if (gym.getSubscriptions() != null && !gym.getSubscriptions().isEmpty()) {
                List<Long> packageIds = gym.getSubscriptions().stream()
                        .map(sub -> sub.getPackageId())
                        .filter(java.util.Objects::nonNull)
                        .toList();
                List<az.fitnest.order.grpc.PackageNameInfo> packageInfos = orderServiceGrpcClient
                        .getPackageNamesByIds(packageIds);
                java.util.Map<Long, az.fitnest.order.grpc.PackageNameInfo> idToInfo = packageInfos.stream()
                        .collect(java.util.stream.Collectors.toMap(
                                az.fitnest.order.grpc.PackageNameInfo::getPackageId,
                                p -> p));
                supportedSubscriptions = gym.getSubscriptions().stream()
                        .filter(sub -> sub.getPackageId() != null)
                        .filter(sub -> idToInfo.get(sub.getPackageId()) != null)
                        .map(sub -> {
                            az.fitnest.order.grpc.PackageNameInfo info = idToInfo.get(sub.getPackageId());
                            String planId = sub.getPackageId().toString();
                            String localizedPackageName = translationService.getTranslatedValue("GYMSUBSCRIPTION",
                                    planId, "name", userLanguage);
                            String packageName = cleanPackageName((localizedPackageName != null && !localizedPackageName.isEmpty())
                                    ? localizedPackageName
                                    : info.getName());
                            List<GymPlanBenefitResponse> benefitsList = sub.getSupportedServices().stream()
                                    .map(b -> {
                                        String localizedBenefit = translationService.getTranslatedValue(
                                                "SUPPORTEDSERVICE", b.getId().toString(), "name", userLanguage);
                                        return GymPlanBenefitResponse.builder()
                                                .description(localizedBenefit != null && !localizedBenefit.isEmpty()
                                                        ? localizedBenefit
                                                        : b.getName())
                                                .iconImageUrl(b.getIconUrl())
                                                .build();
                                    })
                                    .toList();
                            return GymPlanItemResponse.builder()
                                    .plan_id(planId)
                                    .packageName(packageName)
                                    .dailyPrice(sub.getDailyPrice())
                                    .benefits(benefitsList)
                                    .build();
                        })
                        .collect(java.util.stream.Collectors.toList());
            }
        } catch (Exception e) {
            System.err.println("Could not fetch supported subscriptions from order-backend: " + e.getMessage());
        }
        String localizedName = getLocalizedGymName(gym, userLanguage);
        String localizedDescription = translationService.getTranslatedValue("GYM", gym.getId().toString(),
                "description", userLanguage);
        if (localizedDescription == null || localizedDescription.isEmpty())
            localizedDescription = gym.getDescription();
        List<RestDayRequest> restDays = gym.getRestDays() != null
                ? gym.getRestDays().stream()
                        .map(rd -> new RestDayRequest(rd.name()))
                        .collect(java.util.stream.Collectors.toList())
                : java.util.Collections.emptyList();

        GymDetailResponse response = new GymDetailResponse(
                gym.getId().toString(),
                localizedName,
                localizedDescription,
                gym.getAddress() != null ? new az.fitnest.catalog.dto.response.LocationResponse(
                        gym.getAddress().getLatitude(),
                        gym.getAddress().getLongitude(),
                        getLocalizedAddressField(gym.getId(), "GYM", gym.getAddress(), "addressText",
                                userLanguage),
                        getLocalizedAddressField(gym.getId(), "GYM", gym.getAddress(), "city", userLanguage)
                ) : null,
                isSaved,
                gym.getPhone(),
                gym.getEmail(),
                generalWorkHours,
                workHoursWoman,
                workHoursMan,
                rooms,
                trainerDtos,
                recentReviews,
                categoryDto,
                gym.getCoverImageUrl(),
                gym.getRating() != null ? gym.getRating() : 0.0,
                gym.getReviewsCount() != null ? gym.getReviewsCount() : 0,
                gym.getQrCodeUrl(),
                gym.getStatus(),
                supportedSubscriptions,
                restDays
        );
        return response;
    }

    @Transactional(readOnly = true)
    @Cacheable("gym-images")
    public GymImageResponse getGymImages(Long gymId) {
        Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));

        List<GymImageItemResponse> items = gymImageRepository.findByGymId(gymId).stream()
                .map(GymMapper::toImageItemDto)
                .collect(Collectors.toList());

        if (gym.getRooms() != null) {
            gym.getRooms().forEach(room -> {
                room.getImages().forEach(img -> {
                    items.add(GymImageItemResponse.builder()
                            .image_id(img.getId() != null ? img.getId().toString() : "room_" + room.getName())
                            .url(img.getPictureUrl())
                            .type("room")
                            .title(room.getName())
                            .build());
                });
            });
        }

        return GymImageResponse.builder().items(items).build();
    }

    @Transactional(readOnly = true)
    public boolean isReservationEnabled(Long gymId) {
        return gymRepository.findById(gymId)
                .map(gym -> Boolean.TRUE.equals(gym.getIsReservationEnabled()))
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getReservationRules(Long gymId) {
        if (!gymRepository.existsById(gymId)) {
            throw new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found");
        }
        return Map.of(
                "cancellation_policy", List.of(
                        "Free cancellation is possible up to 24 hours before the lesson starts",
                        "Cancellation within 24 hours will result in the loss of the session",
                        "In case of no-show, the full amount will be charged"),
                "studio_rules", List.of(
                        "Please arrive at least 10 minutes before the lesson",
                        "Bring your own mat or rent one from the studio",
                        "Wear comfortable sportswear",
                        "Drink water before and after the lesson"),
                "availability", List.of(
                        "Some classes may require confirmation from the studio",
                        "You will receive a confirmation email within 2 hours",
                        "For urgent bookings, please contact the studio directly"));
    }

    @Transactional(readOnly = true)
    public List<GymNearbyResponse> getNearbyGyms(double lat, double lng, double radiusKm) {
        double[] bbox = boundingBox(lat, lng, radiusKm);
        List<Gym> candidates = gymRepository.findByAddressLatitudeBetweenAndAddressLongitudeBetween(bbox[0], bbox[1],
                bbox[2], bbox[3]);
        LocalDateTime newThreshold = LocalDateTime.now().minusDays(30L);
        Long currentUserId = az.fitnest.catalog.util.UserContext.getCurrentUserId();
        String userLanguage = getUserLanguage(currentUserId);

        return candidates.stream()
                .filter(gym -> gym.getAddress() != null && gym.getAddress().getLatitude() != null
                        && gym.getAddress().getLongitude() != null)
                .map(gym -> {
                    String localizedName = getLocalizedGymName(gym, userLanguage);
                    double rawDistance = calculateDistanceRaw(lat, lng, gym.getAddress().getLatitude(),
                            gym.getAddress().getLongitude());
                    boolean isNew = gym.getCreatedDate() != null && gym.getCreatedDate().isAfter(newThreshold);
                    return new Object() {
                        GymNearbyResponse dto = GymNearbyResponse.builder()
                                .gymId(gym.getId())
                                .name(localizedName)
                                .address(gym.getAddress() != null ? gym.getAddress().getAddressText() : null)
                                .rating(gym.getRating())
                                .isNew(isNew)
                                .distanceKm(Math.round(rawDistance * 10.0) / 10.0)
                                .build();
                        double distance = rawDistance;
                    };
                })
                .filter(wrapper -> wrapper.distance <= radiusKm)
                .sorted(Comparator.comparingDouble(wrapper -> wrapper.distance))
                .map(wrapper -> wrapper.dto)
                .toList();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "main-page-gyms", key = "{#userId, #page, #pageSize, #userLat, #userLng}")
    public PaginatedResponse<GymMainPageResponse> getClosestGyms(Long userId, int page, int pageSize, Double userLat,
            Double userLng) {
        return getGyms(userId, null, "CLOSEST", null, null, page, pageSize, userLat, userLng, "desc");
    }

    @Transactional(readOnly = true)
    @Cacheable(
        value = "gym-listings",
        key = "{#userId != null && 'SAVED'.equalsIgnoreCase(#type) ? #userId : 0, " +
              "#q != null ? #q : '', " +
              "#type != null ? #type : '', " +
              "#categoryId != null ? #categoryId : 0, " +
              "#subscriptionId != null ? #subscriptionId : 0, " +
              "#page, #pageSize, " +
              "#userLat != null ? T(java.lang.Math).round(#userLat * 1000.0) / 1000.0 : null, " +
              "#userLng != null ? T(java.lang.Math).round(#userLng * 1000.0) / 1000.0 : null, " +
              "#sortDir != null ? #sortDir : '', " +
              "T(az.fitnest.catalog.util.UserContext).getUserLanguage()}"
    )
    public PaginatedResponse<GymMainPageResponse> getGyms(Long userId, String q, String type, Long categoryId,
                                                          Long subscriptionId, int page, int pageSize, Double userLat, Double userLng, String sortDir) {
        if (categoryId != null && !categoryRepository.existsById(categoryId)) {
            throw new BadRequestException("INVALID_CATEGORY", "error.invalid_category");
        }

        Page<Gym> gymPage = null;
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = pageable(page, pageSize, Sort.by(direction, "createdDate"));
        String userLanguage = getUserLanguage(userId);

        if ("SAVED".equalsIgnoreCase(type)) {
            if (userId == null)
                return emptyPaginatedResponse(page, pageSize);
            List<SavedGym> saved = savedGymRepository.findByUserIdWithGym(userId);
            List<Gym> candidates = saved.stream().map(SavedGym::getGym).toList();
            return manualPaginate(candidates, userId, userLat, userLng, page, pageSize, q, categoryId, userLanguage);
        }

        String searchKey = (q != null && !q.isBlank()) ? q.trim() : null;

        boolean hasSubscriptionFilter = (subscriptionId != null);
        List<Long> subscriptionIds = getEligibleSubscriptionIds(subscriptionId);
        if (subscriptionIds.isEmpty()) {
            subscriptionIds = java.util.Collections.singletonList(-1L);
        }

        if (userLat != null && userLng != null && ("CLOSEST".equalsIgnoreCase(type) || type == null || type.isEmpty() || "ALL".equalsIgnoreCase(type))) {
            Pageable distancePageable = PageRequest.of(Math.max(0, page - 1), pageSize);
            Page<Long> idPage = gymRepository.findAllClosestWithFiltersNativeIds(searchKey, categoryId, hasSubscriptionFilter, subscriptionIds, userLat, userLng, distancePageable);
            
            if (idPage.isEmpty()) {
                gymPage = Page.empty(distancePageable);
            } else {
                List<Long> ids = idPage.getContent();
                List<Gym> gyms = gymRepository.findWithListDetailsByIdIn(ids);
                Map<Long, Gym> gymMap = gyms.stream().collect(Collectors.toMap(Gym::getId, g -> g));
                List<Gym> sortedGyms = ids.stream().map(gymMap::get).filter(java.util.Objects::nonNull).toList();
                gymPage = new org.springframework.data.domain.PageImpl<>(sortedGyms, distancePageable, idPage.getTotalElements());
            }
        }

        else {
            Page<Long> idPage = gymRepository.findGymIdsWithFilters(searchKey, categoryId, hasSubscriptionFilter, subscriptionIds, pageable);
            if (idPage.isEmpty()) {
                gymPage = Page.empty(pageable);
            } else {
                List<Long> ids = idPage.getContent();
                List<Gym> gyms = gymRepository.findWithListDetailsByIdIn(ids);
                Map<Long, Gym> gymMap = gyms.stream().collect(Collectors.toMap(Gym::getId, g -> g));
                List<Gym> sortedGyms = ids.stream().map(gymMap::get).filter(java.util.Objects::nonNull).toList();
                gymPage = new org.springframework.data.domain.PageImpl<>(sortedGyms, pageable, idPage.getTotalElements());
            }
        }

        if (gymPage == null) {
            gymPage = Page.empty(pageable);
        }

        Map<Long, List<az.fitnest.catalog.model.entity.GymWorkHour>> globalWorkHoursMap = gymPage.getContent().stream()
                .collect(Collectors.toMap(
                        Gym::getId,
                        gym -> gym.getGeneralWorkHours() != null ? new java.util.ArrayList<>(gym.getGeneralWorkHours()) : java.util.Collections.emptyList(),
                        (existing, replacement) -> existing
                ));

        List<Long> savedGymIds = new java.util.ArrayList<>();
        if (userId != null) {
            List<Long> gymIds = gymPage.getContent().stream().map(Gym::getId).toList();
            if (!gymIds.isEmpty()) {
                savedGymIds = savedGymRepository.findGymIdsByUserIdAndGymIdIn(userId, gymIds);
            }
        }

        final List<Long> finalSavedIds = savedGymIds;

        List<Long> allPackageIds = gymPage.getContent().stream()
                .flatMap(g -> g.getSubscriptions() != null ? g.getSubscriptions().stream() : java.util.stream.Stream.empty())
                .map(az.fitnest.catalog.model.entity.GymSubscription::getPackageId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();

        List<Long> uncachedPackageIds = allPackageIds.stream()
                .filter(id -> !packageInfoCache.containsKey(id))
                .toList();
        if (!uncachedPackageIds.isEmpty()) {
            try {
                if (packageInfoCache.size() >= 10000) {
                    packageInfoCache.clear();
                }
                List<az.fitnest.order.grpc.PackageNameInfo> newInfos = orderServiceGrpcClient.getPackageNamesByIds(uncachedPackageIds);
                for (var info : newInfos) {
                    packageInfoCache.put(info.getPackageId(), info);
                }
            } catch (Exception e) {
            }
        }

        Map<Long, az.fitnest.order.grpc.PackageNameInfo> finalPackageMap = allPackageIds.stream()
                .filter(packageInfoCache::containsKey)
                .collect(Collectors.toMap(id -> id, packageInfoCache::get));
        final Map<Long, List<az.fitnest.catalog.model.entity.GymWorkHour>> finalWorkHoursMap = globalWorkHoursMap;

        Map<String, String> translationLookup = fetchTranslationsInBulk(gymPage.getContent(), userLanguage);

        List<GymMainPageResponse> items = gymPage.getContent().stream()
                .map(gym -> mapToGymMainPageDto(
                        gym,
                        userId,
                        userLat,
                        userLng,
                        finalSavedIds.contains(gym.getId()),
                        finalPackageMap,
                        finalWorkHoursMap,
                        translationLookup,
                        userLanguage
                ))
                .collect(Collectors.toList());

        String message = null;
        if (items.isEmpty()) {
            message = messageSource.getMessage("error.gym_not_found", null,
                    org.springframework.context.i18n.LocaleContextHolder.getLocale());
        }

        return PaginatedResponse.<GymMainPageResponse>builder()
                .items(items)
                .total(gymPage.getTotalElements())
                .page(page)
                .pageSize(pageSize)
                .message(message)
                .build();
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<AdminGymResponse> getAllGymsAdmin(String query, String sort, int page, int pageSize) {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        Long userId = UserContext.getCurrentUserId();

        Sort springSort = Sort.unsorted();
        if (sort != null) {
            switch (sort) {
                case "name_asc":
                    springSort = Sort.by(Sort.Direction.ASC, "name");
                    break;
                case "name_desc":
                    springSort = Sort.by(Sort.Direction.DESC, "name");
                    break;
                case "address_asc":
                    springSort = Sort.by(Sort.Direction.ASC, "address.city", "address.addressText");
                    break;
                case "newest":
                    springSort = Sort.by(Sort.Direction.DESC, "createdDate");
                    break;
                case "deactivated":
                    springSort = Sort.by(Sort.Direction.DESC, "status");
                    break;
                default:
                    springSort = Sort.by(Sort.Direction.DESC, "createdDate");
            }
        } else {
            springSort = Sort.by(Sort.Direction.DESC, "createdDate");
        }

        Pageable pageable = PageRequest.of(Math.max(0, page - 1), pageSize, springSort);

        Specification<Gym> spec = (root, criteriaQuery, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (query != null && !query.isBlank()) {
                String pattern = "%" + query.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(root.get("address").get("addressText")), pattern),
                        cb.like(cb.lower(root.get("address").get("city")), pattern)));
            }

            if (!isAdmin && userId != null) {
                jakarta.persistence.criteria.Subquery<Long> subquery = criteriaQuery.subquery(Long.class);
                jakarta.persistence.criteria.Root<az.fitnest.catalog.model.entity.GymAdmin> subRoot = subquery.from(az.fitnest.catalog.model.entity.GymAdmin.class);
                subquery.select(subRoot.get("gym").get("id"))
                        .where(cb.equal(subRoot.get("userId"), userId));
                predicates.add(root.get("id").in(subquery));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Gym> gymPage = gymRepository.findAll(spec, pageable);

        List<Long> gymIds = gymPage.getContent().stream().map(Gym::getId).toList();
        Map<Long, String> ownerNames = gymAdminRepository.findAllByGymIdIn(gymIds).stream()
                .filter(admin -> "Super admin".equalsIgnoreCase(admin.getRole()))
                .collect(Collectors.groupingBy(admin -> admin.getGym().getId(),
                        Collectors.mapping(admin -> admin.getName() + " " + admin.getSurname(),
                                Collectors.joining(", "))));

        String userLanguage = resolveUserLanguage();
        List<AdminGymResponse> items = gymPage.getContent().stream().map(gym -> {
        String ownerName = ownerNames.get(gym.getId());

            String city = null;
            String addressText = null;
            if (gym.getAddress() != null) {
                city = getLocalizedAddressField(gym.getId(), "GYM", gym.getAddress(), "city", userLanguage);
                addressText = getLocalizedAddressField(gym.getId(), "GYM", gym.getAddress(), "addressText", userLanguage);
            }
            String fullAddress = (city != null || addressText != null)
                    ? ((city != null ? city : "") + ", " + (addressText != null ? addressText : ""))
                    : "N/A";

            return AdminGymResponse.builder()
                    .id(gym.getId())
                    .name(gym.getName())
                    .fullAddress(fullAddress)
                    .ownerName(ownerName)
                    .status(gym.getStatus())
                    .build();
        }).collect(Collectors.toList());

        return PaginatedResponse.<AdminGymResponse>builder()
                .items(items)
                .total(gymPage.getTotalElements())
                .page(page)
                .pageSize(pageSize)
                .build();
    }

    @Transactional(readOnly = true)
    public List<AdminQrScanHistoryResponse> getUserQrScanHistoryAdmin(Long userId, String query, String sort) {
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

        List<GymEntranceHistory> historyList = gymEntranceHistoryRepository.findAllByUserIdOrderByScanDateDesc(userId);
        if (historyList.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        List<Long> gymIds = historyList.stream().map(GymEntranceHistory::getGymId).distinct().toList();
        Map<Long, String> gymNames = gymRepository.findAllById(gymIds).stream()
                .collect(Collectors.toMap(Gym::getId, Gym::getName));

        java.util.stream.Stream<AdminQrScanHistoryResponse> stream = historyList.stream()
                .map(h -> {
                    String status = h.getStatus();
                    if ("ELIGIBLE".equalsIgnoreCase(status))
                        status = "Uğurlu";
                    else if ("UNSUCCESSFUL".equalsIgnoreCase(status))
                        status = "Uğursuz";

                    return AdminQrScanHistoryResponse.builder()
                            .dateTime(h.getScanDate() != null ? h.getScanDate().format(formatter) : "N/A")
                            .gymName(gymNames.getOrDefault(h.getGymId(), "Unknown Gym"))
                            .status(status)
                            .failedReason(h.getReason())
                            .platform(h.getPlatform() != null ? h.getPlatform() : "N/A")
                            .rawDate(h.getScanDate())
                            .build();
                });

        if (query != null && !query.isBlank()) {
            String lowerQuery = query.toLowerCase();
            stream = stream.filter(res -> res.gymName().toLowerCase().contains(lowerQuery));
        }

        List<AdminQrScanHistoryResponse> result = stream.collect(Collectors.toList());

        if (sort != null) {
            switch (sort) {
                case "gymName_asc":
                    result.sort(
                            Comparator.comparing(AdminQrScanHistoryResponse::gymName, String.CASE_INSENSITIVE_ORDER));
                    break;
                case "gymName_desc":
                    result.sort(Comparator.comparing(AdminQrScanHistoryResponse::gymName, String.CASE_INSENSITIVE_ORDER)
                            .reversed());
                    break;
                case "date_asc":
                    result.sort(Comparator.comparing(AdminQrScanHistoryResponse::rawDate,
                            Comparator.nullsLast(Comparator.naturalOrder())));
                    break;
                case "date_desc":
                    result.sort(Comparator.comparing(AdminQrScanHistoryResponse::rawDate,
                            Comparator.nullsLast(Comparator.naturalOrder())).reversed());
                    break;
                case "status_asc":
                    result.sort(
                            Comparator.comparing(AdminQrScanHistoryResponse::status, String.CASE_INSENSITIVE_ORDER));
                    break;
                case "status_desc":
                    result.sort(Comparator.comparing(AdminQrScanHistoryResponse::status, String.CASE_INSENSITIVE_ORDER)
                            .reversed());
                    break;
                case "platform_asc":
                    result.sort(
                            Comparator.comparing(AdminQrScanHistoryResponse::platform, String.CASE_INSENSITIVE_ORDER));
                    break;
                case "platform_desc":
                    result.sort(Comparator
                            .comparing(AdminQrScanHistoryResponse::platform, String.CASE_INSENSITIVE_ORDER).reversed());
                    break;
            }
        }

        return result;
    }

    private PaginatedResponse<GymMainPageResponse> manualPaginate(List<Gym> candidates, Long userId, Double lat,
                                                                  Double lng, int page, int pageSize, String q, Long categoryId, String userLanguage) {
        List<Gym> filtered = candidates.stream()
                .filter(g -> {
                    if (q != null && !q.isBlank()) {
                        String lowerQ = q.toLowerCase();
                        boolean nameMatches = g.getName() != null && g.getName().toLowerCase().contains(lowerQ);
                        boolean addressMatches = g.getAddress() != null && g.getAddress().getAddressText() != null
                                && g.getAddress().getAddressText().toLowerCase().contains(lowerQ);
                        if (!nameMatches && !addressMatches) return false;
                    }
                    if (categoryId != null) {
                        if (g.getCategory() == null || !g.getCategory().getId().equals(categoryId)) return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());

        if (lat != null && lng != null) {
            filtered.sort(Comparator.comparingDouble(g -> {
                if (g.getAddress() != null && g.getAddress().getLatitude() != null && g.getAddress().getLongitude() != null) {
                    return calculateDistanceRaw(lat, lng, g.getAddress().getLatitude(), g.getAddress().getLongitude());
                }
                return Double.MAX_VALUE;
            }));
        }

        int total = filtered.size();
        int from = Math.max(0, (page - 1) * pageSize);
        int to = Math.min(total, from + pageSize);
        List<Gym> pageGymsRaw = from >= total ? new java.util.ArrayList<>() : filtered.subList(from, to);
        List<Long> pageGymIds = pageGymsRaw.stream().map(Gym::getId).toList();
        List<Gym> pageGymsDetailed = pageGymIds.isEmpty() ? List.of() : gymRepository.findWithListDetailsByIdIn(pageGymIds);
        Map<Long, Gym> pageGymsDetailedMap = pageGymsDetailed.stream().collect(Collectors.toMap(Gym::getId, g -> g));
        List<Gym> pageGyms = pageGymIds.stream().map(pageGymsDetailedMap::get).filter(java.util.Objects::nonNull).toList();

        Map<Long, List<az.fitnest.catalog.model.entity.GymWorkHour>> manualWorkHoursMap = pageGyms.stream()
                .collect(Collectors.toMap(
                        Gym::getId,
                        gym -> gym.getGeneralWorkHours() != null ? new java.util.ArrayList<>(gym.getGeneralWorkHours()) : java.util.Collections.emptyList(),
                        (existing, replacement) -> existing
                ));

        List<Long> pagePackageIds = pageGyms.stream()
                .flatMap(g -> g.getSubscriptions() != null ? g.getSubscriptions().stream() : java.util.stream.Stream.empty())
                .map(az.fitnest.catalog.model.entity.GymSubscription::getPackageId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();

        List<Long> uncachedPackageIds = pagePackageIds.stream()
                .filter(id -> !packageInfoCache.containsKey(id))
                .toList();
        if (!uncachedPackageIds.isEmpty()) {
            try {
                if (packageInfoCache.size() >= 10000) {
                    packageInfoCache.clear();
                }
                List<az.fitnest.order.grpc.PackageNameInfo> newInfos = orderServiceGrpcClient.getPackageNamesByIds(uncachedPackageIds);
                for (var info : newInfos) {
                    packageInfoCache.put(info.getPackageId(), info);
                }
            } catch (Exception e) {
            }
        }

        Map<Long, az.fitnest.order.grpc.PackageNameInfo> manualPackageMap = pagePackageIds.stream()
                .filter(packageInfoCache::containsKey)
                .collect(Collectors.toMap(id -> id, packageInfoCache::get));

        Map<String, String> translationLookup = fetchTranslationsInBulk(pageGyms, userLanguage);

        List<GymMainPageResponse> pageItems = pageGyms.stream()
                .map(g -> mapToGymMainPageDto(
                        g,
                        userId,
                        lat,
                        lng,
                        true,
                        manualPackageMap,
                        manualWorkHoursMap,
                        translationLookup,
                        userLanguage
                ))
                .collect(Collectors.toList());

        return PaginatedResponse.<GymMainPageResponse>builder()
                .items(pageItems)
                .total(total)
                .page(page)
                .pageSize(pageSize)
                .build();
    }    private GymMainPageResponse mapToGymMainPageDto(
            Gym gym,
            Long userId,
            Double userLat,
            Double userLng,
            boolean isSaved,
            Map<Long, az.fitnest.order.grpc.PackageNameInfo> packageInfoMap,
            Map<Long, List<az.fitnest.catalog.model.entity.GymWorkHour>> workHoursMap,
            Map<String, String> translationLookup,
            String userLanguage) {

        double stars = gym.getRating() != null ? gym.getRating() : 0.0;
        boolean isNew = gym.getCreatedDate() != null
                && gym.getCreatedDate().isAfter(LocalDateTime.now().minusMonths(1L));

        Address address = gym.getAddress();

        Double distanceKm = null;
        if (userLat != null
                && userLng != null
                && address != null
                && address.getLatitude() != null
                && address.getLongitude() != null) {

            distanceKm = Math.round(
                    calculateDistanceRaw(
                            userLat, userLng,
                            address.getLatitude(), address.getLongitude()
                    ) * 10.0
            ) / 10.0;
        }

        CategoryResponse category = null;
        if (gym.getCategory() != null) {
            Category c = gym.getCategory();
            String localizedCatName = getTranslatedValueCached(translationLookup,
                    "CATEGORY", c.getCategoryId().toString(), "name", userLanguage);
            category = CategoryResponse.builder()
                    .id(c.getCategoryId())
                    .name(localizedCatName != null && !localizedCatName.isEmpty()
                            ? localizedCatName : c.getName())
                    .photoUrl(c.getPhotoUrl())
                    .iconUrl(c.getIconUrl())
                    .coverImageUrl(c.getPhotoUrl())
                    .build();
        }

        List<GymPlanItemResponse> supportedSubscriptions = new java.util.ArrayList<>();
        if (gym.getSubscriptions() != null && !gym.getSubscriptions().isEmpty()) {
            supportedSubscriptions = gym.getSubscriptions().stream()
                    .filter(sub -> sub.getPackageId() != null)
                    .map(sub -> {
                        az.fitnest.order.grpc.PackageNameInfo info = packageInfoMap.get(sub.getPackageId());
                        String planId = sub.getPackageId().toString();
                        String localizedPackageName = getTranslatedValueCached(translationLookup,
                                "GYMSUBSCRIPTION", planId, "name", userLanguage);
                        String packageName = cleanPackageName(
                                (localizedPackageName != null && !localizedPackageName.isEmpty())
                                        ? localizedPackageName
                                        : (info != null ? info.getName() : "Bronze")
                        );
                        List<GymPlanBenefitResponse> benefitsList = sub.getSupportedServices().stream()
                                .map(b -> {
                                    String localizedBenefit = getTranslatedValueCached(translationLookup,
                                            "SUPPORTEDSERVICE", b.getId().toString(), "name", userLanguage);
                                    return GymPlanBenefitResponse.builder()
                                            .description(localizedBenefit != null && !localizedBenefit.isEmpty()
                                                    ? localizedBenefit : b.getName())
                                            .iconImageUrl(b.getIconUrl())
                                            .build();
                                })
                                .toList();
                        return GymPlanItemResponse.builder()
                                .plan_id(planId)
                                .packageName(packageName)
                                .dailyPrice(sub.getDailyPrice())
                                .benefits(benefitsList)
                                .build();
                    })
                    .toList();
        }

        String localizedName = getLocalizedGymName(gym, userLanguage);

        List<az.fitnest.catalog.model.entity.GymWorkHour> gymWorkHours = workHoursMap.getOrDefault(gym.getId(), java.util.Collections.emptyList());
        String workHoursText = az.fitnest.catalog.mapper.GymMapper.toWorkHoursText(gymWorkHours, userLanguage);

        return GymMainPageResponse.builder()
                .gymId(gym.getId().toString())
                .name(localizedName)
                .coverImageUrl(gym.getCoverImageUrl())
                .stars(stars)
                .isNew(isNew)
                .location(address != null
                        ? getLocalizedAddressField(gym.getId(), "GYM", address, "addressText", translationLookup, userLanguage)
                        : null)
                .city(address != null
                        ? getLocalizedAddressField(gym.getId(), "GYM", address, "city", translationLookup, userLanguage)
                        : null)
                .distanceKm(distanceKm)
                .isSaved(isSaved)
                .category(category)
                .supportedSubscriptions(supportedSubscriptions)
                .workHoursText(workHoursText) // Sizin tələb etdiyiniz yeni sahə
                .build();
    }

    private PaginatedResponse<GymMainPageResponse> emptyPaginatedResponse(int page, int pageSize) {
        return PaginatedResponse.<GymMainPageResponse>builder().items(java.util.Collections.emptyList()).total(0)
                .page(page).pageSize(pageSize).build();
    }

    @Transactional(readOnly = true)
    public LocationResponse getGymLocation(Long gymId) {
        Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));
        Address addr = gym.getAddress();
        if (addr == null) {
            return LocationResponse.builder().build();
        }
        return LocationResponse.builder()
                .addressText(addr.getAddressText())
                .latitude(addr.getLatitude())
                .longitude(addr.getLongitude())
                .build();
    }

    @Transactional(readOnly = true)
    public boolean gymSupportsPlan(Long gymId, Long planId) {
        Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));
        return orderServiceGrpcClient.checkPackageExists(planId);
    }

    public GymEntranceResponse checkProximity(Double lat, Double lng, Long gymId) {
        Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));
        Address address = gym.getAddress();
        if (address == null || address.getLatitude() == null || address.getLongitude() == null) {
            return GymEntranceResponse.builder()
                    .allowed(false)
                    .build();
        }
        double distance = calculateDistanceRaw(lat, lng, address.getLatitude(), address.getLongitude());
        double allowedRadiusKm = 0.2;
        if (distance > allowedRadiusKm) {
            return GymEntranceResponse.builder()
                    .allowed(false)
                    .build();
        }
        return GymEntranceResponse.builder()
                .allowed(true)
                .build();
    }

    public boolean checkGymEntranceEligibilitySimple(Object principal) {
        Long userId = UserContext.extractUserId(principal);
        if (userId == null) {
            throw new IllegalArgumentException("error.unauthorized");
        }
        az.fitnest.order.grpc.ActiveSubscriptionResponse subResp = null;
        try {
            subResp = orderServiceGrpcClient.getActiveSubscription(userId);
        } catch (Exception e) {
            throw new IllegalStateException("error.subscription_fetch_failed");
        }
        String status = subResp.getSubscriptionStatus();
        if (status == null || status.isEmpty() || status.equalsIgnoreCase("none")) {
            return false;
        }
        if (!status.equalsIgnoreCase("active")) {
            return false;
        }
        int visitLimitRemaining = subResp.getRemainingLimit();
        return visitLimitRemaining > 0;
    }

    @Transactional(readOnly = true)
    public GymCountResponse getGymCount(String type, Long subscriptionId, Long categoryId) {
        java.time.LocalDateTime newThreshold = null;
        if (type != null && type.equalsIgnoreCase("new")) {
            newThreshold = java.time.LocalDateTime.now().minusWeeks(1);
        }
        long count = gymRepository.countGymsWithFilters(categoryId, subscriptionId, newThreshold);
        return new GymCountResponse(count, type != null ? type : "all", subscriptionId, categoryId);
    }

    @Transactional(readOnly = true)
    public GymTypeCountResponse getGymCountByType(String type) {
        long count;
        if (type.equalsIgnoreCase("new")) {
            count = gymRepository.countByCreatedDateAfter(java.time.LocalDateTime.now().minusWeeks(1));
        } else {
            count = gymRepository.count();
        }
        return new GymTypeCountResponse(type, count);
    }

    @Transactional(readOnly = true)
    public List<GymCategoryCountResponse> getGymCountByCategory() {
        Long userId = az.fitnest.catalog.util.UserContext.getCurrentUserId();
        String language = getUserLanguage(userId);
        return self.getGymCountByCategoryCached(language);
    }

    @Cacheable(value = "gym-count-by-category", key = "#language")
    public List<GymCategoryCountResponse> getGymCountByCategoryCached(String language) {
        List<Object[]> results = gymRepository.countGymsByCategory();
        return results.stream()
                .map(row -> {
                    Category cat = (Category) row[0];
                    Long count = (Long) row[1];
                    String catName = "UNKNOWN";
                    String iconUrl = null;
                    if (cat != null) {
                        iconUrl = cat.getIconUrl();
                        String translatedName = translationService.getTranslatedValue("CATEGORY",
                                String.valueOf(cat.getCategoryId()), "name", language);
                        catName = (translatedName != null && !translatedName.isEmpty()) ? translatedName
                                : cat.getName();
                    }
                    return new GymCategoryCountResponse(
                            cat != null ? cat.getCategoryId() : -1L,
                            catName,
                            iconUrl,
                            count);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "gym-count-by-subscription")
    public List<GymSubscriptionCountResponse> getGymCountBySubscription() {
        List<Object[]> results = gymRepository.countGymsBySubscriptionPackageId();
        if (results.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        List<Long> packageIds = results.stream()
                .map(row -> (Long) row[0])
                .filter(java.util.Objects::nonNull)
                .toList();

        List<Long> uncachedPackageIds = packageIds.stream()
                .filter(id -> !packageInfoCache.containsKey(id))
                .toList();
        if (!uncachedPackageIds.isEmpty()) {
            try {
                if (packageInfoCache.size() >= 10000) {
                    packageInfoCache.clear();
                }
                List<az.fitnest.order.grpc.PackageNameInfo> newInfos = orderServiceGrpcClient.getPackageNamesByIds(uncachedPackageIds);
                for (var info : newInfos) {
                    packageInfoCache.put(info.getPackageId(), info);
                }
            } catch (Exception e) {
            }
        }

        return results.stream()
                .map(row -> {
                    Long packageId = (Long) row[0];
                    Long count = (Long) row[1];
                    String packageName = "UNKNOWN";
                    if (packageId != null && packageInfoCache.containsKey(packageId)) {
                        packageName = packageInfoCache.get(packageId).getName();
                    }
                    return new GymSubscriptionCountResponse(
                            packageId,
                            packageName,
                            count
                    );
                })
                .toList();
    }

    private Pageable pageable(int page, int size, Sort sort) {
        int safePage = Math.max(page, 1) - 1;
        int safeSize = Math.max(1, Math.min(size, 100));
        return PageRequest.of(safePage, safeSize, sort);
    }

    private double[] boundingBox(double lat, double lng, double radiusKm) {
        double radiusRatio = radiusKm / 6371.0;
        double minLat = lat - Math.toDegrees(radiusRatio);
        double maxLat = lat + Math.toDegrees(radiusRatio);
        double deltaLng = Math.toDegrees(Math.asin(Math.sin(radiusRatio) / Math.cos(Math.toRadians(lat))));
        return new double[] { minLat, maxLat, lng - deltaLng, lng + deltaLng };
    }

    public double calculateDistanceRaw(double lat1, double lng1, double lat2, double lng2) {
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lng2 - lng1);
        double a = Math.sin(latDistance / 2.0) * Math.sin(latDistance / 2.0) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(lonDistance / 2.0) * Math.sin(lonDistance / 2.0);
        return 6371.0 * 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
    }

    @Transactional
    public String getGymQrUrl(Long gymId) {
        Gym gym = gymRepository.findById(gymId).orElseThrow(() ->
            new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));

        String qrCodeUrl = gym.getQrCodeUrl();
        boolean needsRegenerate = false;

        if (qrCodeUrl == null || qrCodeUrl.trim().isEmpty() || qrCodeUrl.contains("PENDING") || qrCodeUrl.contains("/qr") ||
            gym.getQrCodeValue() == null || !gym.getQrCodeValue().equals(gymId.toString())) {
            needsRegenerate = true;
        } else {
            try {
                String fileId = qrCodeUrl;
                if (qrCodeUrl.contains("/stream/")) {
                    fileId = qrCodeUrl.substring(qrCodeUrl.lastIndexOf("/stream/") + 8);
                } else if (qrCodeUrl.contains("/")) {
                    fileId = qrCodeUrl.substring(qrCodeUrl.lastIndexOf("/") + 1);
                }
                storageGrpcClient.downloadFile(fileId, response -> {});
            } catch (Exception e) {
                needsRegenerate = true;
            }
        }

        if (needsRegenerate) {
            qrCodeUrl = gymQrCodeService.generateAndSaveQrCodeSync(gymId);
        }
        return qrCodeUrl;
    }

    @Transactional
    public GymEntranceScanResponse scanGymQrEntrance(Object principal, String qrCodeValue, Double lat, Double lng,
            String userAgent) {
        String platform = PlatformUtil.detectPlatform(userAgent);
        Long userId = UserContext.extractUserId(principal);
        if (userId == null) {
            throw new IllegalArgumentException("error.unauthorized");
        }
        Long gymId = extractGymIdFromQr(qrCodeValue);
        if (gymId == null) {
            throw new IllegalArgumentException("error.invalid_qr_code");
        }
        Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));

        boolean allowed = true;
        String reason = null;
        String status = "ELIGIBLE";

        Double amount = 0.0;
        az.fitnest.order.grpc.ActiveSubscriptionResponse subResp = null;
        try {
            subResp = orderServiceGrpcClient.getActiveSubscription(userId);
            String subStatus = subResp.getSubscriptionStatus();
            if (subStatus == null || subStatus.isEmpty() || subStatus.equalsIgnoreCase("none")
                    || !subStatus.equalsIgnoreCase("active")) {
                allowed = false;
                reason = "NO_ACTIVE_SUBSCRIPTION";
            } else if (subResp.getRemainingLimit() <= 0) {
                allowed = false;
                reason = "VISIT_LIMIT_EXCEEDED";
            } else {
                long userPackageId = subResp.getPackageId();
                boolean checkHierarchySuccess = false;
                try {
                    List<Long> allPackageIds = new java.util.ArrayList<>();
                    allPackageIds.add(userPackageId);
                    if (gym.getSubscriptions() != null) {
                        for (var sub : gym.getSubscriptions()) {
                            if (sub.getPackageId() != null) {
                                allPackageIds.add(sub.getPackageId());
                            }
                        }
                    }

                    List<az.fitnest.order.grpc.PackageNameInfo> packageNames = orderServiceGrpcClient.getPackageNamesByIds(allPackageIds);
                    java.util.Map<Long, String> packageNamesMap = packageNames.stream()
                            .collect(java.util.stream.Collectors.toMap(
                                    az.fitnest.order.grpc.PackageNameInfo::getPackageId,
                                    az.fitnest.order.grpc.PackageNameInfo::getName,
                                    (a, b) -> a
                            ));

                    String userPackageName = packageNamesMap.get(userPackageId);
                    int userRank = getPackageRank(userPackageName);

                    List<az.fitnest.catalog.model.entity.GymSubscription> eligibleSubscriptions = new java.util.ArrayList<>();
                    if (gym.getSubscriptions() != null) {
                        for (var sub : gym.getSubscriptions()) {
                            if (sub.getPackageId() != null) {
                                String gymPackageName = packageNamesMap.get(sub.getPackageId());
                                if (gymPackageName != null) {
                                    int gymRank = getPackageRank(gymPackageName);
                                    if (userRank >= gymRank) {
                                        eligibleSubscriptions.add(sub);
                                    }
                                }
                            }
                        }
                    }

                    if (eligibleSubscriptions.isEmpty()) {
                        allowed = false;
                        reason = "GYM_NOT_SUPPORTED";
                        checkHierarchySuccess = true;
                    } else {
                        az.fitnest.catalog.model.entity.GymSubscription bestSub = null;
                        int bestRank = -1;
                        for (var sub : eligibleSubscriptions) {
                            String name = packageNamesMap.get(sub.getPackageId());
                            int rank = getPackageRank(name);
                            if (rank > bestRank) {
                                bestRank = rank;
                                bestSub = sub;
                            }
                        }
                        if (bestSub != null) {
                            amount = bestSub.getDailyPrice();
                            checkHierarchySuccess = true;
                        }
                    }
                } catch (Exception e) {
                    // Fallback to exact matching
                }

                if (!checkHierarchySuccess) {
                    var matchedSub = gym.getSubscriptions() != null ? gym.getSubscriptions().stream()
                            .filter(sub -> sub.getPackageId() != null && sub.getPackageId().equals(userPackageId))
                            .findFirst() : java.util.Optional.<az.fitnest.catalog.model.entity.GymSubscription>empty();

                    if (matchedSub.isEmpty()) {
                        allowed = false;
                        reason = "GYM_NOT_SUPPORTED";
                    } else {
                        amount = matchedSub.get().getDailyPrice();
                    }
                }
            }
        } catch (Exception e) {
            allowed = false;
            reason = "NO_ACTIVE_SUBSCRIPTION";
        }

        if (allowed) {
            String gender = null;
            try {
                az.fitnest.catalog.client.CachedUser userResp = userServiceGrpcClient.getUserById(userId);
                gender = userResp.getGender();
            } catch (Exception e) {
            }

            boolean withinHours = isWithinWorkingHours(gym, gender);
            if (!withinHours) {
                allowed = false;
                reason = "OUT_OF_WORKING_HOURS";
            } else {
                Address address = gym.getAddress();
                if (address != null && address.getLatitude() != null && address.getLongitude() != null && lat != null
                        && lng != null) {
                    double distance = calculateDistanceRaw(lat, lng, address.getLatitude(), address.getLongitude());
                    if (distance > 0.2) {
                        allowed = false;
                        reason = "TOO_FAR_FROM_GYM";
                    }
                }
            }
        }

        if (allowed) {
            try {
                // Check if there is an active reservation at this gym today
                List<Reservation> activeReservations = reservationRepository.findActiveReservationsForCheckIn(
                        userId, gymId, java.time.LocalDate.now(), java.time.LocalTime.now(),
                        java.util.List.of(az.fitnest.catalog.model.enums.ReservationStatus.APPROVED, az.fitnest.catalog.model.enums.ReservationStatus.PENDING)
                );

                boolean consumeFrozen = false;
                Reservation targetReservation = null;
                if (!activeReservations.isEmpty()) {
                    consumeFrozen = true;
                    targetReservation = activeReservations.get(0);
                }

                orderServiceGrpcClient.checkIn(userId, gymId, consumeFrozen);

                if (targetReservation != null) {
                    targetReservation.setAttended(true);
                    if (targetReservation.getStatus() == az.fitnest.catalog.model.enums.ReservationStatus.PENDING) {
                        targetReservation.setStatus(az.fitnest.catalog.model.enums.ReservationStatus.APPROVED);
                        targetReservation.setApprovedAt(LocalDateTime.now());
                    }
                    reservationRepository.save(targetReservation);
                }
            } catch (Exception e) {
                allowed = false;
                status = "UNSUCCESSFUL";
                reason = "CHECKIN_FAILED";
            }
        }

        if (!allowed) {
            status = "UNSUCCESSFUL";
        }

        GymEntranceHistory history = GymEntranceHistory.builder()
                .userId(userId)
                .gymId(gymId)
                .scanDate(LocalDateTime.now(java.time.ZoneId.of("Asia/Baku")))
                .status(status)
                .reason(reason)
                .platform(platform)
                .amount(amount)
                .build();
        gymEntranceHistoryRepository.save(history);

        String userLanguage = getUserLanguage(userId);
        String localizedName = getLocalizedGymName(gym, userLanguage);
        Address addr = gym.getAddress();
        String gymAddress = addr != null ? addr.getAddressText() : null;

        return GymEntranceScanResponse.builder()
                .gymName(allowed ? localizedName : null)
                .gymAddress(allowed ? gymAddress : null)
                .enterDate(allowed ? java.time.LocalDate.now(java.time.ZoneId.of("Asia/Baku")).toString() : null)
                .enterHour(allowed
                        ? java.time.LocalTime.now(java.time.ZoneId.of("Asia/Baku")).withSecond(0).withNano(0).toString()
                        : null)
                .isAllowed(allowed)
                .status(status)
                .reason(reason)
                .build();
    }

    @Transactional(readOnly = true)
    public List<GymEntranceHistoryAdminResponse> getGymEntranceHistory(Long gymId) {
        if (!gymRepository.existsById(gymId)) {
            throw new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found");
        }
        List<GymEntranceHistory> historyList = gymEntranceHistoryRepository.findByGymIdOrderByScanDateDesc(gymId);
        return historyList.stream().map(h -> {
            String firstName = "";
            String lastName = "";
            String phone = "";
            String profilePhotoUrl = "";
            try {
                az.fitnest.catalog.client.CachedUser user = userServiceGrpcClient.getUserById(h.getUserId());
                if (user != null) {
                    firstName = user.getFirstName();
                    lastName = user.getLastName();
                    phone = user.getMobile();
                    profilePhotoUrl = user.getProfileImageUrl();
                }
            } catch (Exception e) {
                firstName = "User";
                lastName = String.valueOf(h.getUserId());
            }
            String formattedDate = h.getScanDate()
                    .format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
            String displayStatus = "Uğursuz";
            if ("ELIGIBLE".equalsIgnoreCase(h.getStatus()) || "Uğurlu".equalsIgnoreCase(h.getStatus())) {
                displayStatus = "Uğurlu";
            }
            String userLanguage = resolveUserLanguage();
            String translatedStatus = translationService.getTranslatedValue("ENTRANCE_STATUS", displayStatus, "name", userLanguage);
            if (translatedStatus != null && !translatedStatus.isEmpty()) {
                displayStatus = translatedStatus;
            }
            return GymEntranceHistoryAdminResponse.builder()
                    .id(h.getId())
                    .userId(h.getUserId())
                    .firstName(firstName)
                    .lastName(lastName)
                    .phone(phone)
                    .scanDateTime(formattedDate)
                    .status(displayStatus)
                    .reason(h.getReason())
                    .amount(h.getAmount() != null ? h.getAmount() : 0.0)
                    .profilePhotoUrl(profilePhotoUrl)
                    .build();
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public az.fitnest.catalog.dto.response.GymAnalyticsResponse getGymAnalytics(Long gymId,
            java.time.LocalDateTime startDate, java.time.LocalDateTime endDate, String statusFilter, String sort,
            int page, int pageSize) {
        verifyGymOwnership(gymId);
        if (!gymRepository.existsById(gymId)) {
            throw new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found");
        }
        List<GymEntranceHistory> historyList;
        if (startDate != null && endDate != null) {
            historyList = gymEntranceHistoryRepository.findByGymIdAndScanDateBetweenOrderByScanDateDesc(gymId,
                    startDate, endDate);
        } else {
            historyList = gymEntranceHistoryRepository.findByGymIdOrderByScanDateDesc(gymId);
        }

        long successfulScans = 0;
        long failedScans = 0;
        double totalProfit = 0.0;

        for (GymEntranceHistory h : historyList) {
            if ("ELIGIBLE".equalsIgnoreCase(h.getStatus()) || "Uğurlu".equalsIgnoreCase(h.getStatus())) {
                successfulScans++;
                if (h.getAmount() != null) {
                    totalProfit += h.getAmount();
                }
            } else {
                failedScans++;
            }
        }

        java.util.stream.Stream<GymEntranceHistory> stream = historyList.stream();
        if (statusFilter != null && !statusFilter.isBlank()) {
            if ("SUCCESSFUL".equalsIgnoreCase(statusFilter) || "Uğurlu".equalsIgnoreCase(statusFilter)
                    || "ELIGIBLE".equalsIgnoreCase(statusFilter)) {
                stream = stream.filter(
                        h -> "ELIGIBLE".equalsIgnoreCase(h.getStatus()) || "Uğurlu".equalsIgnoreCase(h.getStatus()));
            } else if ("UNSUCCESSFUL".equalsIgnoreCase(statusFilter) || "Uğursuz".equalsIgnoreCase(statusFilter)
                    || "Xəta".equalsIgnoreCase(statusFilter)) {
                stream = stream.filter(h -> "UNSUCCESSFUL".equalsIgnoreCase(h.getStatus())
                        || "Uğursuz".equalsIgnoreCase(h.getStatus()) || "Xəta".equalsIgnoreCase(h.getStatus()));
            }
        }

        List<GymEntranceHistory> filteredList = stream.collect(Collectors.toList());

        if (sort != null) {
            switch (sort) {
                case "date_asc":
                    filteredList.sort(Comparator.comparing(GymEntranceHistory::getScanDate,
                            Comparator.nullsLast(Comparator.naturalOrder())));
                    break;
                case "date_desc":
                    filteredList.sort(Comparator
                            .comparing(GymEntranceHistory::getScanDate, Comparator.nullsLast(Comparator.naturalOrder()))
                            .reversed());
                    break;
                case "status_asc":
                    filteredList
                            .sort(Comparator.comparing(GymEntranceHistory::getStatus, String.CASE_INSENSITIVE_ORDER));
                    break;
                case "status_desc":
                    filteredList.sort(Comparator.comparing(GymEntranceHistory::getStatus, String.CASE_INSENSITIVE_ORDER)
                            .reversed());
                    break;
            }
        }

        int from = Math.max(0, (page - 1) * pageSize);
        int to = Math.min(filteredList.size(), from + pageSize);
        List<GymEntranceHistory> pageItems = from >= filteredList.size() ? new java.util.ArrayList<>()
                : new java.util.ArrayList<>(filteredList.subList(from, to));

        List<GymEntranceHistoryAdminResponse> paginatedDtos = pageItems.stream().map(h -> {
            String firstName = "";
            String lastName = "";
            String phone = "";
            String profilePhotoUrl = "";
            try {
                az.fitnest.catalog.client.CachedUser user = userServiceGrpcClient.getUserById(h.getUserId());
                if (user != null) {
                    firstName = user.getFirstName();
                    lastName = user.getLastName();
                    phone = user.getMobile();
                    profilePhotoUrl = user.getProfileImageUrl();
                }
            } catch (Exception e) {
                firstName = "User";
                lastName = String.valueOf(h.getUserId());
            }
            String formattedDate = h.getScanDate()
                    .format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
            String displayStatus = "Uğursuz";
            if ("ELIGIBLE".equalsIgnoreCase(h.getStatus()) || "Uğurlu".equalsIgnoreCase(h.getStatus())) {
                displayStatus = "Uğurlu";
            }
            return GymEntranceHistoryAdminResponse.builder()
                    .id(h.getId())
                    .userId(h.getUserId())
                    .firstName(firstName)
                    .lastName(lastName)
                    .phone(phone)
                    .scanDateTime(formattedDate)
                    .status(displayStatus)
                    .reason(h.getReason())
                    .amount(h.getAmount() != null ? h.getAmount() : 0.0)
                    .profilePhotoUrl(profilePhotoUrl)
                    .build();
        }).collect(Collectors.toList());

        PaginatedResponse<GymEntranceHistoryAdminResponse> paginatedResponse = PaginatedResponse
                .<GymEntranceHistoryAdminResponse>builder()
                .items(paginatedDtos)
                .total((long) filteredList.size())
                .page(page)
                .pageSize(pageSize)
                .build();

        return az.fitnest.catalog.dto.response.GymAnalyticsResponse.builder()
                .totalProfit(totalProfit)
                .successfulScans(successfulScans)
                .failedScans(failedScans)
                .history(paginatedResponse)
                .build();
    }

    @Transactional(readOnly = true)
    public GymEntranceEligibilityResponse checkGymEntranceEligibility(Object principal) {
        Long userId = UserContext.extractUserId(principal);
        if (userId == null) {
            throw new IllegalArgumentException("error.unauthorized");
        }
        az.fitnest.order.grpc.ActiveSubscriptionResponse subResp = null;
        try {
            subResp = orderServiceGrpcClient.getActiveSubscription(userId);
            String status = subResp.getSubscriptionStatus();
            if (status == null || status.isEmpty() || status.equalsIgnoreCase("none")
                    || !status.equalsIgnoreCase("active")) {
                return GymEntranceEligibilityResponse.builder()
                        .allowed(false)
                        .status("INELIGIBLE")
                        .reason("NO_ACTIVE_SUBSCRIPTION")
                        .build();
            }
            int visitLimitRemaining = subResp.getRemainingLimit();
            if (visitLimitRemaining <= 0) {
                return GymEntranceEligibilityResponse.builder()
                        .allowed(false)
                        .status("INELIGIBLE")
                        .reason("VISIT_LIMIT_EXCEEDED")
                        .build();
            }
            return GymEntranceEligibilityResponse.builder()
                    .allowed(true)
                    .status("ELIGIBLE")
                    .build();
        } catch (Exception e) {
            return GymEntranceEligibilityResponse.builder()
                    .allowed(false)
                    .status("INELIGIBLE")
                    .reason("NO_ACTIVE_SUBSCRIPTION")
                    .build();
        }
    }

    private boolean isWithinWorkingHours(Gym gym, String gender) {
        boolean noHours = (gym.getGeneralWorkHours() == null || gym.getGeneralWorkHours().isEmpty()) &&
                (gym.getWorkHoursMan() == null || gym.getWorkHoursMan().isEmpty()) &&
                (gym.getWorkHoursWoman() == null || gym.getWorkHoursWoman().isEmpty());

        if (noHours) {
            return true;
        }

        java.time.LocalDateTime now = java.time.LocalDateTime.now(java.time.ZoneId.of("Asia/Baku"));
        java.time.DayOfWeek today = now.getDayOfWeek();
        java.time.LocalTime currentTime = now.toLocalTime();

        boolean allowedInGeneral = isTimeWithinSlots(gym.getGeneralWorkHours(), today, currentTime);
        if (allowedInGeneral) {
            return true;
        }

        if ("MALE".equalsIgnoreCase(gender)) {
            return isTimeWithinSlots(gym.getWorkHoursMan(), today, currentTime);
        } else if ("FEMALE".equalsIgnoreCase(gender)) {
            return isTimeWithinSlots(gym.getWorkHoursWoman(), today, currentTime);
        }

        return false;
    }

    private boolean isTimeWithinSlots(java.util.Collection<az.fitnest.catalog.model.entity.GymWorkHour> slots, java.time.DayOfWeek today, java.time.LocalTime currentTime) {
        if (slots == null || slots.isEmpty()) {
            return false;
        }

        az.fitnest.catalog.model.enums.GymWorkHourPeriod periodToday = az.fitnest.catalog.model.enums.GymWorkHourPeriod.valueOf(today.name());
        az.fitnest.catalog.model.enums.GymWorkHourPeriod periodYesterday = az.fitnest.catalog.model.enums.GymWorkHourPeriod.valueOf(today.minus(1).name());

        // Check if there is a slot starting today that covers current time
        boolean matchesToday = slots.stream()
                .filter(h -> h.getPeriod() == periodToday)
                .anyMatch(h -> {
                    java.time.LocalTime from = h.getFromTime();
                    java.time.LocalTime to = h.getToTime();
                    if (from == null && to == null) return true;
                    if (from == null) return !currentTime.isAfter(to);
                    if (to == null) return !currentTime.isBefore(from);
                    if (!from.isAfter(to)) {
                        // Normal interval e.g. 09:00 - 18:00
                        return !currentTime.isBefore(from) && !currentTime.isAfter(to);
                    } else {
                        // Overnight interval starting today e.g. 16:00 - 02:00
                        // Since it started today and hasn't ended yet, current time today must be >= from.
                        return !currentTime.isBefore(from);
                    }
                });

        if (matchesToday) {
            return true;
        }

        // Check if there is a slot starting yesterday that crossed midnight into today and covers current time
        boolean matchesYesterday = slots.stream()
                .filter(h -> h.getPeriod() == periodYesterday)
                .anyMatch(h -> {
                    java.time.LocalTime from = h.getFromTime();
                    java.time.LocalTime to = h.getToTime();
                    if (from != null && to != null && from.isAfter(to)) {
                        // Overnight interval starting yesterday e.g. 16:00 - 02:00.
                        // It crossed midnight and is still active today up until `to` (02:00).
                        return !currentTime.isAfter(to);
                    }
                    return false;
                });

        return matchesYesterday;
    }

    private Long extractGymIdFromQr(String qrCodeValue) {
        if (qrCodeValue == null || qrCodeValue.isBlank())
            return null;
        try {
            return Long.parseLong(qrCodeValue.trim());
        } catch (NumberFormatException e) {
            try {
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("/gym/(\\d+)");
                java.util.regex.Matcher matcher = pattern.matcher(qrCodeValue);
                if (matcher.find()) {
                    return Long.parseLong(matcher.group(1));
                }
                return null;
            } catch (Exception ex) {
                return null;
            }
        }
    }

    @Transactional(readOnly = true)
    public GymTypeCountResponse getGymCountByGender(String gender) {
        if (gender == null) {
            throw new BadRequestException("GENDER_REQUIRED", "error.gender_required");
        }
        long count;
        switch (gender.toLowerCase()) {
            case "female":
                count = gymRepository.countGymsWithWorkHoursWoman();
                break;
            case "male":
                count = gymRepository.countGymsWithWorkHoursMan();
                break;
            default:
                throw new BadRequestException("INVALID_GENDER", "error.invalid_gender");
        }
        return new GymTypeCountResponse(gender, count);
    }

    private String getTranslatedValueCached(
            Map<String, String> lookup,
            String entityType,
            String entityId,
            String fieldName,
            String userLanguage) {
        if (userLanguage == null || userLanguage.equalsIgnoreCase("AZ")) {
            return null;
        }
        String key = entityType.toUpperCase() + "_" + entityId + "_" + fieldName.toLowerCase();
        if (lookup != null && lookup.containsKey(key)) {
            return lookup.get(key);
        }
        return translationService.getTranslatedValue(entityType, entityId, fieldName, userLanguage);
    }

    private String getLocalizedAddressField(Long entityId, String entityType,
            az.fitnest.catalog.model.entity.Address address, String fieldName, String userLanguage) {
        return getLocalizedAddressField(entityId, entityType, address, fieldName, null, userLanguage);
    }

    private String getLocalizedAddressField(Long entityId, String entityType,
            az.fitnest.catalog.model.entity.Address address, String fieldName,
            Map<String, String> lookup, String userLanguage) {
        if (address == null)
            return null;
        String localized = getTranslatedValueCached(lookup, entityType, entityId.toString(), fieldName,
                userLanguage);
        if (localized == null || localized.isEmpty()) {
            try {
                java.lang.reflect.Field f = address.getClass().getDeclaredField(fieldName);
                f.setAccessible(true);
                Object v = f.get(address);
                if (v != null)
                    return v.toString();
            } catch (Exception ignored) {
            }
            return null;
        }
        return localized;
    }

    private Map<String, String> fetchTranslationsInBulk(List<Gym> gyms, String userLanguage) {
        if (userLanguage == null || userLanguage.equalsIgnoreCase("AZ") || gyms.isEmpty()) {
            return java.util.Collections.emptyMap();
        }

        List<String> gymIds = gyms.stream().map(g -> g.getId().toString()).toList();
        List<String> categoryIds = gyms.stream()
                .map(Gym::getCategory)
                .filter(java.util.Objects::nonNull)
                .map(c -> c.getCategoryId().toString())
                .distinct()
                .toList();
        List<String> subscriptionIds = gyms.stream()
                .flatMap(g -> g.getSubscriptions() != null ? g.getSubscriptions().stream() : java.util.stream.Stream.empty())
                .map(az.fitnest.catalog.model.entity.GymSubscription::getPackageId)
                .filter(java.util.Objects::nonNull)
                .map(Object::toString)
                .distinct()
                .toList();
        List<String> serviceIds = gyms.stream()
                .flatMap(g -> g.getSubscriptions() != null ? g.getSubscriptions().stream() : java.util.stream.Stream.empty())
                .flatMap(sub -> sub.getSupportedServices() != null ? sub.getSupportedServices().stream() : java.util.stream.Stream.empty())
                .map(b -> b.getId().toString())
                .distinct()
                .toList();

        Map<String, String> lookup = new java.util.HashMap<>();

        if (!gymIds.isEmpty()) {
            translationRepository.findByEntityTypeAndEntityIdInAndLanguageCode("GYM", gymIds, userLanguage.toUpperCase())
                    .forEach(t -> lookup.put(t.getEntityType() + "_" + t.getEntityId() + "_" + t.getFieldName().toLowerCase(), t.getFieldValue()));
        }
        if (!categoryIds.isEmpty()) {
            translationRepository.findByEntityTypeAndEntityIdInAndLanguageCode("CATEGORY", categoryIds, userLanguage.toUpperCase())
                    .forEach(t -> lookup.put(t.getEntityType() + "_" + t.getEntityId() + "_" + t.getFieldName().toLowerCase(), t.getFieldValue()));
        }
        if (!subscriptionIds.isEmpty()) {
            translationRepository.findByEntityTypeAndEntityIdInAndLanguageCode("GYMSUBSCRIPTION", subscriptionIds, userLanguage.toUpperCase())
                    .forEach(t -> lookup.put(t.getEntityType() + "_" + t.getEntityId() + "_" + t.getFieldName().toLowerCase(), t.getFieldValue()));
        }
        if (!serviceIds.isEmpty()) {
            translationRepository.findByEntityTypeAndEntityIdInAndLanguageCode("SUPPORTEDSERVICE", serviceIds, userLanguage.toUpperCase())
                    .forEach(t -> lookup.put(t.getEntityType() + "_" + t.getEntityId() + "_" + t.getFieldName().toLowerCase(), t.getFieldValue()));
        }

        return lookup;
    }

    private String getLocalizedGymName(Gym gym, String userLanguage) {
        if (gym == null)
            return null;
        return gym.getName();
    }

    @Override
    @Transactional(readOnly = true)
    public az.fitnest.catalog.dto.response.GymInfoAdminResponse getGymDetailsAdmin(Long gymId) {
        verifyGymOwnership(gymId);
        Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));

        String userLanguage = resolveUserLanguage();

        Long categoryId = null;
        String categoryName = null;
        if (gym.getCategory() != null) {
            Category cat = gym.getCategory();
            categoryId = cat.getId();
            categoryName = translationService.getTranslatedValue("CATEGORY", categoryId.toString(), "name", userLanguage);
            if (categoryName == null || categoryName.isEmpty()) {
                categoryName = cat.getName();
            }
        }

        List<az.fitnest.catalog.dto.response.RoomImageDto> roomDtos = new java.util.ArrayList<>();
        if (gym.getRooms() != null) {
            for (az.fitnest.catalog.model.entity.Room room : gym.getRooms()) {
                String imgUrl = room.getImages() != null && !room.getImages().isEmpty()
                        ? room.getImages().stream().findFirst().map(RoomImage::getPictureUrl).orElse(null)
                        : null;
                roomDtos.add(az.fitnest.catalog.dto.response.RoomImageDto.builder()
                        .id(room.getId())
                        .name(room.getName())
                        .imageUrl(imgUrl)
                        .build());
            }
        }

        String city = null;
        String addressText = null;
        Double lat = null;
        Double lng = null;
        if (gym.getAddress() != null) {
            city = getLocalizedAddressField(gym.getId(), "GYM", gym.getAddress(), "city", userLanguage);
            addressText = getLocalizedAddressField(gym.getId(), "GYM", gym.getAddress(), "addressText", userLanguage);
            lat = gym.getAddress().getLatitude();
            lng = gym.getAddress().getLongitude();
        }

        String created = "";
        if (gym.getCreatedDate() != null) {
            created = gym.getCreatedDate().format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        }

        List<LessonTypeResponse> lessonTypes = gymLessonTypeRepository.findByGymId(gymId).stream()
                .map(lt -> {
                    String localizedLt = translationService.getTranslatedValue("GymLessonType", lt.getId().toString(), "name", userLanguage);
                    return new LessonTypeResponse(lt.getId(), localizedLt != null ? localizedLt : lt.getName());
                })
                .toList();

        String localizedGymName = gym.getName();

        String localizedGymDescription = translationService.getTranslatedValue("GYM", gym.getId().toString(), "description", userLanguage);
        if (localizedGymDescription == null || localizedGymDescription.isEmpty()) {
            localizedGymDescription = gym.getDescription();
        }

        return az.fitnest.catalog.dto.response.GymInfoAdminResponse.builder()
                .id(gym.getId())
                .categoryId(categoryId)
                .categoryName(categoryName)
                .name(localizedGymName)
                .description(localizedGymDescription)
                .coverImageUrl(gym.getCoverImageUrl())
                .rooms(roomDtos)
                .phone(gym.getPhone())
                .email(gym.getEmail())
                .city(city)
                .address(addressText)
                .latitude(lat)
                .longitude(lng)
                .status(gym.getStatus())
                .createdAt(created)
                .lessonTypes(lessonTypes)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public az.fitnest.catalog.dto.response.GymWorkHoursAdminResponse getGymWorkHours(Long gymId) {
        verifyGymOwnership(gymId);
        Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));
        
        java.util.Set<GymWorkHourResponse> general = gym.getGeneralWorkHours().stream()
                .map(wh -> GymWorkHourResponse.builder()
                        .period(wh.getPeriod().name())
                        .from(wh.getFromTime())
                        .to(wh.getToTime())
                        .build())
                .collect(Collectors.toSet());

        java.util.Set<GymWorkHourResponse> woman = gym.getWorkHoursWoman().stream()
                .map(wh -> GymWorkHourResponse.builder()
                        .period(wh.getPeriod().name())
                        .from(wh.getFromTime())
                        .to(wh.getToTime())
                        .build())
                .collect(Collectors.toSet());

        java.util.Set<GymWorkHourResponse> man = gym.getWorkHoursMan().stream()
                .map(wh -> GymWorkHourResponse.builder()
                        .period(wh.getPeriod().name())
                        .from(wh.getFromTime())
                        .to(wh.getToTime())
                        .build())
                .collect(Collectors.toSet());

        java.util.Set<az.fitnest.catalog.dto.request.RestDayRequest> rests = gym.getRestDays().stream()
                .map(rd -> new az.fitnest.catalog.dto.request.RestDayRequest(rd.name()))
                .collect(Collectors.toSet());

        return az.fitnest.catalog.dto.response.GymWorkHoursAdminResponse.builder()
                .generalWorkHours(general)
                .workHoursWoman(woman)
                .workHoursMan(man)
                .restDays(rests)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public az.fitnest.catalog.dto.response.GymSubscriptionsAdminResponse getGymSubscriptions(Long gymId) {
        Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));

        String userLanguage = resolveUserLanguage();
        List<az.fitnest.catalog.dto.response.GymPlanItemAdminResponse> subscriptions = new java.util.ArrayList<>();

        if (gym.getSubscriptions() != null && !gym.getSubscriptions().isEmpty()) {
            try {
                List<Long> packageIds = gym.getSubscriptions().stream()
                        .map(sub -> sub.getPackageId())
                        .filter(java.util.Objects::nonNull)
                        .toList();

                List<az.fitnest.order.grpc.PackageNameInfo> packageInfos = orderServiceGrpcClient
                        .getPackageNamesByIds(packageIds);
                java.util.Map<Long, az.fitnest.order.grpc.PackageNameInfo> idToInfo = packageInfos.stream()
                        .collect(java.util.stream.Collectors.toMap(
                                az.fitnest.order.grpc.PackageNameInfo::getPackageId,
                                p -> p));

                subscriptions = gym.getSubscriptions().stream()
                        .filter(sub -> sub.getPackageId() != null)
                        .filter(sub -> idToInfo.get(sub.getPackageId()) != null)
                        .map(sub -> {
                            az.fitnest.order.grpc.PackageNameInfo info = idToInfo.get(sub.getPackageId());
                            String localizedName = translationService.getTranslatedValue("GYMSUBSCRIPTION",
                                    sub.getPackageId().toString(), "name", userLanguage);
                            if (localizedName == null || localizedName.isEmpty())
                                localizedName = info.getName();

                            List<az.fitnest.catalog.dto.response.GymPlanBenefitAdminResponse> benefits = sub
                                    .getSupportedServices().stream()
                                    .map(b -> {
                                        String localizedBenefit = translationService.getTranslatedValue(
                                                "SUPPORTEDSERVICE", b.getId().toString(), "name", userLanguage);
                                        return az.fitnest.catalog.dto.response.GymPlanBenefitAdminResponse.builder()
                                                .id(b.getId())
                                                .name(localizedBenefit != null && !localizedBenefit.isEmpty()
                                                        ? localizedBenefit
                                                        : b.getName())
                                                .iconImageUrl(b.getIconUrl())
                                                .build();
                                    }).collect(java.util.stream.Collectors.toList());

                            return az.fitnest.catalog.dto.response.GymPlanItemAdminResponse.builder()
                                    .packageId(sub.getPackageId())
                                    .packageName(localizedName)
                                    .dailyPrice(sub.getDailyPrice())
                                    .benefits(benefits)
                                    .build();
                        }).collect(java.util.stream.Collectors.toList());
            } catch (Exception e) {
            }
        }

        return az.fitnest.catalog.dto.response.GymSubscriptionsAdminResponse.builder()
                .gymId(gymId)
                .subscriptions(subscriptions)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<GymAdminResponse> getGymAdmins(Long gymId) {
        if (!gymRepository.existsById(gymId)) {
            throw new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found");
        }

        String userLanguage = resolveUserLanguage();

        return gymAdminRepository.findByGymId(gymId).stream()
                .map(a -> {
                    String localizedName = translationService.getTranslatedValue("GymAdmin", a.getId().toString(), "name", userLanguage);
                    if (localizedName == null || localizedName.isEmpty()) {
                        localizedName = a.getName();
                    }
                    String localizedSurname = translationService.getTranslatedValue("GymAdmin", a.getId().toString(), "surname", userLanguage);
                    if (localizedSurname == null || localizedSurname.isEmpty()) {
                        localizedSurname = a.getSurname();
                    }
                    return GymAdminResponse.builder()
                            .id(a.getId())
                            .userId(a.getUserId())
                            .name(localizedName)
                            .surname(localizedSurname)
                            .phone(a.getPhoneNumber())
                            .email(a.getEmail())
                            .role(a.getRole())
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<ReservationAdminResponse> getGymReservationsAdmin(Long gymId, ReservationStatus status, int page, int pageSize) {
        verifyGymOwnership(gymId);
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), pageSize, Sort.by(Sort.Direction.DESC, "createdDate"));
        Page<Reservation> reservationPage;

        if (status == null) {
            reservationPage = reservationRepository.findByGymId(gymId, pageable);
        } else {
            reservationPage = reservationRepository.findByGymIdAndStatus(gymId, status, pageable);
        }

        List<ReservationAdminResponse> items = reservationPage.getContent().stream()
                .map(r -> {
                    String userFullName = "N/A";
                    try {
                        CachedUser user = userServiceGrpcClient.getUserById(r.getUserId());
                        if (user != null) {
                            userFullName = user.getFirstName() + " " + user.getLastName();
                        }
                    } catch (Exception e) {
                    }

                    String dateStr = r.getReservationDate() != null ? r.getReservationDate().getDate().toString() : "N/A";
                    String timeRange = r.getReservationDate() != null ?
                            r.getReservationDate().getStartTime() + " - " + r.getReservationDate().getEndTime() : "N/A";
                    String trainerName = r.getTrainer() != null ? r.getTrainer().getName() + " " + r.getTrainer().getSurname() : "N/A";

                    return new ReservationAdminResponse(
                        r.getId(),
                        userFullName,
                        dateStr,
                        timeRange,
                        r.getStatus(),
                        trainerName
                    );
                })
                .collect(Collectors.toList());

        return PaginatedResponse.<ReservationAdminResponse>builder()
                .items(items)
                .total(reservationPage.getTotalElements())
                .page(page)
                .pageSize(pageSize)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ReservationDetailAdminResponse getReservationDetailAdmin(Long reservationId) {
        Reservation r = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("RESERVATION_NOT_FOUND", "error.reservation_not_found"));

        verifyGymOwnership(r.getGym().getId());

        CachedUser user = null;
        try {
            user = userServiceGrpcClient.getUserById(r.getUserId());
        } catch (Exception e) {
        }

        String userFullName = user != null ? user.getFirstName() + " " + user.getLastName() : "N/A";
        String userPhone = user != null ? user.getMobile() : "N/A";
        String userEmail = user != null ? user.getEmail() : "N/A";
        String birthDate = "N/A";
        String regDate = r.getCreatedDate() != null ? r.getCreatedDate().format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy")) : "N/A";
        String platform = "N/A";

        String dateStr = r.getReservationDate() != null ? r.getReservationDate().getDate().toString() : "N/A";
        String timeRange = r.getReservationDate() != null ?
                r.getReservationDate().getStartTime() + " - " + r.getReservationDate().getEndTime() : "N/A";
        String trainerName = r.getTrainer() != null ? r.getTrainer().getName() + " " + r.getTrainer().getSurname() : "N/A";

        return new ReservationDetailAdminResponse(
            r.getId(),
            r.getUserId(),
            userFullName,
            userPhone,
            userEmail,
            birthDate,
            regDate,
            platform,
            r.getStatus(),
            trainerName,
            r.getLessonType(),
            dateStr,
            timeRange,
            r.getCancelReasonText()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ReservationStatsResponse getGymReservationStats(Long gymId) {
        verifyGymOwnership(gymId);
        long total = reservationRepository.countByGymId(gymId);
        long pending = reservationRepository.countByGymIdAndStatus(gymId, ReservationStatus.PENDING);
        long confirmed = reservationRepository.countByGymIdAndStatus(gymId, ReservationStatus.APPROVED);
        long cancelled = reservationRepository.countByGymIdAndStatus(gymId, ReservationStatus.CANCELLED);

        return new ReservationStatsResponse(total, pending, confirmed, cancelled);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<ReservationAdminResponse> getAllReservationsAdmin(ReservationStatus status, int page, int pageSize) {
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), pageSize, Sort.by(Sort.Direction.DESC, "createdDate"));
        Page<Reservation> reservationPage;

        if (status == null) {
            reservationPage = reservationRepository.findAll(pageable);
        } else {
            reservationPage = reservationRepository.findByStatus(status, pageable);
        }

        List<ReservationAdminResponse> items = reservationPage.getContent().stream()
                .map(r -> {
                    String userFullName = "N/A";
                    try {
                        CachedUser user = userServiceGrpcClient.getUserById(r.getUserId());
                        if (user != null) {
                            userFullName = user.getFirstName() + " " + user.getLastName();
                        }
                    } catch (Exception e) {
                    }

                    String dateStr = r.getReservationDate() != null ? r.getReservationDate().getDate().toString() : "N/A";
                    String timeRange = r.getReservationDate() != null ?
                            r.getReservationDate().getStartTime() + " - " + r.getReservationDate().getEndTime() : "N/A";
                    String trainerName = r.getTrainer() != null ? r.getTrainer().getName() + " " + r.getTrainer().getSurname() : "N/A";

                    return new ReservationAdminResponse(
                        r.getId(),
                        userFullName,
                        dateStr,
                        timeRange,
                        r.getStatus(),
                        trainerName
                    );
                })
                .collect(Collectors.toList());

        return PaginatedResponse.<ReservationAdminResponse>builder()
                .items(items)
                .total(reservationPage.getTotalElements())
                .page(page)
                .pageSize(pageSize)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<az.fitnest.catalog.dto.response.LessonHourResponse> getGymLessonHoursAdmin(Long gymId, java.time.LocalDate startDate, java.time.LocalDate endDate, int page, int pageSize) {
        List<az.fitnest.catalog.dto.response.LessonHourResponse> allHours = trainerReservationDateRepository.findByGymId(gymId).stream()
                .filter(trd -> startDate == null || !trd.getDate().isBefore(startDate))
                .filter(trd -> endDate == null || !trd.getDate().isAfter(endDate))
                .map(trd -> new az.fitnest.catalog.dto.response.LessonHourResponse(
                        trd.getId(),
                        trd.getTrainer() != null ? trd.getTrainer().getName() + " " + trd.getTrainer().getSurname() : "N/A",
                        trd.getClassType() != null ? trd.getClassType().getName() : "N/A",
                        trd.getDate(),
                        trd.getStartTime() + " - " + trd.getEndTime(),
                        trd.getEmptySpaces(),
                        trd.getStatus()
                ))
                .sorted(Comparator.comparing(az.fitnest.catalog.dto.response.LessonHourResponse::date)
                        .thenComparing(az.fitnest.catalog.dto.response.LessonHourResponse::timeRange))
                .collect(Collectors.toList());

        int from = Math.max(0, (page - 1) * pageSize);
        int to = Math.min(allHours.size(), from + pageSize);
        List<az.fitnest.catalog.dto.response.LessonHourResponse> pageItems = from >= allHours.size() ? new java.util.ArrayList<>()
                : new java.util.ArrayList<>(allHours.subList(from, to));

        return PaginatedResponse.<az.fitnest.catalog.dto.response.LessonHourResponse>builder()
                .items(pageItems)
                .total((long) allHours.size())
                .page(page)
                .pageSize(pageSize)
                .build();
    }
    private String cleanPackageName(String name) {
        if (name == null) return null;
        return name.replace(" Plan", "").replace(" plan", "").trim();
    }

    private void verifyGymOwnership(Long gymId) {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new UnauthorizedException("Unauthorized");
        }
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) {
            return;
        }
        Long userId = UserContext.getCurrentUserId();
        if (userId == null || !gymAdminRepository.existsByGymIdAndUserId(gymId, userId)) {
            throw new ForbiddenException("You do not have access to this gym");
        }
    }

    private int getPackageRank(String packageName) {
        if (packageName == null) {
            return 0;
        }
        String lower = packageName.toLowerCase();
        if (lower.contains("platinum")) {
            return 4;
        }
        if (lower.contains("gold")) {
            return 3;
        }
        if (lower.contains("silver")) {
            return 2;
        }
        if (lower.contains("bronze")) {
            return 1;
        }
        return 0;
    }

    private List<Long> getEligibleSubscriptionIds(Long subscriptionId) {
        if (subscriptionId == null) {
            return java.util.Collections.emptyList();
        }
        if (eligibleSubscriptionIdsCache.size() >= 10000) {
            eligibleSubscriptionIdsCache.clear();
        }
        return eligibleSubscriptionIdsCache.computeIfAbsent(subscriptionId, id -> {
            try {
                List<az.fitnest.order.grpc.SubscriptionPackageInfo> allPlans = orderServiceGrpcClient.getGymPlans();
                String userPackageName = null;
                for (var plan : allPlans) {
                    if (plan.getPackageId() == id.longValue()) {
                        userPackageName = plan.getName();
                        break;
                    }
                }
                if (userPackageName == null) {
                    List<az.fitnest.order.grpc.PackageNameInfo> nameInfos = orderServiceGrpcClient.getPackageNamesByIds(java.util.List.of(id));
                    if (!nameInfos.isEmpty()) {
                        userPackageName = nameInfos.get(0).getName();
                    }
                }
                
                int userRank = getPackageRank(userPackageName);
                List<Long> eligibleIds = new java.util.ArrayList<>();
                for (var plan : allPlans) {
                    int planRank = getPackageRank(plan.getName());
                    if (planRank <= userRank) {
                        eligibleIds.add(plan.getPackageId());
                    }
                }
                if (!eligibleIds.contains(id)) {
                    eligibleIds.add(id);
                }
                return eligibleIds;
            } catch (Exception e) {
                return java.util.Collections.singletonList(id);
            }
        });
    }
}
