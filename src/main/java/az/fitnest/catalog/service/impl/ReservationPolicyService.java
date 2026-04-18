package az.fitnest.catalog.service.impl;

import az.fitnest.catalog.client.OrderServiceGrpcClient;
import az.fitnest.catalog.client.UserServiceGrpcClient;
import az.fitnest.catalog.model.entity.Gym;
import az.fitnest.catalog.model.entity.Reservation;
import az.fitnest.catalog.model.entity.TrainerReservationDate;
import az.fitnest.catalog.model.enums.ReservationStatus;
import az.fitnest.catalog.exception.BadRequestException;
import az.fitnest.order.grpc.ActiveSubscriptionResponse;
import az.fitnest.user.grpc.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class ReservationPolicyService {

    private final UserServiceGrpcClient userServiceClient;
    private final OrderServiceGrpcClient orderServiceClient;

    public void validateReservationAllowed(Long userId, Gym gym, TrainerReservationDate session) {
        if (!Boolean.TRUE.equals(gym.getIsReservationEnabled())) {
            throw new BadRequestException("RESERVATION_MODULE_NOT_ENABLED", "error.reservation_module_not_enabled");
        }

        UserResponse user = userServiceClient.getUserById(userId);

        ActiveSubscriptionResponse subscription = orderServiceClient.getActiveSubscription(userId);
        if (!"ACTIVE".equals(subscription.getSubscriptionStatus())) {
            throw new BadRequestException("SUBSCRIPTION_NOT_ELIGIBLE", "error.subscription_not_eligible");
        }

        LocalDateTime sessionStart = LocalDateTime.of(session.getDate(), session.getStartTime());
        if (sessionStart.isBefore(LocalDateTime.now())) {
            throw new BadRequestException("SESSION_EXPIRED", "error.session_expired");
        }
    }

    public boolean isFreeCancellationAllowed(Reservation reservation) {
        LocalDateTime sessionStart = LocalDateTime.of(
                reservation.getReservationDate().getDate(),
                reservation.getReservationDate().getStartTime()
        );

        long hoursUntilSession = ChronoUnit.HOURS.between(LocalDateTime.now(), sessionStart);
        return hoursUntilSession >= 24;
    }

    public void validateCancellationAllowed(Reservation reservation) {
        if (reservation.getStatus() != ReservationStatus.PENDING && reservation.getStatus() != ReservationStatus.APPROVED) {
            throw new BadRequestException("CANCELLATION_NOT_ALLOWED", "error.cancellation_not_allowed");
        }

        LocalDateTime sessionStart = LocalDateTime.of(
                reservation.getReservationDate().getDate(),
                reservation.getReservationDate().getStartTime()
        );

        if (LocalDateTime.now().isAfter(sessionStart)) {
            throw new BadRequestException("SESSION_ALREADY_STARTED", "error.session_already_started");
        }
    }
}
