package az.fitnest.catalog.service;

import az.fitnest.catalog.dto.request.ReservationRuleUpdateRequest;
import az.fitnest.catalog.dto.*;
import az.fitnest.catalog.dto.request.*;
import az.fitnest.catalog.dto.response.*;
import az.fitnest.catalog.dto.request.ReservationStatusUpdateRequest;
import az.fitnest.catalog.model.entity.Reservation;

public interface ReservationCommandService {
    Reservation createReservation(Long userId, Long gymId, Long categoryId, Long lessonId, Long trainerId, Long sessionId);
    void cancelReservation(Long sessionId, Long userId, String reasonCode, String additionalNote);
    void updateReservationStatus(Long reservationId, ReservationStatusUpdateRequest request);
    void approveReservation(Long reservationId);
    void rejectReservation(Long reservationId, String reason);
    void saveOrUpdateRules(Long gymId, Long categoryId, Long lessonId, ReservationRuleUpdateRequest request);
}
