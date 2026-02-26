package az.fitnest.catalog.service.impl;

import az.fitnest.catalog.dto.*;
import az.fitnest.catalog.exception.ResourceNotFoundException;
import az.fitnest.catalog.model.entity.Address;
import az.fitnest.catalog.model.entity.Gym;
import az.fitnest.catalog.model.entity.GymImage;
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
    private final GymImageRepository gymImageRepository;
    private final TrainerRepository trainerRepository;
    private final ReviewRepository reviewRepository;
    private final OrderServiceGrpcClient orderServiceGrpcClient;

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "gyms", key = "#gymId")
    public GymDetailResponse getGymDetail(Long userId, Long gymId) {
        Gym gym = gymRepository.findWithDetailsById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "Gym not found"));

        Map<String, List<GymImage>> grouped = gymImageRepository.findByGymId(gymId)
                .stream()
                .filter(img -> img.getImageName() != null)
                .collect(Collectors.groupingBy(GymImage::getImageName));

        List<GymRoomDto> rooms = grouped.entrySet().stream().map(entry -> {
            List<GymImageDto> images = entry.getValue().stream().map(this::toGymImageDto).collect(Collectors.toList());
            return GymRoomDto.builder().room_name(entry.getKey()).images(images).build();
        }).collect(Collectors.toList());

        List<GymPlanItemDto> membershipPlans = new java.util.ArrayList<>();
        try {
            if (gym.getSubscriptions() != null && !gym.getSubscriptions().isEmpty()) {
                List<Long> planIds = gym.getSubscriptions().stream()
                        .map(az.fitnest.catalog.model.entity.GymSubscription::getPlanId)
                        .distinct()
                        .toList();
                List<az.fitnest.order.grpc.GymMembershipPlan> remotePlans = orderServiceGrpcClient.getPlansByIds(planIds);
                
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
            }
        } catch (Exception e) {
            // Log error or ignore if unavailable (e.g. StatusRuntimeException)
            System.err.println("Could not fetch membership plans from order-service: " + e.getMessage());
        }

        List<GymTrainerDto> trainerDtos = trainerRepository.findByGymId(gymId, PageRequest.of(0, 5, Sort.by("id")))
                .getContent().stream()
                .map(this::toGymTrainerDto)
                .collect(Collectors.toList());

        List<GymReviewDto> recentReviews = reviewRepository.findByGymId(gymId, PageRequest.of(0, 3, Sort.by(Sort.Direction.DESC, "createdDate")))
                .getContent().stream()
                .map(this::toGymReviewDto)
                .collect(Collectors.toList());

        List<GymWorkHourDto> workHours = gymRepository.findWorkHoursByGymId(gymId).stream()
                .map(wh -> GymWorkHourDto.builder()
                        .day(wh.getDay())
                        .from(wh.getFromTime())
                        .to(wh.getToTime())
                        .build())
                .collect(Collectors.toList());

        return GymDetailResponse.builder()
                .gym_id(gym.getId().toString())
                .name(gym.getName())
                .description(gym.getDescription())
                .isSaved(false)
                .address(gym.getAddress() != null ? az.fitnest.catalog.dto.LocationDto.builder()
                        .addressText(gym.getAddress().getAddressText())
                        .city(gym.getAddress().getCity())
                        .latitude(gym.getAddress().getLatitude())
                        .longitude(gym.getAddress().getLongitude())
                        .build() : null)
                .phone(gym.getPhone())
                .email(gym.getEmail())
                .work_hours(workHours)
                .rooms(rooms)
                .membership_plans(membershipPlans)
                .trainers(trainerDtos)
                .recent_reviews(recentReviews)
                .qr_code_url(gym.getQrCodeUrl())
                .responsiblePerson(gym.getResponsiblePerson())
                .status(gym.getStatus())
                .build();
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "gymImages", key = "#gymId")
    public GymImageResponse getGymImages(Long gymId) {
        if (!gymRepository.existsById(gymId)) {
            throw new ResourceNotFoundException("GYM_NOT_FOUND", "Gym not found");
        }
        List<GymImageItemDto> items = gymImageRepository.findByGymId(gymId).stream()
                .map(this::toGymImageItemDto)
                .collect(Collectors.toList());
        return GymImageResponse.builder().items(items).build();
    }



    @Transactional(readOnly = true)
    public ReservationRulesResponse getReservationRules(Long gymId) {
        if (!gymRepository.existsById(gymId)) {
            throw new ResourceNotFoundException("GYM_NOT_FOUND", "Gym not found");
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
    public PaginatedResponse<GymMainPageDto> getClosestGyms(String q, int page, int pageSize, Double userLat, Double userLng) {
        Page<Gym> gymPage;
        Pageable pageable = pageable(page, pageSize, Sort.unsorted());
        
        if (userLat != null && userLng != null) {
            double initialRadiusKm = 50.0;
            double[] bbox = boundingBox(userLat, userLng, initialRadiusKm);
            if (q != null && !q.isBlank()) {
                gymPage = gymRepository.findClosestGymsWithQuery(q, bbox[0], bbox[1], bbox[2], bbox[3], userLat, userLng, pageable);
            } else {
                gymPage = gymRepository.findClosestGyms(bbox[0], bbox[1], bbox[2], bbox[3], userLat, userLng, pageable);
            }
        } else {
            if (q != null && !q.isBlank()) {
                gymPage = gymRepository.findByNameOrDescriptionContainingIgnoreCase(q, pageable);
            } else {
                gymPage = gymRepository.findAll(pageable);
            }
        }

        List<GymMainPageDto> items = gymPage.getContent().stream().map(gym -> {
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
                    .build();
        }).collect(Collectors.toList());

        String message = null;
        if (items.isEmpty()) {
            message = "There are no gyms close to your location.";
        }
        return PaginatedResponse.<GymMainPageDto>builder()
                .items(items)
                .total(gymPage.getTotalElements())
                .page(page)
                .pageSize(pageSize)
                .message(message)
                .build();
    }
    
    @Transactional(readOnly = true)
    public LocationDto getGymLocation(Long gymId) {
        Gym gym = gymRepository.findById(gymId).orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "Gym not found"));
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

    private GymTrainerDto toGymTrainerDto(Trainer t) {
        az.fitnest.catalog.dto.ProfessionDto professionDto = null;
        if (t.getProfession() != null) {
            professionDto = az.fitnest.catalog.dto.ProfessionDto.builder()
                    .id(t.getProfession().getId())
                    .name(t.getProfession().getName())
                    .build();
        }
        
        return GymTrainerDto.builder()
                .trainer_id(t.getId() != null ? t.getId().toString() : null)
                .name(t.getName())
                .surname(t.getSurname())
                .profession(professionDto)
                .picture(t.getPicture())
                .phone(t.getPhone())
                .email(t.getEmail())
                .build();
    }

    private GymReviewDto toGymReviewDto(az.fitnest.catalog.model.entity.Review r) {
        return GymReviewDto.builder()
                .review_id(r.getId() != null ? r.getId().toString() : null)
                .rating(r.getRating())
                .comment(r.getComment())
                .created_at(r.getCreatedDate())
                .author(GymReviewAuthorDto.builder()
                        .user_id(r.getUserId() != null ? r.getUserId().toString() : null)
                        .full_name("User " + r.getUserId())
                        .build())
                .build();
    }

    private GymImageDto toGymImageDto(GymImage img) {
        return GymImageDto.builder()
                .id(img.getId())
                .gymId(img.getGym() != null ? img.getGym().getId() : null)
                .name(img.getImageName())
                .url(img.getUrl())
                .build();
    }

    private GymImageItemDto toGymImageItemDto(GymImage img) {
        return GymImageItemDto.builder()
                .image_id(img.getId() != null ? img.getId().toString() : "img_" + System.identityHashCode(img))
                .url(img.getUrl())
                .type(img.getType() != null ? img.getType() : "other")
                .title(img.getTitle())
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
