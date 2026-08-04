package az.fitnest.catalog.controller;

import az.fitnest.catalog.dto.response.AppQrReportResponse;
import az.fitnest.catalog.dto.response.GymPaymentReportItem;
import az.fitnest.catalog.dto.response.QrScansReportResponse;
import az.fitnest.catalog.repository.GymEntranceHistoryRepository;
import az.fitnest.catalog.repository.GymRepository;
import az.fitnest.catalog.service.AppQrCodeService;
import az.fitnest.catalog.client.OrderServiceGrpcClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/reports")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Gym Reports Admin", description = "Zallar üzrə hesabat ucluqları")
@SecurityRequirement(name = "bearerAuth")
public class GymReportController {

    private static final Map<Long, String> PACKAGE_NAME_CACHE = new java.util.concurrent.ConcurrentHashMap<>();
    static {
        PACKAGE_NAME_CACHE.put(1L, "Bronze");
        PACKAGE_NAME_CACHE.put(2L, "Silver");
        PACKAGE_NAME_CACHE.put(3L, "Gold");
        PACKAGE_NAME_CACHE.put(4L, "Platinum");
    }

    private final GymEntranceHistoryRepository gymEntranceHistoryRepository;
    private final GymRepository gymRepository;
    private final OrderServiceGrpcClient orderServiceGrpcClient;
    private final AppQrCodeService appQrCodeService;

    @Operation(summary = "Tətbiq yükləmə QR kodlarının skan hesabatını gətir (Light & Dark)")
    @GetMapping("/app-qr-scans")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AppQrReportResponse> getAppQrScansReport() {
        return ResponseEntity.ok(appQrCodeService.getAppQrReport());
    }

    @Operation(summary = "Ümumi QR giriş sayını gətir")
    @GetMapping("/qr-scans")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<QrScansReportResponse> getQrScansCount(
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        LocalDateTime start = startDate != null ? startDate : LocalDateTime.now().minusDays(30);
        LocalDateTime end = endDate != null ? endDate : LocalDateTime.now();

        long count = gymEntranceHistoryRepository.countByStatusInAndScanDateBetween(
                List.of("ELIGIBLE", "SUCCESSFUL", "SUCCESS", "APPROVED", "Uğurlu"), start, end);

        return ResponseEntity.ok(new QrScansReportResponse(count));
    }

    @Operation(summary = "Zallar üzrə ödəniş hesabatını gətir")
    @GetMapping("/gym-payments")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<GymPaymentReportItem>> getGymPaymentsReport(
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        LocalDateTime start = startDate != null ? startDate : LocalDateTime.now().minusDays(30);
        LocalDateTime end = endDate != null ? endDate : LocalDateTime.now();

        List<Object[]> rawReport = gymEntranceHistoryRepository.getGymPaymentsReport(start, end);

        Set<Long> gymIds = rawReport.stream().map(row -> (Long) row[0]).collect(Collectors.toSet());
        Set<Long> packageIds = rawReport.stream()
                .map(row -> (Long) row[1])
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, az.fitnest.catalog.model.entity.Gym> gymsMap = new HashMap<>();
        if (!gymIds.isEmpty()) {
            try {
                var gymsWithDetails = gymRepository.findWithListDetailsByIdIn(new ArrayList<>(gymIds));
                for (var gym : gymsWithDetails) {
                    gymsMap.put(gym.getId(), gym);
                }
            } catch (Exception e) {
                log.warn("Failed to fetch gyms with details via entity graph: {}", e.getMessage());
            }
        }

        Map<Long, String> packageNamesMap = new HashMap<>();
        List<Long> missingPackageIds = new ArrayList<>();
        for (Long pkgId : packageIds) {
            String cachedName = PACKAGE_NAME_CACHE.get(pkgId);
            if (cachedName != null) {
                packageNamesMap.put(pkgId, cachedName);
            } else {
                missingPackageIds.add(pkgId);
            }
        }

        if (!missingPackageIds.isEmpty()) {
            try {
                var nameInfos = orderServiceGrpcClient.getPackageNamesByIds(missingPackageIds);
                for (var info : nameInfos) {
                    PACKAGE_NAME_CACHE.put(info.getPackageId(), info.getName());
                    packageNamesMap.put(info.getPackageId(), info.getName());
                }
            } catch (Exception e) {
                log.warn("Failed to resolve package names via gRPC: {}", e.getMessage());
            }
        }

        long idCounter = 1;
        List<GymPaymentReportItem> responseList = new ArrayList<>();
        for (Object[] row : rawReport) {
            Long gymId = (Long) row[0];
            Long packageId = (Long) row[1];
            long count = ((Number) row[2]).longValue();
            double sumAmount = ((Number) row[3]).doubleValue();

            az.fitnest.catalog.model.entity.Gym gym = gymsMap.get(gymId);
            String gymName = gym != null ? gym.getName() : "Unknown Gym";
            String subscriptionName = "N/A";
            if (packageId != null) {
                subscriptionName = packageNamesMap.getOrDefault(packageId, "N/A");
            } else {
                try {
                    if (gym != null) {
                        double averagePrice = count > 0 ? sumAmount / count : 0.0;
                        var matchedSub = gym.getSubscriptions().stream()
                                .filter(sub -> sub.getDailyPrice() != null && Math.abs(sub.getDailyPrice() - averagePrice) < 0.01)
                                .findFirst();
                        if (matchedSub.isPresent() && matchedSub.get().getPackageId() != null) {
                            Long estimatedPkgId = matchedSub.get().getPackageId();
                            subscriptionName = PACKAGE_NAME_CACHE.get(estimatedPkgId);
                            if (subscriptionName == null) {
                                var nameInfos = orderServiceGrpcClient.getPackageNamesByIds(List.of(estimatedPkgId));
                                if (!nameInfos.isEmpty()) {
                                    subscriptionName = nameInfos.get(0).getName();
                                    PACKAGE_NAME_CACHE.put(estimatedPkgId, subscriptionName);
                                } else {
                                    subscriptionName = "N/A";
                                }
                            }
                        }
                    }
                } catch (Exception ex) {
                    log.debug("Failed estimating package name fallback", ex);
                }
            }

            responseList.add(new GymPaymentReportItem(idCounter++, gymId, packageId, gymName, count, subscriptionName, sumAmount, sumAmount));
        }

        return ResponseEntity.ok(responseList);
    }
}
