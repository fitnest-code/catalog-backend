package az.fitnest.catalog.service;

import az.fitnest.catalog.model.entity.Gym;
import az.fitnest.catalog.model.entity.Reservation;
import az.fitnest.catalog.model.entity.TrainerReservationDate;

public interface ReservationPolicyService {
    void validateReservationAllowed(Long userId, Gym gym, TrainerReservationDate session);
    boolean isFreeCancellationAllowed(Reservation reservation);
    void validateCancellationAllowed(Reservation reservation);
}
