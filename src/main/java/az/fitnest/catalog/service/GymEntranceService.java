package az.fitnest.catalog.service;

import az.fitnest.catalog.dto.PaginatedResponse;
import az.fitnest.catalog.dto.response.*;
import java.time.LocalDateTime;
import java.util.List;

public interface GymEntranceService {
    GymEntranceResponse checkProximity(Double lat, Double lng, Long gymId);
    
    boolean checkGymEntranceEligibilitySimple(Object principal);
    
    String getGymQrUrl(Long gymId);
    
    GymEntranceScanResponse scanGymQrEntrance(Object principal, String qrCodeValue, Double lat, Double lng, String userAgent);
    
    List<GymEntranceHistoryAdminResponse> getGymEntranceHistory(Long gymId);
    
    GymEntranceEligibilityResponse checkGymEntranceEligibility(Object principal);
    
    List<AdminQrScanHistoryResponse> getUserQrScanHistoryAdmin(Long userId, String query, String sort);
    
    GymAnalyticsResponse getGymAnalytics(Long gymId, LocalDateTime startDate, LocalDateTime endDate, String status, String sort, int page, int pageSize);
}
