package az.fitnest.catalog.service.impl;

import az.fitnest.catalog.model.entity.TrainerReservationDate;
import az.fitnest.catalog.model.enums.ReservationStatus;
import az.fitnest.catalog.model.enums.SessionStatus;
import az.fitnest.catalog.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservationAvailabilityService {

    private final ReservationRepository reservationRepository;

    @Transactional(readOnly = true)
    public int getAvailableSeats(TrainerReservationDate session) {
        List<ReservationStatus> activeStatuses = List.of(ReservationStatus.APPROVED, ReservationStatus.PENDING);
        int reservedCount = reservationRepository.countActiveReservations(session.getId(), activeStatuses);

        return Math.max(0, session.getEmptySpaces() - reservedCount);
    }

    public boolean isSessionOpen(TrainerReservationDate session) {
        if (session.getStatus() != SessionStatus.OPEN) {
            return false;
        }

        if (getAvailableSeats(session) <= 0) {
            return false;
        }

        return !isExpired(session);
    }

    public boolean isExpired(TrainerReservationDate session) {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        if (session.getDate().isBefore(today)) {
            return true;
        }

        if (session.getDate().isEqual(today)) {
            return session.getStartTime().isBefore(now);
        }

        return false;
    }

    public SessionStatus calculateActualStatus(TrainerReservationDate session) {
        if (session.getStatus() == SessionStatus.CANCELLED || session.getStatus() == SessionStatus.CLOSED) {
            return session.getStatus();
        }

        if (isExpired(session)) {
            return SessionStatus.CLOSED;
        }

        if (getAvailableSeats(session) <= 0) {
            return SessionStatus.FULL;
        }

        return SessionStatus.OPEN;
    }
}
