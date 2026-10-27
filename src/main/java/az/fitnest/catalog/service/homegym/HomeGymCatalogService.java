package az.fitnest.catalog.service.homegym;

import az.fitnest.catalog.client.OrderServiceGrpcClient;
import az.fitnest.catalog.dto.PaginatedResponse;
import az.fitnest.catalog.dto.response.HomeGymGymResponse;
import az.fitnest.catalog.exception.ResourceNotFoundException;
import az.fitnest.catalog.model.entity.Address;
import az.fitnest.catalog.model.entity.Gym;
import az.fitnest.catalog.repository.GymRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Catalog-side list of gyms the subscriber may pick as home gym (spec R1).
 * Package access comes from order-backend; time-slot restrictions and the
 * R18 admin switch are resolved from catalog tables.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeGymCatalogService {

    private final GymRepository gymRepository;
    private final HomeGymRuleService ruleService;
    private final OrderServiceGrpcClient orderServiceGrpcClient;

    public PaginatedResponse<HomeGymGymResponse> getGyms(Long userId, Long subscriptionId, String q,
                                                         int page, int pageSize,
                                                         Double userLat, Double userLng) {
        int safePage = Math.max(1, page);
        int safeSize = Math.min(Math.max(1, pageSize), 100);

        az.fitnest.order.grpc.GetHomeGymContextResponse context =
                orderServiceGrpcClient.getHomeGymContext(userId, subscriptionId);
        if (!context.getFound()) {
            throw new ResourceNotFoundException("SUBSCRIPTION_NOT_FOUND", "error.home_gym.subscription_not_found");
        }
        Long packageId = context.getPackageId() > 0 ? context.getPackageId() : null;
        HomeGymRuleService.PackageContext packageContext = ruleService.packageContext(packageId);
        List<Long> packageIds = ruleService.eligiblePackageIds(packageId);
        String searchKey = q != null && !q.isBlank() ? q.trim() : null;

        Pageable pageable = PageRequest.of(safePage - 1, safeSize);
        Page<Long> idPage;
        if (userLat != null && userLng != null) {
            idPage = gymRepository.findClosestHomeGymIds(searchKey, packageIds, userLat, userLng, pageable);
        } else {
            idPage = gymRepository.findHomeGymIds(searchKey, packageIds, pageable);
        }

        List<HomeGymGymResponse> items = new ArrayList<>();
        if (!idPage.isEmpty()) {
            List<Long> ids = idPage.getContent();
            Map<Long, Gym> gyms = gymRepository.findWithListDetailsByIdIn(ids).stream()
                    .collect(Collectors.toMap(Gym::getId, gym -> gym, (first, second) -> first));
            for (Long id : ids) {
                Gym gym = gyms.get(id);
                if (gym == null || !ruleService.isHomeEligible(gym, packageId)) {
                    continue;
                }
                items.add(toResponse(gym, packageContext, userLat, userLng));
            }
        }

        return PaginatedResponse.<HomeGymGymResponse>builder()
                .items(items)
                .total(idPage.getTotalElements())
                .page(safePage)
                .pageSize(safeSize)
                .build();
    }

    private HomeGymGymResponse toResponse(Gym gym, HomeGymRuleService.PackageContext packageContext,
                                          Double userLat, Double userLng) {
        Address address = gym.getAddress();
        String city = null;
        String location = null;
        Double latitude = null;
        Double longitude = null;
        Double distanceKm = null;
        if (address != null) {
            city = az.fitnest.catalog.util.AzerbaijanLocations.canonical(address.getCity());
            if (city == null || city.isEmpty()) {
                city = address.getCity();
            }
            location = az.fitnest.catalog.util.AzerbaijanLocations.repairMojibake(address.getAddressText());
            latitude = address.getLatitude();
            longitude = address.getLongitude();
            if (userLat != null && userLng != null && latitude != null && longitude != null) {
                distanceKm = Math.round(distance(userLat, userLng, latitude, longitude) * 10.0) / 10.0;
            }
        }
        return HomeGymGymResponse.builder()
                .gymId(gym.getId().toString())
                .name(gym.getName())
                .coverImageUrl(gym.getCoverImageUrl())
                .city(city)
                .location(location)
                .latitude(latitude)
                .longitude(longitude)
                .distanceKm(distanceKm)
                .rating(gym.getRating())
                .availableHours(ruleService.availableHours(gym, packageContext))
                .build();
    }

    private double distance(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2.0) * Math.sin(dLat / 2.0)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2.0) * Math.sin(dLng / 2.0);
        return 6371.0 * 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
    }
}
