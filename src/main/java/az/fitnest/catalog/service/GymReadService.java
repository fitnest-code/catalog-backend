package az.fitnest.catalog.service;

import az.fitnest.catalog.dto.PaginatedResponse;
import az.fitnest.catalog.dto.request.RestDayRequest;
import az.fitnest.catalog.dto.response.*;
import az.fitnest.catalog.model.enums.ReservationStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface GymReadService {

    // ── V1 metodları ─────────────────────────────────────────────
    GymDetailResponse getGymDetail(Long userId, Long gymId);

    GymImageResponse getGymImages(Long gymId);

    boolean isReservationEnabled(Long gymId);

    Map<String, Object> getReservationRules(Long gymId);

    List<GymNearbyResponse> getNearbyGyms(double lat, double lng,
                                          double radiusKm);

    PaginatedResponse<GymMainPageResponse> getClosestGyms(Long userId,
                                                          int page, int pageSize, Double userLat, Double userLng);

    PaginatedResponse<GymMainPageResponse> getGyms(Long userId, String q,
                                                   String type, Long categoryId, Long subscriptionId, int page,
                                                   int pageSize, Double userLat, Double userLng, String sortDir);

    PaginatedResponse<AdminGymResponse> getAllGymsAdmin(String query,
                                                        String sort, int page, int pageSize);

    List<AdminQrScanHistoryResponse> getUserQrScanHistoryAdmin(Long userId,
                                                               String query, String sort);

    LocationResponse getGymLocation(Long gymId);

    boolean gymSupportsPlan(Long gymId, Long planId);

    GymEntranceResponse checkProximity(Double lat, Double lng, Long gymId);

    boolean checkGymEntranceEligibilitySimple(Object principal);

    GymEntranceScanResponse scanGymQrEntrance(Object principal,
                                              String qrCodeValue, Double lat, Double lng, String userAgent);

    List<GymEntranceHistoryAdminResponse> getGymEntranceHistory(Long gymId);

    GymAnalyticsResponse getGymAnalytics(Long gymId,
                                         java.time.LocalDateTime startDate,
                                         java.time.LocalDateTime endDate,
                                         String statusFilter, String sort, int page, int pageSize);

    GymEntranceEligibilityResponse checkGymEntranceEligibility(
            Object principal);

    GymCountResponse getGymCount(String type, Long subscriptionId,
                                 Long categoryId);

    GymTypeCountResponse getGymCountByType(String type);

    List<GymCategoryCountResponse> getGymCountByCategory();

    List<GymSubscriptionCountResponse> getGymCountBySubscription();

    GymTypeCountResponse getGymCountByGender(String gender);

    String getGymQrUrl(Long gymId);

    List<SupportedServiceResponse> getAllSupportedServices(Long gymId);

    GymInfoAdminResponse getGymDetailsAdmin(Long gymId);

    GymWorkHoursAdminResponse getGymWorkHours(Long gymId);

    GymSubscriptionsAdminResponse getGymSubscriptions(Long gymId);

    List<GymAdminResponse> getGymAdmins(Long gymId);

    PaginatedResponse<ReservationAdminResponse> getGymReservationsAdmin(
            Long gymId, ReservationStatus status, int page, int pageSize);

    ReservationDetailAdminResponse getReservationDetailAdmin(
            Long reservationId);

    ReservationStatsResponse getGymReservationStats(Long gymId);

    PaginatedResponse<ReservationAdminResponse> getAllReservationsAdmin(
            ReservationStatus status, int page, int pageSize);

    PaginatedResponse<LessonHourResponse> getGymLessonHoursAdmin(Long gymId,
                                                                 LocalDate startDate, LocalDate endDate, int page, int pageSize);

    // ── V2 metodları ─────────────────────────────────────────────

    GymDetailResponseV2 getGymDetailV2(Long userId, Long gymId);

    PaginatedResponse<GymMainPageResponseV2> getGymsV2(Long userId, String q,
                                                       String type, Long categoryId, Long subscriptionId, int page,
                                                       int pageSize, Double userLat, Double userLng, String sortDir);

    GymInfoAdminResponseV2 getGymDetailsAdminV2(Long gymId);

    GymSubscriptionsAdminResponseV2 getGymSubscriptionsV2(Long gymId);
}