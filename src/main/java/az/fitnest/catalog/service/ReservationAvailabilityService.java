package az.fitnest.catalog.service;

import az.fitnest.catalog.model.entity.TrainerReservationDate;
import az.fitnest.catalog.model.enums.SessionStatus;

public interface ReservationAvailabilityService {
    int getAvailableSeats(TrainerReservationDate session);
    boolean isSessionOpen(TrainerReservationDate session);
    boolean isExpired(TrainerReservationDate session);
    SessionStatus calculateActualStatus(TrainerReservationDate session);
}
