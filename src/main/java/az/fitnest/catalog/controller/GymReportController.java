package az.fitnest.catalog.controller;

import az.fitnest.catalog.dto.response.GymPaymentReportItem;
import az.fitnest.catalog.dto.response.QrScansReportResponse;
import az.fitnest.catalog.repository.GymEntranceHistoryRepository;
import az.fitnest.catalog.repository.GymRepository;
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

    private final GymEntranceHistoryRepository gymEntranceHistoryRepository;
    private final GymRepository gymRepository;
    private final OrderServiceGrpcClient orderServiceGrpcClient;

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

        Map<Long, String> gymNamesMap = new HashMap<>();
        if (!gymIds.isEmpty()) {
            gymNamesMap = gymRepository.findAllById(gymIds).stream()
                    .collect(Collectors.toMap(az.fitnest.catalog.model.entity.Gym::getId, az.fitnest.catalog.model.entity.Gym::getName));
        }

        Map<Long, String> packageNamesMap = new HashMap<>();
        if (!packageIds.isEmpty()) {
            try {
                var nameInfos = orderServiceGrpcClient.getPackageNamesByIds(new ArrayList<>(packageIds));
                for (var info : nameInfos) {
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

            String gymName = gymNamesMap.getOrDefault(gymId, "Unknown Gym");
            String subscriptionName = "N/A";
            if (packageId != null) {
                subscriptionName = packageNamesMap.getOrDefault(packageId, "N/A");
            } else {
                try {
                    var gymOpt = gymRepository.findById(gymId);
                    if (gymOpt.isPresent()) {
                        var gym = gymOpt.get();
                        double averagePrice = count > 0 ? sumAmount / count : 0.0;
                        var matchedSub = gym.getSubscriptions().stream()
                                .filter(sub -> sub.getDailyPrice() != null && Math.abs(sub.getDailyPrice() - averagePrice) < 0.01)
                                .findFirst();
                        if (matchedSub.isPresent() && matchedSub.get().getPackageId() != null) {
                            Long estimatedPkgId = matchedSub.get().getPackageId();
                            subscriptionName = packageNamesMap.getOrDefault(estimatedPkgId, "N/A");
                            if ("N/A".equals(subscriptionName)) {
                                var nameInfos = orderServiceGrpcClient.getPackageNamesByIds(List.of(estimatedPkgId));
                                if (!nameInfos.isEmpty()) {
                                    subscriptionName = nameInfos.get(0).getName();
                                    packageNamesMap.put(estimatedPkgId, subscriptionName);
                                }
                            }
                        }
                    }
                } catch (Exception ex) {
                    log.debug("Failed estimating package name fallback", ex);
                }
            }

            responseList.add(new GymPaymentReportItem(idCounter++, gymName, count, subscriptionName, sumAmount));
        }

        return ResponseEntity.ok(responseList);
    }
}
