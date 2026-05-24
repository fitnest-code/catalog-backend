package az.fitnest.catalog.scheduler;

import az.fitnest.catalog.client.OrderServiceGrpcClient;
import az.fitnest.catalog.model.entity.Reservation;
import az.fitnest.catalog.model.enums.ReservationStatus;
import az.fitnest.catalog.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReservationScheduler {

    private final ReservationRepository reservationRepository;
    private final OrderServiceGrpcClient orderServiceClient;

    @Scheduled(cron = "0 */5 * * * *") // Runs every 5 minutes
    @Transactional
    public void cleanupExpiredReservations() {
        log.info("Running scheduled cleanup for expired unattended reservations...");
        List<Reservation> expiredReservations = reservationRepository.findExpiredUnattendedReservations(
                LocalDate.now(), LocalTime.now(), List.of(ReservationStatus.PENDING, ReservationStatus.APPROVED)
        );

        for (Reservation reservation : expiredReservations) {
            try {
                // Set status to EXPIRED first to prevent concurrent operations
                reservation.setStatus(ReservationStatus.EXPIRED);
                reservationRepository.save(reservation);

                // Consume frozen session via gRPC client
                orderServiceClient.consumeFrozenSession(reservation.getUserId());
                log.info("Expired unattended reservation ID {} for user {}. Frozen session consumed.",
                        reservation.getId(), reservation.getUserId());
            } catch (Exception e) {
                log.error("Failed to clean up expired reservation ID {}: {}", reservation.getId(), e.getMessage());
            }
        }
    }
}
