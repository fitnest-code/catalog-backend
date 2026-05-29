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
        boolean isPackageSupported = gym.getSubscriptions().stream()
                .anyMatch(s -> s.getPackageId() != null && s.getPackageId().equals(userPackageId));

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
