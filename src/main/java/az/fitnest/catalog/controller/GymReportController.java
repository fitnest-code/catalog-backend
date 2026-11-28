package az.fitnest.catalog.controller;

import az.fitnest.catalog.dto.response.AppQrReportResponse;
import az.fitnest.catalog.dto.response.GymPaymentReportItem;
import az.fitnest.catalog.dto.response.QrScansReportResponse;
import az.fitnest.catalog.model.entity.Gym;
import az.fitnest.catalog.model.entity.GymSubscription;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/reports")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Gym Reports Admin", description = "Zallar üzrə hesabat ucluqları")
@SecurityRequirement(name = "bearerAuth")
public class GymReportController {

    /**
     * Soft cache of package names from order-service only.
     * Do not seed 1→Bronze…4→Platinum — prod package IDs are not those ordinals,
     * and history.packageId is the member's subscription, not the gym entry tier.
     */
    private static final Map<Long, String> PACKAGE_NAME_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

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

    private static final List<String> QR_SUCCESS_STATUSES =
            List.of("ELIGIBLE", "SUCCESSFUL", "SUCCESS", "APPROVED", "Uğurlu");

    @Operation(summary = "Ümumi QR giriş sayını gətir")
    @GetMapping("/qr-scans")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<QrScansReportResponse> getQrScansCount(
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        LocalDateTime end = endDate != null ? endDate : LocalDateTime.now();
        LocalDateTime start = startDate != null ? startDate : end.minusDays(30);

        long count = gymEntranceHistoryRepository.countByStatusInAndScanDateBetween(
                QR_SUCCESS_STATUSES, start, end);

        // Previous equal-length window for MoM/DoD trend without a second chart series.
        long days = Math.max(1, java.time.Duration.between(start, end).toDays());
        LocalDateTime prevEnd = start;
        LocalDateTime prevStart = start.minusDays(days);
        long prevCount = gymEntranceHistoryRepository.countByStatusInAndScanDateBetween(
                QR_SUCCESS_STATUSES, prevStart, prevEnd);

        double pctChange = 0.0;
        if (prevCount > 0) {
            pctChange = ((count - prevCount) * 100.0) / prevCount;
        } else if (count > 0) {
            pctChange = 100.0;
        }

        return ResponseEntity.ok(new QrScansReportResponse(count, Math.abs(pctChange), count >= prevCount));
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
        Set<Long> historyPackageIds = rawReport.stream()
                .map(row -> (Long) row[1])
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, Gym> gymsMap = new HashMap<>();
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

        // Collect gym subscription package IDs (entry-price tiers) + history package IDs for name lookup.
        Set<Long> packageIdsToResolve = new HashSet<>(historyPackageIds);
        for (Gym gym : gymsMap.values()) {
            if (gym.getSubscriptions() == null) {
                continue;
            }
            for (GymSubscription sub : gym.getSubscriptions()) {
                if (sub.getPackageId() != null) {
                    packageIdsToResolve.add(sub.getPackageId());
                }
            }
        }

        Map<Long, String> packageNamesMap = resolvePackageNames(packageIdsToResolve);

        // Rebuild by (gymId, entryPricePackageId) so "Zalın Abunəliyi" is the charged gym tier,
        // not the visiting member's subscription (history.packageId).
        Map<String, Aggregate> aggregates = new LinkedHashMap<>();
        for (Object[] row : rawReport) {
            Long gymId = (Long) row[0];
            Long memberPackageId = (Long) row[1];
            long count = ((Number) row[2]).longValue();
            double sumAmount = ((Number) row[3]).doubleValue();

            // Payment-obligation report: free/home-gym entries (amount 0) are not payable —
            // including them made Go Premium Fitness show Platinum @ 0 ₼ from member packageId.
            if (sumAmount <= 0.0001) {
                continue;
            }

            Gym gym = gymsMap.get(gymId);
            String gymName = gym != null ? gym.getName() : "Unknown Gym";
            double averagePrice = count > 0 ? sumAmount / count : 0.0;

            ResolvedTier tier = resolveEntryTier(
                    gym, averagePrice, memberPackageId, packageNamesMap);

            String key = gymId + "|" + (tier.packageId() != null ? tier.packageId() : "na")
                    + "|" + tier.subscriptionName();
            Aggregate agg = aggregates.computeIfAbsent(key,
                    k -> new Aggregate(gymId, tier.packageId(), gymName, tier.subscriptionName()));
            agg.qrCount += count;
            agg.amount += sumAmount;
        }

        long idCounter = 1;
        List<GymPaymentReportItem> responseList = new ArrayList<>();
        for (Aggregate agg : aggregates.values()) {
            if (agg.amount <= 0.0001) {
                continue;
            }
            responseList.add(new GymPaymentReportItem(
                    idCounter++,
                    agg.gymId,
                    agg.packageId,
                    agg.gymName,
                    agg.qrCount,
                    agg.subscriptionName,
                    agg.amount,
                    agg.amount));
        }

        return ResponseEntity.ok(responseList);
    }

    /**
     * Prefer gym subscription whose dailyPrice matches the charged average (entry price).
     * Never use the visitor's membership package for "Zalın Abunəliyi".
     */
    private static ResolvedTier resolveEntryTier(
            Gym gym,
            double averagePrice,
            Long memberPackageId,
            Map<Long, String> packageNamesMap) {
        if (gym != null && gym.getSubscriptions() != null && !gym.getSubscriptions().isEmpty()) {
            Optional<GymSubscription> matched = gym.getSubscriptions().stream()
                    .filter(sub -> sub.getDailyPrice() != null
                            && Math.abs(sub.getDailyPrice() - averagePrice) < 0.01)
                    .findFirst();
            if (matched.isPresent()) {
                ResolvedTier tier = namedTier(matched.get(), packageNamesMap);
                if (tier != null) {
                    return tier;
                }
            }
            // Single-tier gym: column is "gym subscription" — use that tier even if price drifted.
            if (gym.getSubscriptions().size() == 1) {
                ResolvedTier only = namedTier(gym.getSubscriptions().iterator().next(), packageNamesMap);
                if (only != null) {
                    return only;
                }
            }
            // Multi-tier, no price match: lowest dailyPrice offered by the gym (not member package).
            Optional<GymSubscription> cheapest = gym.getSubscriptions().stream()
                    .filter(sub -> sub.getDailyPrice() != null && sub.getPackageId() != null)
                    .min(Comparator.comparingDouble(GymSubscription::getDailyPrice));
            if (cheapest.isPresent()) {
                ResolvedTier tier = namedTier(cheapest.get(), packageNamesMap);
                if (tier != null) {
                    return tier;
                }
            }
        }
        // Last resort only — prefer N/A over a misleading member-tier badge.
        if (memberPackageId != null) {
            String name = packageNamesMap.get(memberPackageId);
            if (name != null && !name.isBlank()) {
                return new ResolvedTier(memberPackageId, name);
            }
        }
        return new ResolvedTier(null, "N/A");
    }

    private static ResolvedTier namedTier(GymSubscription sub, Map<Long, String> packageNamesMap) {
        if (sub == null || sub.getPackageId() == null) {
            return null;
        }
        String name = packageNamesMap.getOrDefault(sub.getPackageId(), "N/A");
        if ("N/A".equals(name)) {
            return null;
        }
        return new ResolvedTier(sub.getPackageId(), name);
    }

    private Map<Long, String> resolvePackageNames(Set<Long> packageIds) {
        Map<Long, String> packageNamesMap = new HashMap<>();
        if (packageIds == null || packageIds.isEmpty()) {
            return packageNamesMap;
        }
        List<Long> missing = new ArrayList<>();
        for (Long pkgId : packageIds) {
            String cached = PACKAGE_NAME_CACHE.get(pkgId);
            if (cached != null) {
                packageNamesMap.put(pkgId, cached);
            } else {
                missing.add(pkgId);
            }
        }
        if (!missing.isEmpty()) {
            try {
                var nameInfos = orderServiceGrpcClient.getPackageNamesByIds(missing);
                for (var info : nameInfos) {
                    PACKAGE_NAME_CACHE.put(info.getPackageId(), info.getName());
                    packageNamesMap.put(info.getPackageId(), info.getName());
                }
            } catch (Exception e) {
                log.warn("Failed to resolve package names via gRPC: {}", e.getMessage());
            }
        }
        return packageNamesMap;
    }

    private record ResolvedTier(Long packageId, String subscriptionName) {}

    private static final class Aggregate {
        final Long gymId;
        final Long packageId;
        final String gymName;
        final String subscriptionName;
        long qrCount;
        double amount;

        Aggregate(Long gymId, Long packageId, String gymName, String subscriptionName) {
            this.gymId = gymId;
            this.packageId = packageId;
            this.gymName = gymName;
            this.subscriptionName = subscriptionName;
        }
    }
}
