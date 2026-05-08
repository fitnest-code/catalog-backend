package az.fitnest.catalog.service;

import az.fitnest.catalog.dto.*;
import az.fitnest.catalog.dto.request.*;
import az.fitnest.catalog.dto.response.*;
import java.util.List;
import java.util.Map;

public interface GymReadService {
    List<SupportedServiceResponse> getAllSupportedServices(Long gymId);
    String getUserLanguage(Long userId);
    GymDetailResponse getGymDetail(Long userId, Long gymId);
    GymImageResponse getGymImages(Long gymId);
    boolean isReservationEnabled(Long gymId);
    Map<String, Object> getReservationRules(Long gymId);
    List<GymNearbyResponse> getNearbyGyms(double lat, double lng, double radiusKm);
    PaginatedResponse<GymMainPageResponse> getClosestGyms(Long userId, int page, int pageSize, Double userLat, Double userLng);
    PaginatedResponse<GymMainPageResponse> getGyms(Long userId, String q, String type, Long categoryId, Long subscriptionId, int page, int pageSize, Double userLat, Double userLng, String sortDir);
    PaginatedResponse<AdminGymResponse> getAllGymsAdmin(String query, String sort, int page, int pageSize);
    List<AdminQrScanHistoryResponse> getUserQrScanHistoryAdmin(Long userId, String query, String sort);
    LocationResponse getGymLocation(Long gymId);
    boolean gymSupportsPlan(Long gymId, Long planId);
    GymEntranceResponse checkProximity(Double lat, Double lng, Long gymId);
    boolean checkGymEntranceEligibilitySimple(Object principal);
    GymCountResponse getGymCount(String type, Long subscriptionId, Long categoryId);
    GymTypeCountResponse getGymCountByType(String type);
    List<GymCategoryCountResponse> getGymCountByCategory();
    List<GymSubscriptionCountResponse> getGymCountBySubscription();
    double calculateDistanceRaw(double lat1, double lng1, double lat2, double lng2);
    String getGymQrUrl(Long gymId);
    GymEntranceScanResponse scanGymQrEntrance(Object principal, String qrCodeValue, Double lat, Double lng, String userAgent);
    List<GymEntranceHistoryAdminResponse> getGymEntranceHistory(Long gymId);
    GymEntranceEligibilityResponse checkGymEntranceEligibility(Object principal);
    GymTypeCountResponse getGymCountByGender(String gender);
}
