package az.fitnest.catalog.service.impl;

import az.fitnest.catalog.dto.*;
import az.fitnest.catalog.mapper.GymMapper;
import az.fitnest.catalog.exception.ResourceNotFoundException;
import az.fitnest.catalog.model.entity.Address;
import az.fitnest.catalog.model.entity.Gym;
import az.fitnest.catalog.model.entity.GymImage;
import az.fitnest.catalog.model.entity.SavedGym;
import az.fitnest.catalog.model.entity.Trainer;
import az.fitnest.catalog.repository.GymImageRepository;
import az.fitnest.catalog.repository.GymRepository;
import az.fitnest.catalog.repository.ReviewRepository;
import az.fitnest.catalog.repository.TrainerRepository;
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

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "gyms", key = "#gymId")
    public GymDetailResponse getGymDetail(Long userId, Long gymId) {
        Gym gym = gymRepository.findWithDetailsById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));

        boolean isSaved = false;
        if (userId != null) {
            isSaved = savedGymRepository.findByUserIdAndGymId(userId, gymId).isPresent();
        }

        List<GymRoomDto> rooms = new java.util.ArrayList<>();
        if (gym.getRooms() != null) {
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

        List<GymPlanItemDto> membershipPlans = new java.util.ArrayList<>();
        try {
            if (gym.getSubscriptions() != null && !gym.getSubscriptions().isEmpty()) {
                List<Long> planIds = gym.getSubscriptions().stream()
                        .map(az.fitnest.catalog.model.entity.GymSubscription::getPlanId)
                        .distinct()
                        .toList();
                List<az.fitnest.order.grpc.GymMembershipPlan> remotePlans = orderServiceGrpcClient.getPlansByIds(planIds);

                if (remotePlans != null && !remotePlans.isEmpty()) {
                    membershipPlans = remotePlans.stream().map(remotePlan -> {
                        // Find matching subscription to get benefits
                        az.fitnest.catalog.model.entity.GymSubscription matchingSub = gym.getSubscriptions().stream()
                                .filter(s -> s.getPlanId() == remotePlan.getPlanId())
                                .findFirst().orElse(null);

                        List<String> benefitsList = new java.util.ArrayList<>();
                        if (matchingSub != null && matchingSub.getBenefits() != null) {
                            benefitsList = matchingSub.getBenefits().stream()
                                    .map(az.fitnest.catalog.model.entity.GymSubscriptionBenefit::getBenefit)
                                    .toList();
                        }

                        return GymPlanItemDto.builder()
                                .plan_id(String.valueOf(remotePlan.getPlanId()))
                                .name(remotePlan.getName())
                                .benefits(benefitsList)
                                .build();
                    }).collect(java.util.stream.Collectors.toList());
                } else {
                    // Fallback to local data with placeholder names if gRPC returns nothing
                    membershipPlans = gym.getSubscriptions().stream().map(sub -> {
                        String placeholderName = switch (sub.getPlanId().intValue()) {
                            case 1 -> "Bronze Plan";
                            case 2 -> "Silver Plan";
                            case 3 -> "Gold Plan";
                            case 4 -> "Platinum Plan";
                            default -> "Standard Plan";
                        };
                        return GymPlanItemDto.builder()
                                .plan_id(String.valueOf(sub.getPlanId()))
                                .name(placeholderName)
                                .benefits(sub.getBenefits().stream().map(az.fitnest.catalog.model.entity.GymSubscriptionBenefit::getBenefit).toList())
                                .build();
                    }).toList();
                }
            }
        } catch (Exception e) {
            // Log error and fallback to local data
            System.err.println("Could not fetch membership plans from order-service: " + e.getMessage());
            if (gym.getSubscriptions() != null) {
                membershipPlans = gym.getSubscriptions().stream().map(sub -> {
                    String placeholderName = switch (sub.getPlanId().intValue()) {
                        case 1 -> "Bronze Plan";
                        case 2 -> "Silver Plan";
                        case 3 -> "Gold Plan";
                        case 4 -> "Platinum Plan";
                        default -> "Standard Plan";
                    };
                    return GymPlanItemDto.builder()
                            .plan_id(String.valueOf(sub.getPlanId()))
                            .name(placeholderName)
                            .benefits(sub.getBenefits().stream().map(az.fitnest.catalog.model.entity.GymSubscriptionBenefit::getBenefit).toList())
                            .build();
                }).toList();
            }
        }

        List<GymTrainerDto> trainerDtos = trainerRepository.findByGymId(gymId, PageRequest.of(0, 5, Sort.by("id")))
                .getContent().stream()
                .map(GymMapper::toTrainerDto)
                .collect(Collectors.toList());

        List<GymReviewDto> recentReviews = reviewRepository.findByGymId(gymId, PageRequest.of(0, 3, Sort.by(Sort.Direction.DESC, "createdDate")))
                .getContent().stream()
                .map(GymMapper::toReviewDto)
                .collect(Collectors.toList());

        List<GymWorkHourDto> workHours = gymRepository.findWorkHoursByGymId(gymId).stream()
                .map(GymMapper::toWorkHourDto)
                .collect(Collectors.toList());

        List<GymWorkHourDto> workHoursWoman = gym.getWorkHoursWoman() != null ? gym.getWorkHoursWoman().stream()
                .map(GymMapper::toWorkHourDto)
                .collect(Collectors.toList()) : null;

        List<GymWorkHourDto> workHoursMan = gym.getWorkHoursMan() != null ? gym.getWorkHoursMan().stream()
                .map(GymMapper::toWorkHourDto)
                .collect(Collectors.toList()) : null;

        List<CategoryDto> categoryDtos = gym.getCategories() != null ? gym.getCategories().stream()
                .map(GymMapper::toCategoryDto)
                .collect(Collectors.toList()) : null;

        return GymDetailResponse.builder()
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
                .work_hours(workHours)
                .work_hours_woman(workHoursWoman)
                .work_hours_man(workHoursMan)
                .rooms(rooms)
                .membership_plans(membershipPlans)
                .trainers(trainerDtos)
                .recent_reviews(recentReviews)
                .categories(categoryDtos)
                .coverImageUrl(gym.getCoverImageUrl())
                .rating(gym.getRating())
                .reviewsCount(gym.getReviewsCount())
                .qr_code_url(gym.getQrCodeUrl())
                .status(gym.getStatus())
                .build();
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "gymImages", key = "#gymId")
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
    public PaginatedResponse<GymMainPageDto> getClosestGyms(Long userId, int page, int pageSize, Double userLat, Double userLng) {
        return getGyms(userId, null, "CLOSEST", page, pageSize, userLat, userLng, "desc");
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<GymMainPageDto> getGyms(Long userId, String q, String type, int page, int pageSize, Double userLat, Double userLng, String sortDir) {
        Page<Gym> gymPage;
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = pageable(page, pageSize, Sort.by(direction, "createdDate"));

        if ("SAVED".equalsIgnoreCase(type)) {
            if (userId == null) return emptyPaginatedResponse(page, pageSize);
            List<SavedGym> saved = savedGymRepository.findByUserId(userId);
            List<Gym> candidates = saved.stream().map(SavedGym::getGym).toList();
            return manualPaginate(candidates, userId, userLat, userLng, page, pageSize, q);
        }

        if (userLat != null && userLng != null) {
            double initialRadiusKm = 50.0;
            double[] bbox = boundingBox(userLat, userLng, initialRadiusKm);
            if (q != null && !q.isBlank()) {
                gymPage = gymRepository.findClosestGymsWithQuery(q, bbox[0], bbox[1], bbox[2], bbox[3], userLat, userLng, pageable);
            } else if ("CLOSEST".equalsIgnoreCase(type)) {
                gymPage = gymRepository.findClosestGyms(bbox[0], bbox[1], bbox[2], bbox[3], userLat, userLng, pageable);
            } else {
                gymPage = gymRepository.findAll(pageable);
            }
        } else {
            if (q != null && !q.isBlank()) {
                gymPage = gymRepository.findByNameOrDescriptionContainingIgnoreCase(q, pageable);
            } else {
                gymPage = gymRepository.findAll(pageable);
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

    private PaginatedResponse<GymMainPageDto> manualPaginate(List<Gym> candidates, Long userId, Double lat, Double lng, int page, int pageSize, String q) {
        java.util.stream.Stream<Gym> stream = candidates.stream();
        if (q != null && !q.isBlank()) {
            String lowerQ = q.toLowerCase();
            stream = stream.filter(g -> (g.getName() != null && g.getName().toLowerCase().contains(lowerQ)) ||
                    (g.getAddress() != null && g.getAddress().getAddressText() != null && g.getAddress().getAddressText().toLowerCase().contains(lowerQ)));
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

    private double calculateDistanceRaw(double lat1, double lng1, double lat2, double lng2) {
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lng2 - lng1);
        double a = Math.sin(latDistance / 2.0) * Math.sin(latDistance / 2.0) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(lonDistance / 2.0) * Math.sin(lonDistance / 2.0);
        return 6371.0 * 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
    }
}
