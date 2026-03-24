package az.fitnest.catalog.service.impl;
import az.fitnest.catalog.model.entity.Category;

import az.fitnest.catalog.dto.*;
import az.fitnest.catalog.mapper.GymMapper;
import az.fitnest.catalog.exception.ResourceNotFoundException;
import az.fitnest.catalog.exception.BadRequestException;
import az.fitnest.catalog.model.entity.Address;
import az.fitnest.catalog.model.entity.Gym;
import az.fitnest.catalog.model.entity.GymImage;
import az.fitnest.catalog.model.entity.SavedGym;
import az.fitnest.catalog.model.entity.Trainer;
import az.fitnest.catalog.repository.GymImageRepository;
import az.fitnest.catalog.repository.GymRepository;
import az.fitnest.catalog.repository.ReviewRepository;
import az.fitnest.catalog.repository.TrainerRepository;
import az.fitnest.catalog.repository.CategoryRepository;
import az.fitnest.catalog.service.ReverseGeocodingService;
import az.fitnest.catalog.client.OrderServiceGrpcClient;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
public class GymReadService {
    private final GymRepository gymRepository;
    private final az.fitnest.catalog.repository.SavedGymRepository savedGymRepository;
    private final GymImageRepository gymImageRepository;
    private final TrainerRepository trainerRepository;
    private final ReviewRepository reviewRepository;
    private final OrderServiceGrpcClient orderServiceGrpcClient;
    private final org.springframework.context.MessageSource messageSource;
    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    @Cacheable("gym-detail")
    public GymDetailResponse getGymDetail(Long userId, Long gymId) {
        ExecutorService executor = Executors.newFixedThreadPool(6);
        CompletableFuture<Gym> gymFuture = CompletableFuture.supplyAsync(() -> gymRepository.findWithDetailsById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found")), executor);
        CompletableFuture<Boolean> isSavedFuture = CompletableFuture.supplyAsync(() -> {
            if (userId != null) {
                return savedGymRepository.findByUserIdAndGymId(userId, gymId).isPresent();
            }
            return false;
        }, executor);
        CompletableFuture<List<GymTrainerDto>> trainerDtosFuture = CompletableFuture.supplyAsync(() ->
            trainerRepository.findByGymId(gymId, PageRequest.of(0, 5, Sort.by("id")))
                .getContent().stream()
                .map(GymMapper::toTrainerDto)
                .collect(Collectors.toList()), executor);
        CompletableFuture<List<GymReviewDto>> recentReviewsFuture = CompletableFuture.supplyAsync(() ->
            reviewRepository.findByGymId(gymId, PageRequest.of(0, 3, Sort.by(Sort.Direction.DESC, "createdDate")))
                .getContent().stream()
                .map(GymMapper::toReviewDto)
                .collect(Collectors.toList()), executor);
        CompletableFuture<List<GymWorkHourDto>> generalWorkHoursFuture = CompletableFuture.supplyAsync(() ->
            gymRepository.findGeneralWorkHoursByGymId(gymId).stream()
                .map(GymMapper::toWorkHourDto)
                .collect(Collectors.toList()), executor);
        CompletableFuture<List<CategoryDto>> categoryDtosFuture = gymFuture.thenApplyAsync(gym -> {
            if (gym != null && gym.getCategories() != null) {
                return gym.getCategories().stream()
                        .map(GymMapper::toCategoryDto)
                        .collect(Collectors.toList());
            }
            return null;
        }, executor);
        CompletableFuture<List<GymRoomDto>> roomsFuture = gymFuture.thenApplyAsync(gym -> {
            List<GymRoomDto> rooms = new java.util.ArrayList<>();
            if (gym != null && gym.getRooms() != null) {
                rooms = gym.getRooms().stream().map(room -> {
                    List<GymImageDto> images = room.getImages().stream().map(img ->
                        GymImageDto.builder()
                                .id(img.getId())
                                .gymId(gym.getId())
                                .name(room.getName())
                                .url(img.getPictureUrl())
                                .build()
                    ).collect(Collectors.toList());
                    return GymRoomDto.builder().room_name(room.getName()).images(images).build();
                }).collect(Collectors.toList());
            }
            return rooms;
        }, executor);

        CompletableFuture.allOf(
            gymFuture, isSavedFuture, trainerDtosFuture, recentReviewsFuture, generalWorkHoursFuture, categoryDtosFuture, roomsFuture
        ).join();
        Gym gym = gymFuture.join();
        boolean isSaved = isSavedFuture.join();
        List<GymTrainerDto> trainerDtos = trainerDtosFuture.join();
        List<GymReviewDto> recentReviews = recentReviewsFuture.join();
        java.util.Set<GymWorkHourDto> generalWorkHours = null;
        List<GymWorkHourDto> generalWorkHoursList = generalWorkHoursFuture.join();
        if (generalWorkHoursList != null && !generalWorkHoursList.isEmpty()) {
            generalWorkHours = new java.util.HashSet<>(generalWorkHoursList);
        }
        List<CategoryDto> categoryDtos = categoryDtosFuture.join();
        List<GymRoomDto> rooms = roomsFuture.join();

        java.util.Set<GymWorkHourDto> workHoursWoman = null;
        if (gym.getWorkHoursWoman() != null && !gym.getWorkHoursWoman().isEmpty()) {
            workHoursWoman = gym.getWorkHoursWoman().stream()
                .map(GymMapper::toWorkHourDto)
                .collect(java.util.stream.Collectors.toSet());
        }
        java.util.Set<GymWorkHourDto> workHoursMan = null;
        if (gym.getWorkHoursMan() != null && !gym.getWorkHoursMan().isEmpty()) {
            workHoursMan = gym.getWorkHoursMan().stream()
                .map(GymMapper::toWorkHourDto)
                .collect(java.util.stream.Collectors.toSet());
        }
        if (generalWorkHours != null && generalWorkHours.isEmpty()) generalWorkHours = null;
        GymDetailResponse response = GymDetailResponse.builder()
                .gym_id(gym.getId().toString())
                .name(gym.getName())
                .description(gym.getDescription())
                .isSaved(isSaved)
                .address(gym.getAddress() != null ? az.fitnest.catalog.dto.LocationDto.builder()
                        .addressText(gym.getAddress().getAddressText())
                        .city(gym.getAddress().getCity())
                        .latitude(gym.getAddress().getLatitude())
                        .longitude(gym.getAddress().getLongitude())
                        .build() : null)
                .phone(gym.getPhone())
                .email(gym.getEmail())
                .general_work_hours(generalWorkHours)
                .work_hours_woman(workHoursWoman)
                .work_hours_man(workHoursMan)
                .rooms(rooms)
                .trainers(trainerDtos)
                .recent_reviews(recentReviews)
                .categories(categoryDtos)
                .coverImageUrl(gym.getCoverImageUrl())
                .rating(gym.getRating())
                .reviewsCount(gym.getReviewsCount())
                .qr_code_url(gym.getQrCodeUrl())
                .status(gym.getStatus())
                .build();
        executor.shutdown();
        return response;
    }

    @Transactional(readOnly = true)
    @Cacheable("gym-images")
    public GymImageResponse getGymImages(Long gymId) {
        Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));

        List<GymImageItemDto> items = gymImageRepository.findByGymId(gymId).stream()
                .map(GymMapper::toImageItemDto)
                .collect(Collectors.toList());

        if (gym.getRooms() != null) {
            gym.getRooms().forEach(room -> {
                room.getImages().forEach(img -> {
                    items.add(GymImageItemDto.builder()
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
    public ReservationRulesResponse getReservationRules(Long gymId) {
        if (!gymRepository.existsById(gymId)) {
            throw new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found");
        }
        return new ReservationRulesResponse(false, Map.of("max_reservations_per_day", 1, "cancel_before_minutes", 60));
    }

    @Transactional(readOnly = true)
    public List<GymNearbyResponseDto> getNearbyGyms(double lat, double lng, double radiusKm) {
        double[] bbox = boundingBox(lat, lng, radiusKm);
        List<Gym> candidates = gymRepository.findByAddressLatitudeBetweenAndAddressLongitudeBetween(bbox[0], bbox[1], bbox[2], bbox[3]);
        LocalDateTime newThreshold = LocalDateTime.now().minusDays(30L);

        return candidates.stream()
                .filter(gym -> gym.getAddress() != null && gym.getAddress().getLatitude() != null && gym.getAddress().getLongitude() != null)
                .map(gym -> {
                    double rawDistance = calculateDistanceRaw(lat, lng, gym.getAddress().getLatitude(), gym.getAddress().getLongitude());
                    boolean isNew = gym.getCreatedDate() != null && gym.getCreatedDate().isAfter(newThreshold);
                    return new Object() {
                        GymNearbyResponseDto dto = GymNearbyResponseDto.builder()
                                .gymId(gym.getId())
                                .name(gym.getName())
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
    @Cacheable("main-page-gyms")
    public PaginatedResponse<GymMainPageDto> getClosestGyms(Long userId, int page, int pageSize, Double userLat, Double userLng) {
        return getGyms(userId, null, "CLOSEST", null, null, page, pageSize, userLat, userLng, "desc");
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<GymMainPageDto> getGyms(Long userId, String q, String type, Long categoryId, Long subscriptionId, int page, int pageSize, Double userLat, Double userLng, String sortDir) {
        if (categoryId != null && !categoryRepository.existsById(categoryId)) {
            throw new BadRequestException("INVALID_CATEGORY", "error.invalid_category");
        }
        Page<Gym> gymPage;
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = pageable(page, pageSize, Sort.by(direction, "createdDate"));

        if ("SAVED".equalsIgnoreCase(type)) {
            if (userId == null) return emptyPaginatedResponse(page, pageSize);
            List<SavedGym> saved = savedGymRepository.findByUserId(userId);
            List<Gym> candidates = saved.stream().map(SavedGym::getGym).toList();
            return manualPaginate(candidates, userId, userLat, userLng, page, pageSize, q, categoryId);
        }

        if (userLat != null && userLng != null) {
            double initialRadiusKm = 50.0;
            double[] bbox = boundingBox(userLat, userLng, initialRadiusKm);
            if (q != null && !q.isBlank()) {
                if (categoryId != null) {
                    gymPage = gymRepository.findByNameOrDescriptionContainingIgnoreCaseAndCategory(q, categoryId, pageable);
                } else {
                    gymPage = gymRepository.searchByNameAddressCategory(q, pageable);
                }
            } else if ("CLOSEST".equalsIgnoreCase(type)) {
                if (categoryId != null) {
                    gymPage = gymRepository.findByCategory(categoryId, pageable);
                } else {
                    gymPage = gymRepository.findClosestGyms(bbox[0], bbox[1], bbox[2], bbox[3], userLat, userLng, pageable);
                }
            } else {
                if (categoryId != null) {
                    gymPage = gymRepository.findByCategory(categoryId, pageable);
                } else {
                    gymPage = gymRepository.findAll(pageable);
                }
            }
        } else {
            if (q != null && !q.isBlank()) {
                if (categoryId != null) {
                    gymPage = gymRepository.findByNameOrDescriptionContainingIgnoreCaseAndCategory(q, categoryId, pageable);
                } else {
                    gymPage = gymRepository.searchByNameAddressCategory(q, pageable);
                }
            } else {
                if (categoryId != null) {
                    gymPage = gymRepository.findByCategory(categoryId, pageable);
                } else {
                    gymPage = gymRepository.findAll(pageable);
                }
            }
        }

        List<Long> savedGymIds = new java.util.ArrayList<>();
        if (userId != null) {
            List<Long> gymIds = gymPage.getContent().stream().map(Gym::getId).toList();
            if (!gymIds.isEmpty()) {
                savedGymIds = savedGymRepository.findGymIdsByUserIdAndGymIdIn(userId, gymIds);
            }
        }

        final List<Long> finalSavedIds = savedGymIds;
        List<GymMainPageDto> items = gymPage.getContent().stream().map(gym -> mapToGymMainPageDto(gym, userId, userLat, userLng, finalSavedIds.contains(gym.getId())))
                .filter(gymDto -> {
                    if (subscriptionId == null) return true;
                    Gym gym = gymPage.getContent().stream().filter(g -> g.getId().toString().equals(gymDto.gymId())).findFirst().orElse(null);
                    if (gym == null || gym.getSubscriptions() == null) return false;
                    return gym.getSubscriptions().stream().anyMatch(sub -> subscriptionId.equals(sub.getPackageId()));
                })
                .collect(Collectors.toList());

        String message = null;
        if (items.isEmpty()) {
            message = messageSource.getMessage("error.gym_not_found", null, org.springframework.context.i18n.LocaleContextHolder.getLocale());
        }
        return PaginatedResponse.<GymMainPageDto>builder()
                .items(items)
                .total(gymPage.getTotalElements())
                .page(page)
                .pageSize(pageSize)
                .message(message)
                .build();
    }

    private PaginatedResponse<GymMainPageDto> manualPaginate(List<Gym> candidates, Long userId, Double lat, Double lng, int page, int pageSize, String q, Long categoryId) {
        java.util.stream.Stream<Gym> stream = candidates.stream();
        if (q != null && !q.isBlank()) {
            String lowerQ = q.toLowerCase();
            stream = stream.filter(g -> (g.getName() != null && g.getName().toLowerCase().contains(lowerQ)) ||
                    (g.getAddress() != null && g.getAddress().getAddressText() != null && g.getAddress().getAddressText().toLowerCase().contains(lowerQ)));
        }
        if (categoryId != null) {
            stream = stream.filter(g -> g.getCategories() != null && g.getCategories().stream().anyMatch(c -> c.getId().equals(categoryId)));
        }

        List<GymMainPageDto> all = stream.map(g -> mapToGymMainPageDto(g, userId, lat, lng, true)).collect(Collectors.toList());

        if (lat != null && lng != null) {
            all.sort(Comparator.comparing(GymMainPageDto::distanceKm, Comparator.nullsLast(Comparator.naturalOrder())));
        }

        int from = Math.max(0, (page - 1) * pageSize);
        int to = Math.min(all.size(), from + pageSize);
        List<GymMainPageDto> pageItems = from >= all.size() ? new java.util.ArrayList<>() : new java.util.ArrayList<>(all.subList(from, to));

        return PaginatedResponse.<GymMainPageDto>builder()
                .items(pageItems)
                .total(all.size())
                .page(page)
                .pageSize(pageSize)
                .build();
    }

    private GymMainPageDto mapToGymMainPageDto(Gym gym, Long userId, Double userLat, Double userLng, boolean isSaved) {
        double stars = gym.getRating() != null ? gym.getRating() : 0.0;
        boolean isNew = gym.getCreatedDate() != null && gym.getCreatedDate().isAfter(LocalDateTime.now().minusMonths(1L));
        Address address = gym.getAddress();
        Double distanceKm = null;
        if (userLat != null && userLng != null && address != null && address.getLatitude() != null && address.getLongitude() != null) {
            distanceKm = Math.round(calculateDistanceRaw(userLat, userLng, address.getLatitude(), address.getLongitude()) * 10.0) / 10.0;
        }

        List<CategoryDto> categories = gym.getCategories() != null ?
            gym.getCategories().stream()
                .map(c -> CategoryDto.builder()
                    .id(c.getId())
                    .name(c.getName())
                    .photoUrl(c.getPhotoUrl())
                    .build())
                .collect(Collectors.toList())
            : java.util.Collections.emptyList();

        List<GymPlanItemDto> supportedSubscriptions = new java.util.ArrayList<>();
        try {
            if (gym.getSubscriptions() != null && !gym.getSubscriptions().isEmpty()) {
                List<Long> packageIds = gym.getSubscriptions().stream()
                    .map(sub -> sub.getPackageId())
                    .filter(java.util.Objects::nonNull)
                    .toList();
                List<az.fitnest.order.grpc.PackageNameInfo> packageInfos = orderServiceGrpcClient.getPackageNamesByIds(packageIds);
                java.util.Map<Long, az.fitnest.order.grpc.PackageNameInfo> idToInfo = packageInfos.stream()
                    .collect(java.util.stream.Collectors.toMap(
                        az.fitnest.order.grpc.PackageNameInfo::getPackageId,
                        p -> p
                    ));
                supportedSubscriptions = gym.getSubscriptions().stream().map(sub -> {
                    az.fitnest.order.grpc.PackageNameInfo info = idToInfo.get(sub.getPackageId());
                    String planId = sub.getPackageId() != null ? sub.getPackageId().toString() : "N/A";
                    String packageName = info != null ? info.getName() : "Unknown";
                    List<String> benefitsList = sub.getBenefits().stream()
                        .map(az.fitnest.catalog.model.entity.GymSubscriptionBenefit::getBenefit)
                        .toList();
                    return GymPlanItemDto.builder()
                        .plan_id(planId)
                        .packageName(packageName)
                        .benefits(benefitsList)
                        .build();
                }).collect(java.util.stream.Collectors.toList());
            }
        } catch (Exception e) {
            System.err.println("Could not fetch supported subscriptions from order-service: " + e.getMessage());
            if (gym.getSubscriptions() != null) {
                supportedSubscriptions = gym.getSubscriptions().stream().map(sub -> {
                    String planId = sub.getPackageId() != null ? sub.getPackageId().toString() : "N/A";
                    String placeholderName = "Standard Plan";
                    return GymPlanItemDto.builder()
                        .plan_id(planId)
                        .packageName(placeholderName)
                        .benefits(sub.getBenefits().stream().map(az.fitnest.catalog.model.entity.GymSubscriptionBenefit::getBenefit).toList())
                        .build();
                }).collect(java.util.stream.Collectors.toList());
            }
        }
        return GymMainPageDto.builder()
                .gymId(gym.getId().toString())
                .name(gym.getName())
                .coverImageUrl(gym.getCoverImageUrl())
                .stars(stars)
                .isNew(isNew)
                .location(address != null ? address.getAddressText() : null)
                .city(address != null ? address.getCity() : null)
                .distanceKm(distanceKm)
                .isSaved(isSaved)
                .categories(categories)
                .supportedSubscriptions(supportedSubscriptions)
                .build();
    }

    private PaginatedResponse<GymMainPageDto> emptyPaginatedResponse(int page, int pageSize) {
        return PaginatedResponse.<GymMainPageDto>builder().items(java.util.Collections.emptyList()).total(0).page(page).pageSize(pageSize).build();
    }

    @Transactional(readOnly = true)
    public LocationDto getGymLocation(Long gymId) {
        Gym gym = gymRepository.findById(gymId).orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));
        Address addr = gym.getAddress();
        if (addr == null) {
            return LocationDto.builder().build();
        }
        return LocationDto.builder()
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
        Long userId = extractUserId(principal);
        if (userId == null) {
            throw new IllegalArgumentException("Unauthorized");
        }
        az.fitnest.order.grpc.ActiveSubscriptionResponse subResp = null;
        try {
            subResp = orderServiceGrpcClient.getActiveSubscription(userId);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to fetch subscription: " + e.getMessage());
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
        List<Gym> gyms = gymRepository.findAll();
        if (categoryId != null) {
            gyms = gyms.stream()
                    .filter(g -> g.getCategories() != null && g.getCategories().stream().anyMatch(c -> c.getId().equals(categoryId)))
                    .toList();
        }
        if (subscriptionId != null) {
            gyms = gyms.stream()
                    .filter(g -> g.getSubscriptions() != null)
                    .toList();
        }
        if (type != null && type.equalsIgnoreCase("new")) {
            gyms = gyms.stream()
                    .filter(g -> g.getCreatedDate() != null && g.getCreatedDate().isAfter(java.time.LocalDateTime.now().minusWeeks(1)))
                    .toList();
        }
        return new GymCountResponse(
            gyms.stream()
                .filter(g -> g.getSubscriptions() != null)
                .count(),
            type != null ? type : "all",
            subscriptionId,
            categoryId
        );
    }

    @Transactional(readOnly = true)
    public GymTypeCountResponse getGymCountByType(String type) {
        List<Gym> gyms = gymRepository.findAll();
        long count;
        if (type.equalsIgnoreCase("new")) {
            count = gyms.stream()
                .filter(g -> g.getCreatedDate() != null && g.getCreatedDate().isAfter(java.time.LocalDateTime.now().minusWeeks(1)))
                .count();
        } else {
            count = gyms.size();
        }
        return new GymTypeCountResponse(type, count);
    }

    @Transactional(readOnly = true)
    public List<GymCategoryCountResponse> getGymCountByCategory() {
        List<Gym> gyms = gymRepository.findAll();
        java.util.Map<Long, Long> categoryCounts = gyms.stream()
            .flatMap(gym -> gym.getCategories() != null ? gym.getCategories().stream() : java.util.stream.Stream.empty())
            .collect(java.util.stream.Collectors.groupingBy(
                category -> category.getCategoryId(),
                java.util.stream.Collectors.counting()
            ));
        List<Category> allCategories = categoryRepository.findAllById(categoryCounts.keySet());
        java.util.Map<Long, String> idToName = allCategories.stream()
            .collect(java.util.stream.Collectors.toMap(Category::getCategoryId, Category::getName));
        return categoryCounts.entrySet().stream()
            .map(e -> new GymCategoryCountResponse(e.getKey(), idToName.getOrDefault(e.getKey(), "UNKNOWN"), e.getValue()))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<GymSubscriptionCountResponse> getGymCountBySubscription() {
        org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(GymReadService.class);
        logger.info("[getGymCountBySubscription] Start fetching gyms and aggregating by subscription.");
        List<Gym> gyms = gymRepository.findAll();
        logger.debug("[getGymCountBySubscription] Total gyms fetched: {}", gyms.size());
        java.util.Map<Long, java.util.Set<Long>> packageIdToGyms = new java.util.HashMap<>();
        for (Gym gym : gyms) {
            if (gym.getSubscriptions() != null) {
                logger.debug("[getGymCountBySubscription] Gym ID {} has {} subscriptions.", gym.getId(), gym.getSubscriptions().size());
                for (var subscription : gym.getSubscriptions()) {
                    Long packageId = subscription.getPackageId();
                    logger.debug("[getGymCountBySubscription] Gym ID {} subscription packageId: {}", gym.getId(), packageId);
                    if (packageId != null) {
                        packageIdToGyms.computeIfAbsent(packageId, k -> new java.util.HashSet<>()).add(gym.getId());
                    }
                }
            } else {
                logger.debug("[getGymCountBySubscription] Gym ID {} has no subscriptions.", gym.getId());
            }
        }
        logger.info("[getGymCountBySubscription] Aggregated packageIdToGyms: {}", packageIdToGyms);
        java.util.List<Long> packageIds = new java.util.ArrayList<>(packageIdToGyms.keySet());
        logger.info("[getGymCountBySubscription] Package IDs to fetch: {}", packageIds);
        java.util.List<az.fitnest.order.grpc.PackageNameInfo> packageNames = orderServiceGrpcClient.getPackageNamesByIds(packageIds);
        logger.info("[getGymCountBySubscription] Fetched packageNames: {}", packageNames);
        java.util.Map<Long, String> packageIdToName = new java.util.HashMap<>();
        for (az.fitnest.order.grpc.PackageNameInfo info : packageNames) {
            logger.debug("[getGymCountBySubscription] Mapping packageId {} to name {}", info.getPackageId(), info.getName());
            packageIdToName.put(info.getPackageId(), info.getName());
        }
        return packageIdToGyms.entrySet().stream()
            .map(e -> new GymSubscriptionCountResponse(e.getKey(), packageIdToName.getOrDefault(e.getKey(), "UNKNOWN"), (long) e.getValue().size()))
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
        return new double[]{minLat, maxLat, lng - deltaLng, lng + deltaLng};
    }

    public double calculateDistanceRaw(double lat1, double lng1, double lat2, double lng2) {
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lng2 - lng1);
        double a = Math.sin(latDistance / 2.0) * Math.sin(latDistance / 2.0) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(lonDistance / 2.0) * Math.sin(lonDistance / 2.0);
        return 6371.0 * 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
    }
    @Transactional(readOnly = true)
    public String getGymQrUrl(Long gymId) {
        String qrCodeUrl = gymRepository.findQrCodeUrlById(gymId);
        if (qrCodeUrl == null) {
            throw new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found");
        }
        return qrCodeUrl;
    }

    public GymEntranceResponse scanGymQrEntrance(Object principal, String qrCodeValue, Double lat, Double lng) {
        Long userId = extractUserId(principal);
        if (userId == null) {
            throw new IllegalArgumentException("Unauthorized");
        }
        Long gymId = extractGymIdFromQr(qrCodeValue);
        if (gymId == null) {
            throw new IllegalArgumentException("Invalid QR code");
        }
        GymEntranceResponse response = checkProximity(lat, lng, gymId);
        if (!response.allowed()) {
            throw new IllegalStateException("Not allowed");
        }
        return response;
    }

    public GymEntranceResponse checkGymEntranceEligibility(Object principal) {
        org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(GymReadService.class);
        Long userId = extractUserId(principal);
        logger.info("[checkGymEntranceEligibility] Checking eligibility for userId={}", userId);
        if (userId == null) {
            logger.warn("[checkGymEntranceEligibility] Unauthorized: principal is null or invalid");
            throw new IllegalArgumentException("Unauthorized");
        }
        az.fitnest.order.grpc.ActiveSubscriptionResponse subResp = null;
        try {
            subResp = orderServiceGrpcClient.getActiveSubscription(userId);
            logger.info("[checkGymEntranceEligibility] Received ActiveSubscriptionResponse: {}", subResp);
        } catch (Exception e) {
            logger.error("[checkGymEntranceEligibility] Error fetching subscription for userId={}: {}", userId, e.getMessage(), e);
            return GymEntranceResponse.builder()
                .allowed(false)
                .build();
        }
        String status = subResp.getSubscriptionStatus();
        logger.debug("[checkGymEntranceEligibility] Subscription status for userId={}: {}", userId, status);
        if (status == null || status.isEmpty() || status.equalsIgnoreCase("none")) {
            logger.info("[checkGymEntranceEligibility] No active subscription found for userId={}", userId);
            return GymEntranceResponse.builder()
                .allowed(false)
                .build();
        }
        if (!status.equalsIgnoreCase("active")) {
            logger.info("[checkGymEntranceEligibility] Subscription not active for userId={}, status={}", userId, status);
            return GymEntranceResponse.builder()
                .allowed(false)
                .build();
        }
        int visitLimitRemaining = subResp.getRemainingLimit();
        logger.debug("[checkGymEntranceEligibility] Remaining visit limit for userId={}: {}", userId, visitLimitRemaining);
        if (visitLimitRemaining <= 0) {
            logger.info("[checkGymEntranceEligibility] No visits left for userId={}", userId);
            return GymEntranceResponse.builder()
                .allowed(false)
                .build();
        }
        logger.info("[checkGymEntranceEligibility] Eligibility check PASSED for userId={}, remainingLimit={}", userId, visitLimitRemaining);
        return GymEntranceResponse.builder()
            .allowed(true)
            .build();
    }

    private Long extractUserId(Object principal) {
        if (principal instanceof Long) {
            return (Long) principal;
        }
        return null;
    }
    private Long extractGymIdFromQr(String qrCodeValue) {
        try {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("/gym/(\\d+)");
            java.util.regex.Matcher matcher = pattern.matcher(qrCodeValue);
            if (matcher.find()) {
                return Long.parseLong(matcher.group(1));
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
