package az.fitnest.catalog.service.impl;

import az.fitnest.catalog.client.OrderServiceGrpcClient;
import az.fitnest.catalog.client.UserServiceGrpcClient;
import az.fitnest.catalog.model.entity.Gym;
import az.fitnest.catalog.model.entity.Reservation;
import az.fitnest.catalog.model.entity.TrainerReservationDate;
import az.fitnest.catalog.model.enums.ReservationStatus;
import az.fitnest.catalog.exception.BadRequestException;
import az.fitnest.order.grpc.ActiveSubscriptionResponse;
import az.fitnest.catalog.client.CachedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class ReservationPolicyServiceImpl implements az.fitnest.catalog.service.ReservationPolicyService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ReservationPolicyServiceImpl.class);

    private final OrderServiceGrpcClient orderServiceClient;

    public void validateReservationAllowed(Long userId, Gym gym, TrainerReservationDate session) {
        if (!Boolean.TRUE.equals(gym.getIsReservationEnabled())) {
            throw new BadRequestException("RESERVATION_MODULE_NOT_ENABLED", "error.reservation_module_not_enabled");
        }

        ActiveSubscriptionResponse subscription = orderServiceClient.getActiveSubscription(userId);
        if (!"ACTIVE".equalsIgnoreCase(subscription.getSubscriptionStatus())) {
            throw new BadRequestException("SUBSCRIPTION_NOT_ELIGIBLE", "error.subscription_not_eligible");
        }

        long userPackageId = subscription.getPackageId();
        java.util.List<Long> gymPackageIds = gym.getSubscriptions().stream()
                .map(s -> s.getPackageId())
                .filter(java.util.Objects::nonNull)
                .toList();

        boolean isPackageSupported = isPackageSufficient(userPackageId, gymPackageIds);

        if (!isPackageSupported) {
            throw new BadRequestException("SUBSCRIPTION_NOT_ELIGIBLE", "error.gym_not_supported");
        }

        LocalDateTime sessionStart = LocalDateTime.of(session.getDate(), session.getStartTime());
        if (sessionStart.isBefore(LocalDateTime.now())) {
            throw new BadRequestException("SESSION_EXPIRED", "error.session_expired");
        }

        if (ChronoUnit.HOURS.between(LocalDateTime.now(), sessionStart) < 2) {
            throw new BadRequestException("BOOKING_RESTRICTED", "error.booking_must_be_2_hours_before");
        }
    }

    private boolean isPackageSufficient(Long userPackageId, java.util.List<Long> gymPackageIds) {
        if (userPackageId == null || gymPackageIds == null || gymPackageIds.isEmpty()) {
            log.info("[isPackageSufficient] Invalid inputs: userPackageId={}, gymPackageIds={}", userPackageId, gymPackageIds);
            return false;
        }
        try {
            java.util.List<az.fitnest.order.grpc.SubscriptionPackageInfo> allPlans = orderServiceClient.getGymPlans();
            log.info("[isPackageSufficient] Retrieved allPlans count: {}", allPlans != null ? allPlans.size() : "null");
            if (allPlans != null) {
                for (var p : allPlans) {
                    log.info("[isPackageSufficient] Plan from allPlans: id={}, name={}", p.getPackageId(), p.getName());
                }
            }
            
            // Get user package rank
            String userPackageName = null;
            for (var plan : allPlans) {
                if (plan.getPackageId() == userPackageId.longValue()) {
                    userPackageName = plan.getName();
                    break;
                }
            }
            if (userPackageName == null) {
                java.util.List<az.fitnest.order.grpc.PackageNameInfo> nameInfos = orderServiceClient.getPackageNamesByIds(java.util.List.of(userPackageId));
                log.info("[isPackageSufficient] Fallback packageNames for user: {}", nameInfos);
                if (!nameInfos.isEmpty()) {
                    userPackageName = nameInfos.get(0).getName();
                }
            }
            int userRank = getPackageRank(userPackageName);
            log.info("[isPackageSufficient] User package rank: name={}, rank={}", userPackageName, userRank);

            // Check if any supported gym package has rank <= user package rank
            for (Long gymPkgId : gymPackageIds) {
                String gymPkgName = null;
                for (var plan : allPlans) {
                    if (plan.getPackageId() == gymPkgId.longValue()) {
                        gymPkgName = plan.getName();
                        break;
                    }
                }
                if (gymPkgName == null) {
                    java.util.List<az.fitnest.order.grpc.PackageNameInfo> nameInfos = orderServiceClient.getPackageNamesByIds(java.util.List.of(gymPkgId));
                    log.info("[isPackageSufficient] Fallback packageNames for gym pkg {}: {}", gymPkgId, nameInfos);
                    if (!nameInfos.isEmpty()) {
                        gymPkgName = nameInfos.get(0).getName();
                    }
                }
                int gymRank = getPackageRank(gymPkgName);
                log.info("[isPackageSufficient] Gym package rank for pkgId={}: name={}, rank={}", gymPkgId, gymPkgName, gymRank);
                if (gymRank > 0 && gymRank <= userRank) {
                    log.info("[isPackageSufficient] Match found! gymRank {} <= userRank {}", gymRank, userRank);
                    return true;
                }
            }
        } catch (Exception e) {
            log.error("[isPackageSufficient] Exception occurred during ranking checks: {}", e.getMessage(), e);
            return gymPackageIds.contains(userPackageId);
        }
        boolean exactContains = gymPackageIds.contains(userPackageId);
        log.info("[isPackageSufficient] No match by rank. Fallback to exact contains: {}", exactContains);
        return exactContains;
    }

    private int getPackageRank(String packageName) {
        if (packageName == null) {
            return 0;
        }
        String lower = packageName.toLowerCase();
        if (lower.contains("platinum")) {
            return 4;
        }
        if (lower.contains("gold")) {
            return 3;
        }
        if (lower.contains("silver")) {
            return 2;
        }
        if (lower.contains("bronze")) {
            return 1;
        }
        return 0;
    }

    public boolean isFreeCancellationAllowed(Reservation reservation) {
        LocalDateTime sessionStart = LocalDateTime.of(
                reservation.getReservationDate().getDate(),
                reservation.getReservationDate().getStartTime()
        );

        long hoursUntilSession = ChronoUnit.HOURS.between(LocalDateTime.now(), sessionStart);
        return hoursUntilSession >= 12;
    }

    public void validateCancellationAllowed(Reservation reservation) {
        if (Boolean.TRUE.equals(reservation.getAttended())) {
            throw new BadRequestException("CANCELLATION_NOT_ALLOWED", "error.cancellation_not_allowed");
        }

        if (reservation.getStatus() != ReservationStatus.PENDING && reservation.getStatus() != ReservationStatus.APPROVED) {
            throw new BadRequestException("CANCELLATION_NOT_ALLOWED", "error.cancellation_not_allowed");
        }

        LocalDateTime sessionStart = LocalDateTime.of(
                reservation.getReservationDate().getDate(),
                reservation.getReservationDate().getStartTime()
        );

        long hoursUntilSession = ChronoUnit.HOURS.between(LocalDateTime.now(), sessionStart);
        if (hoursUntilSession < 12) {
            throw new BadRequestException("CANCELLATION_DISABLED", "error.cancellation_disabled_less_than_12_hours");
        }
    }
}
