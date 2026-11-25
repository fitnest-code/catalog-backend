package az.fitnest.catalog.service;

import az.fitnest.catalog.dto.PaginatedResponse;
import az.fitnest.catalog.dto.response.*;
import java.time.LocalDateTime;
import java.util.List;

public interface GymEntranceService {
    GymEntranceResponse checkProximity(Double lat, Double lng, Long gymId);
    
    boolean checkGymEntranceEligibilitySimple(Object principal);
    
    String getGymQrUrl(Long gymId);
    
    GymEntranceScanResponse scanGymQrEntrance(Object principal, String qrCodeValue, Double lat, Double lng,
                                              String userAgent, String confirmation, String confirmationToken,
                                              String qrScanId);
    
    /**
     * v5 variant of the QR scan: same flow, but records {@code categoryId} on the history row and
     * (for subcategory selections) prices the entry from {@code gym_category_package_price}.
     * Kept as an overload so the v1 signature/behaviour above stays untouched.
     */
    CategoryScanOutcome scanGymQrEntrance(Object principal, String qrCodeValue, Double lat, Double lng,
                                          String userAgent, String confirmation, String confirmationToken,
                                          String qrScanId, Long categoryId);

    /** Classic response plus the amount persisted on gym_entrance_history (v5 needs it for entryPrice). */
    record CategoryScanOutcome(GymEntranceScanResponse response, Double amount, Long categoryId) {}

    List<GymEntranceHistoryAdminResponse> getGymEntranceHistory(Long gymId);
    
    GymEntranceEligibilityResponse checkGymEntranceEligibility(Object principal);
    
    List<AdminQrScanHistoryResponse> getUserQrScanHistoryAdmin(Long userId, String query, String sort);
    
    GymAnalyticsResponse getGymAnalytics(Long gymId, LocalDateTime startDate, LocalDateTime endDate, String status, String sort, int page, int pageSize);

    void updateGymAnalyticsOverrides(Long gymId, az.fitnest.catalog.dto.request.UpdateGymAnalyticsRequest request);
}
