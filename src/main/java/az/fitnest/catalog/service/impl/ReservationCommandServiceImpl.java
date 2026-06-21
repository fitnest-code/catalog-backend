package az.fitnest.catalog.service.impl;

import az.fitnest.catalog.dto.request.ReservationRuleUpdateRequest;
import az.fitnest.catalog.dto.*;
import az.fitnest.catalog.dto.request.*;
import az.fitnest.catalog.dto.response.*;
import az.fitnest.catalog.dto.request.ReservationStatusUpdateRequest;
import az.fitnest.catalog.model.entity.*;
import az.fitnest.catalog.model.enums.ReservationStatus;
import az.fitnest.catalog.repository.*;
import az.fitnest.catalog.exception.BadRequestException;
import az.fitnest.catalog.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import az.fitnest.catalog.service.ReservationPolicyService;
import az.fitnest.catalog.service.ReservationAvailabilityService;
import az.fitnest.catalog.service.ReservationAuditService;
import az.fitnest.catalog.service.CancellationReasonService;
import az.fitnest.catalog.service.TranslationService;
import az.fitnest.catalog.client.OrderServiceGrpcClient;
import az.fitnest.catalog.client.NotificationsServiceGrpcClient;
import az.fitnest.catalog.client.UserServiceGrpcClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronization;

@Service
@RequiredArgsConstructor
public class ReservationCommandServiceImpl implements az.fitnest.catalog.service.ReservationCommandService {

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
    private final ReservationRuleRepository ruleRepository;
    private final OrderServiceGrpcClient orderServiceClient;
    private final GymAdminRepository gymAdminRepository;
    private final NotificationsServiceGrpcClient notificationsServiceClient;
    private final TranslationService translationService;
    private final UserServiceGrpcClient userServiceGrpcClient;

    @Transactional
    public Reservation createReservation(Long userId, Long gymId, Long categoryId, Long lessonId, Long trainerId, Long sessionId) {
        Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));

        TrainerReservationDate session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("SESSION_NOT_FOUND", "error.session_not_found"));

        policyService.validateReservationAllowed(userId, gym, session);

        if (lessonId != null && session.getClassType() != null && !session.getClassType().getId().equals(lessonId)) {
            throw new BadRequestException("LESSON_SESSION_MISMATCH", "error.lesson_session_mismatch");
        }

        if (lessonId != null && reservationRepository.existsByUserIdAndClassTypeIdAndReservationDateDateAndStatusIn(userId, lessonId, session.getDate(),
                List.of(ReservationStatus.PENDING, ReservationStatus.APPROVED))) {
            throw new BadRequestException("DUPLICATE_DAILY_RESERVATION", "error.duplicate_daily_reservation");
        }

        if (!session.getGymId().equals(gymId)) {
            throw new BadRequestException("INVALID_SESSION_REFERENCE", "error.invalid_session_reference");
        }

        if (trainerId != null && session.getTrainer() != null && !session.getTrainer().getId().equals(trainerId)) {
            throw new BadRequestException("INVALID_SESSION_REFERENCE", "error.invalid_session_reference");
        }

        Category category = null;
        if (categoryId != null) {
            category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new ResourceNotFoundException("CATEGORY_NOT_FOUND", "error.category_not_found"));

            if (gym.getCategory() == null || !gym.getCategory().getId().equals(category.getId())) {
                throw new BadRequestException("CATEGORY_NOT_ASSIGNED_TO_GYM", "error.category_not_assigned_to_gym");
            }
        } else if (session.getClassType() != null && session.getClassType().getCategory() != null) {
            category = session.getClassType().getCategory();
        }

        GymLessonType classType = null;
        if (lessonId != null) {
            classType = gymLessonTypeRepository.findById(lessonId)
                    .orElseThrow(() -> new ResourceNotFoundException("CLASS_TYPE_NOT_FOUND", "error.class_type_not_found"));
        } else if (session.getClassType() != null) {
            classType = session.getClassType();
        }

        Trainer trainer = null;
        if (trainerId != null) {
            trainer = trainerRepository.findById(trainerId).orElse(null);
        } else if (session.getTrainer() != null) {
            trainer = session.getTrainer();
        }

        Reservation reservation = Reservation.builder()
                .userId(userId)
                .gym(gym)
                .trainer(trainer)
                .category(category)
                .classType(classType)
                .lessonType(classType != null ? classType.getName() : null)
                .reservationDate(session)
                .status(ReservationStatus.PENDING)
                .build();

        orderServiceClient.freezeSession(userId);

        reservation = reservationRepository.save(reservation);

        auditService.log(reservation.getId(), userId, "CREATE", null, "PENDING", null);

        sendReservationNotificationToAdmins(reservation);

        return reservation;
    }

    @Transactional
    public void cancelReservation(Long sessionId, Long userId, String reasonCode, String additionalNote) {
        Reservation reservation = reservationRepository.findFirstByUserIdAndReservationDateIdAndStatusIn(
                        userId, sessionId, List.of(ReservationStatus.PENDING, ReservationStatus.APPROVED))
                .orElseThrow(() -> new ResourceNotFoundException("RESERVATION_NOT_FOUND", "error.reservation_not_found"));

        policyService.validateCancellationAllowed(reservation);

        var reason = reasonService.getByCode(reasonCode);

        String finalNote = additionalNote;
        if (!"OTHER".equalsIgnoreCase(reasonCode)) {
            finalNote = null;
        }

        if (Boolean.TRUE.equals(reason.getRequiresComment()) && (finalNote == null || finalNote.trim().isEmpty())) {
            throw new BadRequestException("CANCEL_COMMENT_REQUIRED", "error.cancel_comment_required");
        }

        String oldStatus = reservation.getStatus().name();
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setCancelledAt(LocalDateTime.now());
        reservation.setCancelReasonCode(reasonCode);
        reservation.setCancelAdditionalNote(finalNote);

        reservationRepository.save(reservation);

        if (!Boolean.TRUE.equals(reservation.getAttended()) &&
            (oldStatus.equals(ReservationStatus.APPROVED.name()) || oldStatus.equals(ReservationStatus.PENDING.name()))) {
            orderServiceClient.restoreSession(userId);
        }

        auditService.log(reservation.getId(), userId, "CANCEL", oldStatus, "CANCELLED", reasonCode);
    }

    @Transactional
    public void updateReservationStatus(Long reservationId, ReservationStatusUpdateRequest request) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("RESERVATION_NOT_FOUND", "error.reservation_not_found"));

        verifyReservationGymAccess(reservation.getGym().getId());

        String oldStatus = reservation.getStatus().name();
        reservation.setStatus(request.getStatus());
        reservation = reservationRepository.save(reservation);

        if (!Boolean.TRUE.equals(reservation.getAttended()) &&
            (oldStatus.equals(ReservationStatus.APPROVED.name()) || oldStatus.equals(ReservationStatus.PENDING.name())) &&
            (request.getStatus() == ReservationStatus.CANCELLED || request.getStatus() == ReservationStatus.REJECTED)) {
            orderServiceClient.restoreSession(reservation.getUserId());
        }

        auditService.log(reservationId, null, "UPDATE_STATUS", oldStatus, request.getStatus().name(), "Admin update");

        if (request.getStatus() == ReservationStatus.REJECTED || request.getStatus() == ReservationStatus.APPROVED) {
            sendStatusUpdateNotificationToUser(reservation, request.getStatus(), null);
        }
    }

    @Transactional
    public void approveReservation(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("RESERVATION_NOT_FOUND", "error.reservation_not_found"));

        verifyReservationGymAccess(reservation.getGym().getId());

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new BadRequestException("INVALID_STATUS", "error.reservation_not_pending");
        }

        String oldStatus = reservation.getStatus().name();
        reservation.setStatus(ReservationStatus.APPROVED);
        reservation.setApprovedAt(LocalDateTime.now());
        reservation = reservationRepository.save(reservation);

        auditService.log(reservationId, null, "APPROVE", oldStatus, "APPROVED", "Admin approval");

        sendStatusUpdateNotificationToUser(reservation, ReservationStatus.APPROVED, null);
    }

    @Transactional
    public void rejectReservation(Long reservationId, String reason) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("RESERVATION_NOT_FOUND", "error.reservation_not_found"));

        verifyReservationGymAccess(reservation.getGym().getId());

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new BadRequestException("INVALID_STATUS", "error.reservation_not_pending");
        }

        String oldStatus = reservation.getStatus().name();
        reservation.setStatus(ReservationStatus.REJECTED);
        reservation.setCancelledAt(LocalDateTime.now());
        reservation.setCancelReasonText(reason);
        reservation = reservationRepository.save(reservation);

        if (!Boolean.TRUE.equals(reservation.getAttended())) {
            orderServiceClient.restoreSession(reservation.getUserId());
        }

        auditService.log(reservationId, null, "REJECT", oldStatus, "REJECTED", reason);

        sendStatusUpdateNotificationToUser(reservation, ReservationStatus.REJECTED, reason);
    }

    private void sendStatusUpdateNotificationToUser(Reservation reservation, ReservationStatus newStatus, String reason) {
        if (reservation == null || reservation.getUserId() == null) {
            return;
        }
        final Long userId = reservation.getUserId();
        final String gymName = reservation.getGym() != null ? reservation.getGym().getName() : "";
        final String lessonName = reservation.getLessonType() != null ? reservation.getLessonType() :
                (reservation.getCategory() != null ? reservation.getCategory().getName() : "Məşq");
        
        // Resolve user language preference from UserServiceGrpcClient
        String userLang = "AZ";
        try {
            az.fitnest.catalog.client.CachedUser user = userServiceGrpcClient.getUserById(userId);
            if (user != null && user.getLanguage() != null && !user.getLanguage().isEmpty()) {
                userLang = user.getLanguage().toUpperCase();
            }
        } catch (Exception e) {
            // Ignored
        }

        final String title;
        final String body;

        if (newStatus == ReservationStatus.REJECTED) {
            String targetReason = reason;
            if (reason != null && !reason.trim().isEmpty()) {
                if ("EN".equals(userLang)) {
                    try {
                        String translated = translationService.translateText(reason, "en");
                        if (translated != null && !translated.trim().isEmpty()) {
                            targetReason = translated;
                        }
                    } catch (Exception e) {
                        // ignore
                    }
                } else if ("RU".equals(userLang)) {
                    try {
                        String translated = translationService.translateText(reason, "ru");
                        if (translated != null && !translated.trim().isEmpty()) {
                            targetReason = translated;
                        }
                    } catch (Exception e) {
                        // ignore
                    }
                }
            }

            switch (userLang) {
                case "EN":
                    title = "Reservation Not Approved";
                    if (targetReason != null && !targetReason.trim().isEmpty()) {
                        body = String.format("%s - %s reservation was not approved. Reason: %s", gymName, lessonName, targetReason);
                    } else {
                        body = String.format("%s - %s reservation was not approved.", gymName, lessonName);
                    }
                    break;
                case "RU":
                    title = "Бронирование отклонено";
                    if (targetReason != null && !targetReason.trim().isEmpty()) {
                        body = String.format("%s - %s ваше бронирование не одобрено. Причина: %s", gymName, lessonName, targetReason);
                    } else {
                        body = String.format("%s - %s ваше бронирование не одобрено.", gymName, lessonName);
                    }
                    break;
                case "AZ":
                default:
                    title = "Rezervasiya təsdiq edilmədi";
                    if (targetReason != null && !targetReason.trim().isEmpty()) {
                        body = String.format("%s - %s rezervasiyanız təsdiq edilmədi. Səbəb: %s", gymName, lessonName, targetReason);
                    } else {
                        body = String.format("%s - %s rezervasiyanız təsdiq edilmədi.", gymName, lessonName);
                    }
                    break;
            }
        } else if (newStatus == ReservationStatus.APPROVED) {
            switch (userLang) {
                case "EN":
                    title = "Reservation Confirmed";
                    body = String.format("Your reservation for %s - %s has been successfully confirmed!", gymName, lessonName);
                    break;
                case "RU":
                    title = "Бронирование подтверждено";
                    body = String.format("Ваше бронирование %s - %s успешно подтверждено!", gymName, lessonName);
                    break;
                case "AZ":
                default:
                    title = "Rezervasiya təsdiqləndi";
                    body = String.format("%s - %s rezervasiyanız uğurla təsdiqləndi!", gymName, lessonName);
                    break;
            }
        } else {
            return; // Only notify for approval and rejection
        }

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        CompletableFuture.runAsync(() -> {
                            try {
                                notificationsServiceClient.sendPushNotification(userId, title, body);
                            } catch (Exception e) {
                                // Ignore to avoid breaking execution
                            }
                        });
                    }
                }
            );
        } else {
            CompletableFuture.runAsync(() -> {
                try {
                    notificationsServiceClient.sendPushNotification(userId, title, body);
                } catch (Exception e) {
                    // Ignore
                }
            });
        }
    }

    @Transactional
    public void saveOrUpdateRules(Long gymId, Long categoryId, Long lessonId, ReservationRuleUpdateRequest request) {
        Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));
        
        Category category = null;
        if (categoryId != null) {
            category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new ResourceNotFoundException("CATEGORY_NOT_FOUND", "error.category_not_found"));
        }
        
        GymLessonType lessonType = null;
        if (lessonId != null) {
            lessonType = gymLessonTypeRepository.findById(lessonId)
                    .orElseThrow(() -> new ResourceNotFoundException("LESSON_TYPE_NOT_FOUND", "error.class_type_not_found"));
        }

        List<ReservationRule> existingRules;
        if (categoryId != null && lessonId != null) {
            existingRules = ruleRepository.findByGymIdAndCategoryIdAndLessonTypeIdAndStatus(gymId, categoryId, lessonId, "ACTIVE");
        } else {
            existingRules = ruleRepository.findByGymIdAndCategoryIdIsNullAndLessonTypeIsNullAndStatus(gymId, "ACTIVE");
        }

        ReservationRule rule;
        if (!existingRules.isEmpty()) {
            rule = existingRules.get(0);
            rule.setDescription(request.getHtmlContent());
        } else {
            String title = (lessonType != null) ? "Rules for " + lessonType.getName() : "Rules for " + gym.getName();
            rule = ReservationRule.builder()
                    .gym(gym)
                    .category(category)
                    .lessonType(lessonType)
                    .title(title)
                    .description(request.getHtmlContent())
                    .status("ACTIVE")
                    .build();
        }
        rule = ruleRepository.save(rule);
        translationService.autoTranslateAndSave("RESERVATION_RULE", String.valueOf(rule.getId()), "description", request.getHtmlContent());
    }

    private void sendReservationNotificationToAdmins(Reservation reservation) {
        if (reservation == null || reservation.getGym() == null) {
            return;
        }
        final Long gymId = reservation.getGym().getId();
        final String gymName = reservation.getGym().getName();
        final String lessonName = reservation.getLessonType() != null ? reservation.getLessonType() :
                (reservation.getCategory() != null ? reservation.getCategory().getName() : "Dərs");
        final String dateStr = reservation.getReservationDate() != null && reservation.getReservationDate().getDate() != null
                ? reservation.getReservationDate().getDate().toString() : "";
        final String timeStr = reservation.getReservationDate() != null && reservation.getReservationDate().getStartTime() != null
                ? reservation.getReservationDate().getStartTime().toString() : "";
        
        final String title = "Yeni Rezervasiya";
        final String body = String.format("Yeni rezervasiya: %s - %s (%s %s)", gymName, lessonName, dateStr, timeStr);

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        triggerNotificationAsync(gymId, title, body);
                    }
                }
            );
        } else {
            triggerNotificationAsync(gymId, title, body);
        }
    }

    private void triggerNotificationAsync(Long gymId, String title, String body) {
        CompletableFuture.runAsync(() -> {
            try {
                List<GymAdmin> admins = gymAdminRepository.findByGymId(gymId);
                if (admins != null) {
                    for (GymAdmin admin : admins) {
                        if (admin.getUserId() != null) {
                            notificationsServiceClient.sendPushNotification(admin.getUserId(), title, body);
                        }
                    }
                }
            } catch (Exception e) {
                // Ignore to avoid breaking execution
            }
        });
    }

    private void verifyReservationGymAccess(Long gymId) {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new az.fitnest.catalog.exception.UnauthorizedException("Unauthorized");
        }
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) {
            return;
        }
        Long userId = az.fitnest.catalog.util.UserContext.getCurrentUserId();
        if (userId == null || !gymAdminRepository.existsByGymIdAndUserId(gymId, userId)) {
            throw new az.fitnest.catalog.exception.ForbiddenException("You do not have access to this gym");
        }
    }
}
