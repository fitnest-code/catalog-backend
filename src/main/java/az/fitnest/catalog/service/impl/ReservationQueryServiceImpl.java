package az.fitnest.catalog.service.impl;

import az.fitnest.catalog.dto.*;
import az.fitnest.catalog.dto.request.*;
import az.fitnest.catalog.dto.response.*;
import az.fitnest.catalog.client.OrderServiceGrpcClient;
import az.fitnest.catalog.model.entity.Category;
import az.fitnest.catalog.model.entity.Gym;
import az.fitnest.catalog.model.entity.GymLessonType;
import az.fitnest.catalog.model.entity.Reservation;
import az.fitnest.catalog.model.entity.ReservationRule;
import az.fitnest.catalog.model.entity.Trainer;
import az.fitnest.catalog.model.entity.TrainerReservationDate;
import az.fitnest.catalog.model.enums.ReservationStatus;
import az.fitnest.catalog.repository.CategoryRepository;
import az.fitnest.catalog.repository.GymLessonTypeRepository;
import az.fitnest.catalog.repository.GymRepository;
import az.fitnest.catalog.repository.ReservationRepository;
import az.fitnest.catalog.repository.ReservationRuleRepository;
import az.fitnest.catalog.repository.TrainerRepository;
import az.fitnest.catalog.repository.TrainerReservationDateRepository;
import az.fitnest.catalog.exception.BadRequestException;
import az.fitnest.catalog.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReservationQueryServiceImpl implements az.fitnest.catalog.service.ReservationQueryService {

    private final CategoryRepository categoryRepository;
    private final GymLessonTypeRepository gymLessonTypeRepository;
    private final TrainerReservationDateRepository sessionRepository;
    private final ReservationRuleRepository ruleRepository;
    private final ReservationRepository reservationRepository;
    private final GymRepository gymRepository;
    private final TrainerRepository trainerRepository;
    private final OrderServiceGrpcClient orderServiceClient;

    @Transactional(readOnly = true)
    public List<ReservationLessonResponse> getLessonsForReservation(Long gymId, Long categoryId) {
        return gymLessonTypeRepository.findByGymIdAndCategoryIdAndStatus(gymId, categoryId, "ACTIVE").stream()
                .map(lt -> ReservationLessonResponse.builder()
                        .gymId(gymId)
                        .categoryId(categoryId)
                        .lessonId(lt.getId())
                        .lessonName(lt.getName())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReservationTeacherResponse> getTrainersForLesson(Long gymId, Long categoryId, Long lessonTypeId) {
        return trainerRepository.findByGymId(gymId, PageRequest.of(0, 100)).getContent().stream()
                .filter(t -> t.getEnabledLessonTypes().stream().anyMatch(lt -> lt.getId().equals(lessonTypeId)))
                .filter(t -> Boolean.TRUE.equals(t.getIsReservationEnabled()))
                .map(t -> ReservationTeacherResponse.builder()
                        .teacherId(t.getId())
                        .teacherName(t.getFirstName() + " " + t.getLastName())
                        .teacherImageProfileUrl(t.getProfileImageUrl())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DayAvailabilityResponse> getAvailabilityForTeacher(Long userId, Long gymId, Long categoryId, Long lessonTypeId, Long trainerId) {
        List<TrainerReservationDate> sessions = sessionRepository.findByTrainerIdOrderByDateAscStartTimeAsc(trainerId);

        Gym gym = gymRepository.findById(gymId).orElse(null);
        boolean isGymEnabled = gym != null && Boolean.TRUE.equals(gym.getIsReservationEnabled());

        az.fitnest.order.grpc.ActiveSubscriptionResponse subscription = null;
        if (userId != null && isGymEnabled) {
            try {
                subscription = orderServiceClient.getActiveSubscription(userId);
            } catch (Exception ignored) {
            }
        }

        final az.fitnest.order.grpc.ActiveSubscriptionResponse sub = subscription;
        final boolean isPackageSupported = (sub != null && gym != null) && gym.getSubscriptions().stream()
                .anyMatch(s -> s.getPackageId() != null && s.getPackageId().equals(sub.getPackageId()));
        final boolean isSubscriptionActive = sub != null && "Active".equalsIgnoreCase(sub.getSubscriptionStatus());

        LocalDateTime now = LocalDateTime.now();

        return sessions.stream()
                .filter(session -> {
                    LocalDateTime sessionStart = LocalDateTime.of(session.getDate(), session.getStartTime());
                    return !sessionStart.isBefore(now);
                })
                .collect(Collectors.groupingBy(TrainerReservationDate::getDate))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    LocalDate date = entry.getKey();
                    return DayAvailabilityResponse.builder()
                        .date(date)
                        .slots(entry.getValue().stream().map(session -> {
                            int activeBookings = reservationRepository.countActiveReservations(session.getId(),
                                List.of(ReservationStatus.PENDING, ReservationStatus.APPROVED));
                            int emptySpaces = Math.max(0, session.getEmptySpaces() - activeBookings);

                            boolean isAcceptable = isGymEnabled;

                            if (emptySpaces <= 0) {
                                isAcceptable = false;
                            }

                            if (userId != null && isAcceptable) {
                                if (!isSubscriptionActive || !isPackageSupported) {
                                    isAcceptable = false;
                                } else {
                                    boolean hasOverlap = reservationRepository.existsOverlappingReservation(
                                            userId, date, session.getStartTime(), session.getEndTime(),
                                            List.of(ReservationStatus.PENDING, ReservationStatus.APPROVED));
                                    if (hasOverlap) {
                                        isAcceptable = false;
                                    }
                                }
                            }

                            return TimeSlotResponse.builder()
                                    .sessionId(session.getId())
                                    .startTime(session.getStartTime())
                                    .endTime(session.getEndTime())
                                    .emptySpaces(emptySpaces)
                                    .isRegisterAcceptable(isAcceptable)
                                    .build();
                        })
                        .filter(slot -> slot.getEmptySpaces() > 0)
                        .collect(Collectors.toList()))
                        .build();
                })
                .filter(day -> !day.getSlots().isEmpty())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Trainer> getTrainers(Long gymId, Long classTypeId, LocalDate date) {
        List<TrainerReservationDate> sessions = sessionRepository.findByGymIdAndClassTypeIdAndDate(gymId, classTypeId, date);

        return sessions.stream()
                .map(TrainerReservationDate::getTrainer)
                .distinct()
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TrainerReservationDate> getSessions(Long gymId, Long classTypeId, Long trainerId, LocalDate date) {
        return sessionRepository.findByGymIdAndClassTypeIdAndTrainerIdAndDate(gymId, classTypeId, trainerId, date);
    }

    @Transactional(readOnly = true)
    public ReservationRuleResponse getRules(Long gymId, Long categoryId, Long lessonId) {
        List<ReservationRule> rules = ruleRepository.findByGymIdAndCategoryIdAndLessonTypeIdAndStatus(gymId, categoryId, lessonId, "ACTIVE");

        String htmlContent = rules.stream()
                .map(ReservationRule::getDescription)
                .collect(Collectors.joining("\n"));

        return new ReservationRuleResponse(htmlContent);
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> getMyReservations(Long userId, int page, int pageSize, ReservationStatus status) {
        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by("reservationDate.date").ascending());
        var result = status != null
                ? reservationRepository.findUpcomingByUserIdAndStatus(userId, status, pageable)
                : reservationRepository.findUpcomingByUserId(userId, pageable);
        return result.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ReservationDetailResponse getReservationDetail(Long id, Long userId) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RESERVATION_NOT_FOUND", "error.reservation_not_found"));

        if (!reservation.getUserId().equals(userId)) {
            throw new IllegalArgumentException("ACCESS_DENIED");
        }

        return ReservationDetailResponse.builder()
                .id(reservation.getId())
                .gymId(reservation.getGym().getId())
                .gymName(reservation.getGym().getName())
                .trainerId(reservation.getTrainer().getId())
                .trainerName(reservation.getTrainer().getFirstName() + " " + reservation.getTrainer().getLastName())
                .classType(reservation.getClassType() != null ? reservation.getClassType().getName() : reservation.getLessonType())
                .categoryName(reservation.getCategory() != null ? reservation.getCategory().getName() : null)
                .date(reservation.getReservationDate().getDate())
                .fromHour(reservation.getReservationDate().getStartTime())
                .toHour(reservation.getReservationDate().getEndTime())
                .status(reservation.getStatus())
                .createdAt(reservation.getCreatedDate())
                .approvedAt(reservation.getApprovedAt())
                .cancelledAt(reservation.getCancelledAt())
                .cancelReasonCode(reservation.getCancelReasonCode())
                .cancelReasonText(reservation.getCancelReasonText())
                .cancelAdditionalNote(reservation.getCancelAdditionalNote())
                .build();
    }

    @Transactional(readOnly = true)
    public ReservationWidgetResponse getReservationWidget(Long userId) {
        List<ReservationStatus> activeStatuses = List.of(ReservationStatus.PENDING, ReservationStatus.APPROVED);
        LocalDateTime now = LocalDateTime.now();
        List<ReservationResponse> active = reservationRepository.findByUserId(userId, PageRequest.of(0, 50, Sort.by("reservationDate.date").ascending()))
                .stream()
                .filter(r -> activeStatuses.contains(r.getStatus()))
                .map(this::mapToResponse)
                .filter(r -> {
                    LocalDateTime resDateTime = LocalDateTime.of(r.getDate(), r.getFromHour());
                    return !resDateTime.isBefore(now);
                })
                .limit(2)
                .collect(Collectors.toList());

        return new ReservationWidgetResponse(active);
    }

    @Transactional(readOnly = true)
    public ReservationPreviewResponse getReservationPreview(Long sessionId) {
        TrainerReservationDate session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("SESSION_NOT_FOUND", "error.session_not_found"));

        Long categoryId = session.getClassType() != null && session.getClassType().getCategory() != null
                ? session.getClassType().getCategory().getId() : null;
        Long classTypeId = session.getClassType() != null ? session.getClassType().getId() : null;

        String htmlContent = ruleRepository.findByGymIdAndCategoryIdAndLessonTypeIdAndStatus(session.getGymId(), categoryId, classTypeId, "ACTIVE")
                .stream()
                .map(ReservationRule::getDescription)
                .collect(Collectors.joining("<br/>"));

        return ReservationPreviewResponse.builder()
                .date(session.getDate())
                .fromHour(session.getStartTime())
                .toHour(session.getEndTime())
                .trainerName(session.getTrainer().getFirstName() + " " + session.getTrainer().getLastName())
                .classType(session.getClassType().getName())
                .htmlContent(htmlContent)
                .build();
    }

    @Transactional(readOnly = true)
    public TrainerDailyAvailabilityResponse getTrainerDailyAvailability(Long gymId, Long trainerId, LocalDate date) {
        Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));

        if (!Boolean.TRUE.equals(gym.getIsReservationEnabled())) {
            throw new BadRequestException("GYM_RESERVATION_DISABLED", "error.gym_reservation_disabled");
        }

        Trainer trainer = trainerRepository.findById(trainerId)
                .orElseThrow(() -> new ResourceNotFoundException("TRAINER_NOT_FOUND", "error.trainer_not_found"));

        if (!trainer.getGymId().equals(gymId)) {
            throw new BadRequestException("TRAINER_NOT_IN_GYM", "error.trainer_not_in_gym");
        }

        List<TrainerReservationDate> slots = sessionRepository.findByTrainerIdAndDate(trainerId, date);

        LocalDateTime now = LocalDateTime.now();

        List<GymReservationDetailsResponse.TimeSlotDto> timeSlots = slots.stream()
                .filter(slot -> !LocalDateTime.of(slot.getDate(), slot.getStartTime()).isBefore(now))
                .sorted(Comparator.comparing(TrainerReservationDate::getStartTime))
                .map(slot -> {
                    int booked = reservationRepository.countByReservationDateId(slot.getId());
                    return GymReservationDetailsResponse.TimeSlotDto.builder()
                            .slotId(slot.getId())
                            .startTime(slot.getStartTime())
                            .endTime(slot.getEndTime())
                            .totalSpaces(slot.getEmptySpaces())
                            .bookedSpaces(booked)
                            .availableSpaces(Math.max(0, slot.getEmptySpaces() - booked))
                            .build();
                })
                .filter(dto -> dto.getAvailableSpaces() > 0)
                .collect(Collectors.toList());

        return TrainerDailyAvailabilityResponse.builder()
                .trainerId(trainerId)
                .date(date)
                .timeSlots(timeSlots)
                .build();
    }

    @Transactional(readOnly = true)
    public GymReservationDetailsResponse getReservationDetails(Long gymId) {
        Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));

        List<String> lessonTypes = gymLessonTypeRepository.findByGymId(gymId).stream()
                .map(GymLessonType::getName)
                .collect(Collectors.toList());

        List<Trainer> trainers = gym.getTrainers() != null ? new ArrayList<>(gym.getTrainers()) : List.of();

        List<GymReservationDetailsResponse.TrainerAvailabilityDto> trainerDtos = trainers.stream()
                .map(trainer -> GymReservationDetailsResponse.TrainerAvailabilityDto.builder()
                        .trainerId(trainer.getId())
                        .trainerName(trainer.getName())
                        .trainerSurname(trainer.getSurname())
                        .profileImageUrl(trainer.getProfileImageUrl())
                        .reservationEnabled(Boolean.TRUE.equals(trainer.getIsReservationEnabled()))
                        .build())
                .collect(Collectors.toList());

        return GymReservationDetailsResponse.builder()
                .gymId(gymId)
                .reservationEnabled(Boolean.TRUE.equals(gym.getIsReservationEnabled()))
                .lessonTypes(lessonTypes)
                .trainers(trainerDtos)
                .build();
    }

    @Transactional(readOnly = true)
    public GymReservationStatusResponse getGymReservationStatus(Long gymId) {
        if (!gymRepository.existsById(gymId)) {
            throw new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found");
        }
        boolean hasLessons = gymLessonTypeRepository.existsByGymId(gymId);
        return new GymReservationStatusResponse(hasLessons);
    }

    private ReservationResponse mapToResponse(Reservation reservation) {
        return ReservationResponse.builder()
                .id(reservation.getId())
                .gymId(reservation.getGym().getId())
                .gymName(reservation.getGym().getName())
                .trainerId(reservation.getTrainer().getId())
                .lessonType(reservation.getClassType() != null ? reservation.getClassType().getName() : reservation.getLessonType())
                .categoryName(reservation.getCategory() != null ? reservation.getCategory().getName() : null)
                .date(reservation.getReservationDate().getDate())
                .fromHour(reservation.getReservationDate().getStartTime())
                .toHour(reservation.getReservationDate().getEndTime())
                .status(reservation.getStatus())
                .sessionId(reservation.getReservationDate().getId())
                .build();
    }
}
