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
import az.fitnest.catalog.service.TranslationService;
import az.fitnest.catalog.util.PlatformUtil;
import az.fitnest.catalog.util.UserContext;
import az.fitnest.catalog.client.OrderServiceGrpcClient;
import az.fitnest.catalog.client.UserServiceGrpcClient;
import az.fitnest.catalog.model.entity.Reservation;
import az.fitnest.catalog.model.enums.ReservationStatus;
import az.fitnest.user.grpc.UserResponse;
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
    private final GymEntranceHistoryRepository gymEntranceHistoryRepository;
    private final az.fitnest.catalog.repository.SupportedServiceRepository supportedServiceRepository;
    private final az.fitnest.catalog.repository.GymAdminRepository gymAdminRepository;
    private final az.fitnest.catalog.repository.GymLessonTypeRepository gymLessonTypeRepository;
    private final java.util.concurrent.Executor taskExecutor;

    private String resolveUserLanguage() {
        Long userId = UserContext.getCurrentUserId();
        return getUserLanguage(userId);
    }

    public List<az.fitnest.catalog.dto.response.SupportedServiceResponse> getAllSupportedServices(Long gymId) {
        java.util.List<az.fitnest.catalog.model.entity.SupportedService> combined = new java.util.ArrayList<>();
        combined.addAll(supportedServiceRepository.findAllByGymIdIsNull());
        if (gymId != null) {
            combined.addAll(supportedServiceRepository.findAllByGymId(gymId));
        }
        return combined.stream()
                .map(s -> new az.fitnest.catalog.dto.response.SupportedServiceResponse(s.getId(), s.getName(), s.getGymId()))
                .toList();
    }

    public String getUserLanguage(Long userId) {
        String language = "AZ";
        if (userId != null) {
            try {
                az.fitnest.user.grpc.UserResponse user = userServiceGrpcClient.getUserById(userId);
                if (user != null && user.getLanguage() != null && !user.getLanguage().isEmpty()) {
                    language = user.getLanguage().toUpperCase();
                }
            } catch (Exception ignored) {
            }
        }
        if (language.equals("AZ")) {
            String localeLang = org.springframework.context.i18n.LocaleContextHolder.getLocale().getLanguage()
                    .toUpperCase();
            if (localeLang.equals("EN") || localeLang.equals("RU")) {
                language = localeLang;
            }
        }
        return language;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "gym-detail", key = "#userId + '_' + #gymId + '_' + #root.target.getUserLanguage(#userId)")
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
                                        UserResponse user = userServiceGrpcClient.getUserById(r.getUserId());
                                        if (user != null) {
                                            fullName = user.getFirstName() + " " + user.getLastName();
                                            avatarUrl = user.getProfileImageUrl();
                                        }
                                    }
                                } catch (Exception e) {
                                }
                                return GymMapper.toReviewDto(r, fullName, avatarUrl);
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

        return candidates.stream()
                .filter(gym -> gym.getAddress() != null && gym.getAddress().getLatitude() != null
                        && gym.getAddress().getLongitude() != null)
                .map(gym -> {
                    String userLanguage = getUserLanguage(az.fitnest.catalog.util.UserContext.getCurrentUserId());
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
    public PaginatedResponse<GymMainPageResponse> getGyms(Long userId, String q, String type, Long categoryId,
            Long subscriptionId, int page, int pageSize, Double userLat, Double userLng, String sortDir) {
        if (categoryId != null && !categoryRepository.existsById(categoryId)) {
            throw new BadRequestException("INVALID_CATEGORY", "error.invalid_category");
        }
        Page<Gym> gymPage;
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = pageable(page, pageSize, Sort.by(direction, "createdDate"));

        if ("SAVED".equalsIgnoreCase(type)) {
            if (userId == null)
                return emptyPaginatedResponse(page, pageSize);
            List<SavedGym> saved = savedGymRepository.findByUserId(userId);
            List<Gym> candidates = saved.stream().map(SavedGym::getGym).toList();
            return manualPaginate(candidates, userId, userLat, userLng, page, pageSize, q, categoryId);
        }

        if (userLat != null && userLng != null) {
            Pageable distancePageable = PageRequest.of(Math.max(0, page - 1), pageSize);
            if (q != null && !q.isBlank()) {
                if (categoryId != null) {
                    gymPage = gymRepository.searchClosestWithCategory(q, categoryId, userLat, userLng,
                            distancePageable);
                } else {
                    gymPage = gymRepository.searchClosest(q, userLat, userLng, distancePageable);
                }
            } else if ("CLOSEST".equalsIgnoreCase(type) || type == null || type.isEmpty() || "ALL".equalsIgnoreCase(type)) {
                if (categoryId != null) {
                    gymPage = gymRepository.findByCategoryClosest(categoryId, userLat, userLng, distancePageable);
                } else {
                    gymPage = gymRepository.findAllClosest(userLat, userLng, distancePageable);
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
                    gymPage = gymRepository.findByNameOrDescriptionContainingIgnoreCaseAndCategory(q, categoryId,
                            pageable);
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

        List<Long> allPackageIds = gymPage.getContent().stream()
                .flatMap(g -> g.getSubscriptions() != null ? g.getSubscriptions().stream() : java.util.stream.Stream.empty())
                .map(az.fitnest.catalog.model.entity.GymSubscription::getPackageId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, az.fitnest.order.grpc.PackageNameInfo> globalPackageInfos = new java.util.HashMap<>();
        if (!allPackageIds.isEmpty()) {
            try {
                globalPackageInfos = orderServiceGrpcClient.getPackageNamesByIds(allPackageIds).stream()
                        .collect(Collectors.toMap(az.fitnest.order.grpc.PackageNameInfo::getPackageId, p -> p, (a, b) -> a));
            } catch (Exception e) {
            }
        }

        final Map<Long, az.fitnest.order.grpc.PackageNameInfo> finalPackageMap = globalPackageInfos;
        List<GymMainPageResponse> items = gymPage.getContent().stream()
                .map(gym -> mapToGymMainPageDto(gym, userId, userLat, userLng, finalSavedIds.contains(gym.getId()), finalPackageMap))
                .filter(gymDto -> {
                    if (subscriptionId == null)
                        return true;
                    Gym gym = gymPage.getContent().stream().filter(g -> g.getId().toString().equals(gymDto.gymId()))
                            .findFirst().orElse(null);
                    if (gym == null || gym.getSubscriptions() == null)
                        return false;
                    return gym.getSubscriptions().stream().anyMatch(sub -> subscriptionId.equals(sub.getPackageId()));
                })
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
    @Cacheable(value = "admin-gyms", key = "{#query, #sort, #page, #pageSize}")
    public PaginatedResponse<AdminGymResponse> getAllGymsAdmin(String query, String sort, int page, int pageSize) {
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

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Gym> gymPage = gymRepository.findAll(spec, pageable);

        List<Long> gymIds = gymPage.getContent().stream().map(Gym::getId).toList();
        Map<Long, String> ownerNames = gymAdminRepository.findAllByGymIdIn(gymIds).stream()
                .filter(admin -> "Super admin".equalsIgnoreCase(admin.getRole()))
                .collect(Collectors.groupingBy(admin -> admin.getGym().getId(),
                        Collectors.mapping(admin -> admin.getName() + " " + admin.getSurname(),
                                Collectors.joining(", "))));

        List<AdminGymResponse> items = gymPage.getContent().stream().map(gym -> {
        String ownerName = ownerNames.get(gym.getId());

            String fullAddress = (gym.getAddress() != null)
                    ? (gym.getAddress().getCity() + ", " + gym.getAddress().getAddressText())
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
            Double lng, int page, int pageSize, String q, Long categoryId) {
        java.util.stream.Stream<Gym> stream = candidates.stream();
        if (q != null && !q.isBlank()) {
            String lowerQ = q.toLowerCase();
            stream = stream.filter(g -> (g.getName() != null && g.getName().toLowerCase().contains(lowerQ)) ||
                    (g.getAddress() != null && g.getAddress().getAddressText() != null
                            && g.getAddress().getAddressText().toLowerCase().contains(lowerQ)));
        }
        if (categoryId != null) {
            stream = stream.filter(g -> g.getCategory() != null && g.getCategory().getId().equals(categoryId));
        }

        List<GymMainPageResponse> all = stream.map(g -> mapToGymMainPageDto(g, userId, lat, lng, true, java.util.Collections.emptyMap()))
                .collect(Collectors.toList());

        if (lat != null && lng != null) {
            all.sort(Comparator.comparing(GymMainPageResponse::distanceKm,
                    Comparator.nullsLast(Comparator.naturalOrder())));
        }

        int from = Math.max(0, (page - 1) * pageSize);
        int to = Math.min(all.size(), from + pageSize);
        List<GymMainPageResponse> pageItems = from >= all.size() ? new java.util.ArrayList<>()
                : new java.util.ArrayList<>(all.subList(from, to));

        return PaginatedResponse.<GymMainPageResponse>builder()
                .items(pageItems)
                .total(all.size())
                .page(page)
                .pageSize(pageSize)
                .build();
    }

    private GymMainPageResponse mapToGymMainPageDto(Gym gym, Long userId, Double userLat, Double userLng,
            boolean isSaved, Map<Long, az.fitnest.order.grpc.PackageNameInfo> packageInfoMap) {
        double stars = gym.getRating() != null ? gym.getRating() : 0.0;
        boolean isNew = gym.getCreatedDate() != null
                && gym.getCreatedDate().isAfter(LocalDateTime.now().minusMonths(1L));
        Address address = gym.getAddress();
        Double distanceKm = null;
        if (userLat != null && userLng != null && address != null && address.getLatitude() != null
                && address.getLongitude() != null) {
            distanceKm = Math
                    .round(calculateDistanceRaw(userLat, userLng, address.getLatitude(), address.getLongitude()) * 10.0)
                    / 10.0;
        }

        String userLanguage = getUserLanguage(userId);
        CategoryResponse category = null;
        if (gym.getCategory() != null) {
            Category c = gym.getCategory();
            String localizedCatName = translationService.getTranslatedValue("CATEGORY",
                    c.getCategoryId().toString(), "name", userLanguage);
            category = CategoryResponse.builder()
                    .id(c.getCategoryId())
                    .name(localizedCatName != null && !localizedCatName.isEmpty() ? localizedCatName : c.getName())
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
                        String localizedPackageName = translationService.getTranslatedValue("GYMSUBSCRIPTION",
                                planId, "name", userLanguage);
                        String packageName = cleanPackageName((localizedPackageName != null && !localizedPackageName.isEmpty())
                                ? localizedPackageName
                                : (info != null ? info.getName() : "Bronze"));
                        List<GymPlanBenefitResponse> benefitsList = sub.getSupportedServices().stream()
                                .map(b -> {
                                    String localizedBenefit = translationService.getTranslatedValue(
                                            "SUPPORTEDSERVICE", b.getId().toString(), "name", userLanguage);
                                    return GymPlanBenefitResponse.builder()
                                            .description(localizedBenefit != null && !localizedBenefit.isEmpty()
                                                    ? localizedBenefit
                                                    : b.getName())
                                            .build();
                                })
                                .toList();
                        return GymPlanItemResponse.builder()
                                .plan_id(planId)
                                .packageName(packageName)
                                .dailyPrice(sub.getDailyPrice())
                                .benefits(benefitsList)
                                .build();
                    }).toList();
        }
        String localizedName = getLocalizedGymName(gym, userLanguage);
        return GymMainPageResponse.builder()
                .gymId(gym.getId().toString())
                .name(localizedName)
                .coverImageUrl(gym.getCoverImageUrl())
                .stars(stars)
                .isNew(isNew)
                .location(address != null
                        ? getLocalizedAddressField(gym.getId(), "GYM", address, "addressText", userLanguage)
                        : null)
                .city(address != null ? getLocalizedAddressField(gym.getId(), "GYM", address, "city", userLanguage)
                        : null)
                .distanceKm(distanceKm)
                .isSaved(isSaved)
                .category(category)
                .supportedSubscriptions(supportedSubscriptions)
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
        List<Gym> gyms = gymRepository.findAll();
        if (categoryId != null) {
            gyms = gyms.stream()
                    .filter(g -> g.getCategory() != null && g.getCategory().getId().equals(categoryId))
                    .toList();
        }
        if (subscriptionId != null) {
            gyms = gyms.stream()
                    .filter(g -> g.getSubscriptions() != null)
                    .toList();
        }
        if (type != null && type.equalsIgnoreCase("new")) {
            gyms = gyms.stream()
                    .filter(g -> g.getCreatedDate() != null
                            && g.getCreatedDate().isAfter(java.time.LocalDateTime.now().minusWeeks(1)))
                    .toList();
        }
        return new GymCountResponse(
                gyms.stream()
                        .filter(g -> g.getSubscriptions() != null)
                        .count(),
                type != null ? type : "all",
                subscriptionId,
                categoryId);
    }

    @Transactional(readOnly = true)
    public GymTypeCountResponse getGymCountByType(String type) {
        List<Gym> gyms = gymRepository.findAll();
        long count;
        if (type.equalsIgnoreCase("new")) {
            count = gyms.stream()
                    .filter(g -> g.getCreatedDate() != null
                            && g.getCreatedDate().isAfter(java.time.LocalDateTime.now().minusWeeks(1)))
                    .count();
        } else {
            count = gyms.size();
        }
        return new GymTypeCountResponse(type, count);
    }

    @Transactional(readOnly = true)
    public List<GymCategoryCountResponse> getGymCountByCategory() {
        Long userId = az.fitnest.catalog.util.UserContext.getCurrentUserId();
        String language = getUserLanguage(userId);

        List<Gym> gyms = gymRepository.findAll();
        java.util.Map<Long, Long> categoryCounts = gyms.stream()
                .map(gym -> gym.getCategory())
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.groupingBy(
                        category -> category.getCategoryId(),
                        java.util.stream.Collectors.counting()));
        List<Category> allCategories = categoryRepository.findAllById(categoryCounts.keySet());
        java.util.Map<Long, Category> idToCategory = allCategories.stream()
                .collect(java.util.stream.Collectors.toMap(Category::getCategoryId, c -> c));
        return categoryCounts.entrySet().stream()
                .map(e -> {
                    Category cat = idToCategory.get(e.getKey());
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
                            e.getKey(),
                            catName,
                            iconUrl,
                            e.getValue());
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<GymSubscriptionCountResponse> getGymCountBySubscription() {
        List<Gym> gyms = gymRepository.findAll();
        java.util.Map<Long, java.util.Set<Long>> packageIdToGyms = new java.util.HashMap<>();
        for (Gym gym : gyms) {
            if (gym.getSubscriptions() != null) {
                for (var subscription : gym.getSubscriptions()) {
                    Long packageId = subscription.getPackageId();
                    if (packageId != null) {
                        packageIdToGyms.computeIfAbsent(packageId, k -> new java.util.HashSet<>()).add(gym.getId());
                    }
                }
            } else {
            }
        }
        java.util.List<Long> packageIds = new java.util.ArrayList<>(packageIdToGyms.keySet());
        java.util.List<az.fitnest.order.grpc.PackageNameInfo> packageNames = orderServiceGrpcClient
                .getPackageNamesByIds(packageIds);
        java.util.Map<Long, String> packageIdToName = new java.util.HashMap<>();
        for (az.fitnest.order.grpc.PackageNameInfo info : packageNames) {
            packageIdToName.put(info.getPackageId(), info.getName());
        }
        return packageIdToGyms.entrySet().stream()
                .map(e -> new GymSubscriptionCountResponse(e.getKey(),
                        packageIdToName.getOrDefault(e.getKey(), "UNKNOWN"), (long) e.getValue().size()))
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

    @Transactional(readOnly = true)
    public String getGymQrUrl(Long gymId) {
        String qrCodeUrl = gymRepository.findQrCodeUrlById(gymId);
        if (qrCodeUrl == null) {
            throw new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found");
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
        } catch (Exception e) {
            allowed = false;
            reason = "NO_ACTIVE_SUBSCRIPTION";
        }

        if (allowed) {
            String gender = null;
            try {
                az.fitnest.user.grpc.UserResponse userResp = userServiceGrpcClient.getUserById(userId);
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
                orderServiceGrpcClient.checkIn(userId, gymId);
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
            try {
                az.fitnest.user.grpc.UserResponse user = userServiceGrpcClient.getUserById(h.getUserId());
                if (user != null) {
                    firstName = user.getFirstName();
                    lastName = user.getLastName();
                    phone = user.getMobile();
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
                    .build();
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public az.fitnest.catalog.dto.response.GymAnalyticsResponse getGymAnalytics(Long gymId,
            java.time.LocalDateTime startDate, java.time.LocalDateTime endDate, String statusFilter, String sort,
            int page, int pageSize) {
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
            try {
                az.fitnest.user.grpc.UserResponse user = userServiceGrpcClient.getUserById(h.getUserId());
                if (user != null) {
                    firstName = user.getFirstName();
                    lastName = user.getLastName();
                    phone = user.getMobile();
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
        java.time.DayOfWeek day = now.getDayOfWeek();
        az.fitnest.catalog.model.enums.GymWorkHourPeriod period = az.fitnest.catalog.model.enums.GymWorkHourPeriod
                .valueOf(day.name());

        java.time.LocalTime currentTime = now.toLocalTime();

        boolean allowedInGeneral = gym.getGeneralWorkHours() != null && gym.getGeneralWorkHours().stream()
                .filter(h -> h.getPeriod() == period)
                .anyMatch(h -> (h.getFromTime() == null || !currentTime.isBefore(h.getFromTime())) &&
                        (h.getToTime() == null || !currentTime.isAfter(h.getToTime())));

        if (allowedInGeneral)
            return true;

        if ("MALE".equalsIgnoreCase(gender)) {
            return gym.getWorkHoursMan() != null && gym.getWorkHoursMan().stream()
                    .filter(h -> h.getPeriod() == period)
                    .anyMatch(h -> (h.getFromTime() == null || !currentTime.isBefore(h.getFromTime())) &&
                            (h.getToTime() == null || !currentTime.isAfter(h.getToTime())));
        } else if ("FEMALE".equalsIgnoreCase(gender)) {
            return gym.getWorkHoursWoman() != null && gym.getWorkHoursWoman().stream()
                    .filter(h -> h.getPeriod() == period)
                    .anyMatch(h -> (h.getFromTime() == null || !currentTime.isBefore(h.getFromTime())) &&
                            (h.getToTime() == null || !currentTime.isAfter(h.getToTime())));
        }

        return false;
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
        List<Gym> gyms = gymRepository.findAll();
        long count;
        if (gender == null) {
            throw new BadRequestException("GENDER_REQUIRED", "error.gender_required");
        }
        switch (gender.toLowerCase()) {
            case "female":
                count = gyms.stream().filter(g -> g.getWorkHoursWoman() != null && !g.getWorkHoursWoman().isEmpty())
                        .count();
                break;
            case "male":
                count = gyms.stream().filter(g -> g.getWorkHoursMan() != null && !g.getWorkHoursMan().isEmpty())
                        .count();
                break;
            default:
                throw new BadRequestException("INVALID_GENDER", "error.invalid_gender");
        }
        return new GymTypeCountResponse(gender, count);
    }

    private String getLocalizedAddressField(Long entityId, String entityType,
            az.fitnest.catalog.model.entity.Address address, String fieldName, String userLanguage) {
        if (address == null)
            return null;
        String localized = translationService.getTranslatedValue(entityType, entityId.toString(), fieldName,
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

    private String getLocalizedGymName(Gym gym, String userLanguage) {
        if (gym == null)
            return null;
        String translated = translationService.getTranslatedValue("GYM", gym.getId().toString(), "name", userLanguage);
        return (translated != null && !translated.isEmpty()) ? translated : gym.getName();
    }

    @Override
    @Transactional(readOnly = true)
    public az.fitnest.catalog.dto.response.GymInfoAdminResponse getGymDetailsAdmin(Long gymId) {
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

        String localizedGymName = translationService.getTranslatedValue("GYM", gym.getId().toString(), "name", userLanguage);
        if (localizedGymName == null || localizedGymName.isEmpty()) {
            localizedGymName = gym.getName();
        }

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
                        UserResponse user = userServiceGrpcClient.getUserById(r.getUserId());
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

        UserResponse user = null;
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
        long total = reservationRepository.countByGymId(gymId);
        long pending = reservationRepository.countByGymIdAndStatus(gymId, ReservationStatus.PENDING);
        long confirmed = reservationRepository.countByGymIdAndStatus(gymId, ReservationStatus.APPROVED);
        long cancelled = reservationRepository.countByGymIdAndStatus(gymId, ReservationStatus.CANCELLED);

        return new ReservationStatsResponse(total, pending, confirmed, cancelled);
    }

    @Override
    @Transactional(readOnly = true)
    public List<az.fitnest.catalog.dto.response.LessonHourResponse> getGymLessonHoursAdmin(Long gymId) {
        return trainerReservationDateRepository.findByGymId(gymId).stream()
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
    }
    private String cleanPackageName(String name) {
        if (name == null) return null;
        return name.replace(" Plan", "").replace(" plan", "").trim();
    }
}
