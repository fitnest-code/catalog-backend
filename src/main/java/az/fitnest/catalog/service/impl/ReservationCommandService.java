package az.fitnest.catalog.service.impl;

import az.fitnest.catalog.dto.ReservationStatusUpdateRequest;
import az.fitnest.catalog.model.entity.Category;
import az.fitnest.catalog.model.entity.Gym;
import az.fitnest.catalog.model.entity.GymLessonType;
import az.fitnest.catalog.model.entity.Reservation;
import az.fitnest.catalog.model.entity.Trainer;
import az.fitnest.catalog.model.entity.TrainerReservationDate;
import az.fitnest.catalog.model.enums.ReservationStatus;
import az.fitnest.catalog.repository.*;
import az.fitnest.catalog.exception.BadRequestException;
import az.fitnest.catalog.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservationCommandService {

    private final ReservationRepository reservationRepository;
    private final GymRepository gymRepository;
    private final TrainerRepository trainerRepository;
    private final GymLessonTypeRepository gymLessonTypeRepository;
    private final CategoryRepository categoryRepository;
    private final TrainerReservationDateRepository sessionRepository;
    private final ReservationPolicyService policyService;
    private final ReservationAvailabilityService availabilityService;
    private final ReservationAuditService auditService;
    private final CancellationReasonService reasonService;

    @Transactional
    public Reservation createReservation(Long userId, Long gymId, String categoryName, Long classTypeId, Long trainerId, Long sessionId) {
        Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));

        TrainerReservationDate session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("SESSION_NOT_FOUND", "error.session_not_found"));

        policyService.validateReservationAllowed(userId, gym, session);

        if (reservationRepository.existsByUserIdAndReservationDateIdAndStatusIn(userId, sessionId,
                List.of(ReservationStatus.PENDING, ReservationStatus.APPROVED))) {
            throw new BadRequestException("DUPLICATE_RESERVATION", "error.duplicate_reservation");
        }

        if (!session.getGymId().equals(gymId) || !session.getTrainer().getTrainerId().equals(trainerId)) {
            throw new BadRequestException("INVALID_SESSION_REFERENCE", "error.invalid_session_reference");
        }

        Category category = categoryRepository.findByNameIgnoreCase(categoryName)
                .orElseThrow(() -> new ResourceNotFoundException("CATEGORY_NOT_FOUND", "error.category_not_found"));

        if (!gym.getCategories().contains(category)) {
            throw new BadRequestException("CATEGORY_NOT_ASSIGNED_TO_GYM", "error.category_not_assigned_to_gym");
        }

        GymLessonType classType = gymLessonTypeRepository.findById(classTypeId)
                .orElseThrow(() -> new ResourceNotFoundException("CLASS_TYPE_NOT_FOUND", "error.class_type_not_found"));

        if (!availabilityService.isSessionOpen(session)) {
            throw new BadRequestException("SESSION_FULL_OR_CLOSED", "error.session_full_or_closed");
        }

        Reservation reservation = Reservation.builder()
                .userId(userId)
                .gym(gym)
                .trainer(session.getTrainer())
                .reservationDate(session)
                .category(category)
                .classType(classType)
                .lessonType(classType.getName())
                .status(ReservationStatus.APPROVED)
                .build();

        reservation = reservationRepository.save(reservation);

        auditService.log(reservation.getId(), userId, "CREATE", null, "APPROVED", null);

        return reservation;
    }

    @Transactional
    public void cancelReservation(Long reservationId, Long userId, String reasonCode, String additionalNote) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("RESERVATION_NOT_FOUND", "error.reservation_not_found"));

        if (!reservation.getUserId().equals(userId)) {
            throw new BadRequestException("OWNERSHIP_CHECK_FAILED", "error.ownership_check_failed");
        }

        policyService.validateCancellationAllowed(reservation);

        var reason = reasonService.getByCode(reasonCode);
        if (Boolean.TRUE.equals(reason.getRequiresComment()) && (additionalNote == null || additionalNote.trim().isEmpty())) {
            throw new BadRequestException("CANCEL_COMMENT_REQUIRED", "error.cancel_comment_required");
        }

        String oldStatus = reservation.getStatus().name();
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setCancelledAt(LocalDateTime.now());
        reservation.setCancelReasonCode(reasonCode);
        reservation.setCancelAdditionalNote(additionalNote);

        reservationRepository.save(reservation);

        auditService.log(reservationId, userId, "CANCEL", oldStatus, "CANCELLED", reasonCode);
    }

    @Transactional
    public void updateReservationStatus(Long reservationId, ReservationStatusUpdateRequest request) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("RESERVATION_NOT_FOUND", "error.reservation_not_found"));

        String oldStatus = reservation.getStatus().name();
        reservation.setStatus(request.getStatus());
        reservationRepository.save(reservation);

        auditService.log(reservationId, null, "UPDATE_STATUS", oldStatus, request.getStatus().name(), "Admin update");
    }
}
