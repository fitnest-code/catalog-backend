package az.fitnest.catalog.service.impl;

import az.fitnest.catalog.configuration.ReservationBookingProperties;
import az.fitnest.catalog.dto.*;
import az.fitnest.catalog.dto.request.*;
import az.fitnest.catalog.dto.response.*;
import az.fitnest.catalog.client.OrderServiceGrpcClient;
import az.fitnest.catalog.service.TranslationService;
import az.fitnest.catalog.client.UserServiceGrpcClient;
import az.fitnest.catalog.util.UserContext;
import az.fitnest.catalog.client.CachedUser;
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

import java.time.Clock;
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

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ReservationQueryServiceImpl.class);

    private final CategoryRepository categoryRepository;
    private final GymLessonTypeRepository gymLessonTypeRepository;
    private final TrainerReservationDateRepository sessionRepository;
    private final ReservationRuleRepository ruleRepository;
    private final ReservationRepository reservationRepository;
    private final GymRepository gymRepository;
    private final TrainerRepository trainerRepository;
    private final OrderServiceGrpcClient orderServiceClient;
    private final TranslationService translationService;
    private final UserServiceGrpcClient userServiceGrpcClient;
    private final Clock clock;
    private final ReservationBookingProperties bookingProperties;

    @Transactional(readOnly = true)
    public List<ReservationLessonResponse> getLessonsForReservation(Long gymId, Long categoryId) {
        List<GymLessonType> lessonTypes;
        if (categoryId != null) {
            lessonTypes = gymLessonTypeRepository.findByGymIdAndCategoryIdAndStatus(gymId, categoryId, "ACTIVE");
        } else {
            lessonTypes = gymLessonTypeRepository.findByGymIdAndStatus(gymId, "ACTIVE");
        }
        return lessonTypes.stream()
                .map(lt -> ReservationLessonResponse.builder()
                        .gymId(gymId)
                        .categoryId(lt.getCategory() != null ? lt.getCategory().getId() : null)
                        .lessonId(lt.getId())
                        .lessonName(lt.getName())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReservationTeacherResponse> getTrainersForLesson(Long gymId, Long categoryId, Long lessonTypeId) {
        return trainerRepository.findByGymId(gymId, PageRequest.of(0, 100)).getContent().stream()
                .filter(t -> {
                    if (lessonTypeId != null) {
                        return t.getEnabledLessonTypes().stream().anyMatch(lt -> lt.getId().equals(lessonTypeId));
                    }
                    if (categoryId != null) {
                        return t.getEnabledLessonTypes().stream().anyMatch(lt -> lt.getCategory() != null && lt.getCategory().getId().equals(categoryId));
                    }
                    return true;
                })
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
        log.info("[Availability] userId={}, gymId={}, categoryId={}, lessonTypeId={}, trainerId={}",
                userId, gymId, categoryId, lessonTypeId, trainerId);
        List<TrainerReservationDate> sessions;
        if (trainerId != null) {
            sessions = sessionRepository.findByTrainerIdOrderByDateAscStartTimeAsc(trainerId);
        } else {
            sessions = new java.util.ArrayList<>(sessionRepository.findByGymId(gymId));
            sessions.sort(java.util.Comparator.comparing(TrainerReservationDate::getDate)
                    .thenComparing(TrainerReservationDate::getStartTime));
        }

        Gym gym = gymRepository.findById(gymId).orElse(null);
        boolean isGymEnabled = gym != null && Boolean.TRUE.equals(gym.getIsReservationEnabled());
        log.info("[Availability] Gym: exists={}, isReservationEnabled={}", gym != null, isGymEnabled);

        az.fitnest.order.grpc.ActiveSubscriptionResponse subscription = null;
        if (userId != null && isGymEnabled) {
            try {
                subscription = orderServiceClient.getActiveSubscription(userId);
                log.info("[Availability] Retrieved subscription details: status={}, packageId={}, packageName={}",
                        subscription != null ? subscription.getSubscriptionStatus() : "null",
                        subscription != null ? subscription.getPackageId() : "null",
                        subscription != null ? subscription.getPackageName() : "null");
            } catch (Exception ex) {
                log.error("[Availability] Error retrieving subscription for userId={}: {}", userId, ex.getMessage(), ex);
            }
        }

        final az.fitnest.order.grpc.ActiveSubscriptionResponse sub = subscription;
        boolean isPkgSupported = false;
        if (sub != null && gym != null) {
            java.util.List<Long> gymPackageIds = gym.getSubscriptions().stream()
                    .map(s -> s.getPackageId())
                    .filter(java.util.Objects::nonNull)
                    .toList();
            isPkgSupported = isPackageSufficient(sub.getPackageId(), gymPackageIds);
            log.info("[Availability] Package check: userPackageId={}, gymPackageIds={}, isPkgSupported={}",
                    sub.getPackageId(), gymPackageIds, isPkgSupported);
        } else {
            log.info("[Availability] Package check skipped: subIsPresent={}, gymIsPresent={}", sub != null, gym != null);
        }
        final boolean isPackageSupported = isPkgSupported;
        final boolean isSubscriptionActive = sub != null && "Active".equalsIgnoreCase(sub.getSubscriptionStatus());
        log.info("[Availability] Final flags: isSubscriptionActive={}, isPackageSupported={}", isSubscriptionActive, isPackageSupported);

        LocalDateTime cutoff = LocalDateTime.now(clock)
                .plusHours(bookingProperties.getMinHoursBeforeStart());
        log.info("[Availability] cutoff={}, now={}", cutoff, LocalDateTime.now(clock));

        List<TrainerReservationDate> filteredSessions = sessions.stream()
                .filter(session -> {
                    if (lessonTypeId != null && (session.getClassType() == null || !session.getClassType().getId().equals(lessonTypeId))) {
                        return false;
                    }
                    if (categoryId != null && (session.getClassType() == null || session.getClassType().getCategory() == null || !session.getClassType().getCategory().getId().equals(categoryId))) {
                        return false;
                    }
                    LocalDateTime sessionStart = LocalDateTime.of(session.getDate(), session.getStartTime());
                    return !sessionStart.isBefore(cutoff);
                })
                .toList();

        if (filteredSessions.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        List<Long> sessionIds = filteredSessions.stream().map(TrainerReservationDate::getId).toList();
        List<Object[]> activeBookingCounts = reservationRepository.countActiveReservationsForSessions(
                sessionIds, List.of(ReservationStatus.PENDING, ReservationStatus.APPROVED));
        Map<Long, Integer> activeBookingMap = activeBookingCounts.stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> ((Number) row[1]).intValue()
                ));

        List<Reservation> userOverlapReservations = java.util.Collections.emptyList();
        if (userId != null && isGymEnabled) {
            List<LocalDate> dates = filteredSessions.stream().map(TrainerReservationDate::getDate).distinct().toList();
            userOverlapReservations = reservationRepository.findReservationsForOverlapCheck(
                    userId, dates, List.of(ReservationStatus.PENDING, ReservationStatus.APPROVED));
        }

        final List<Reservation> overlapCheckReservations = userOverlapReservations;

        return filteredSessions.stream()
                .collect(Collectors.groupingBy(TrainerReservationDate::getDate))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    LocalDate date = entry.getKey();
                    return DayAvailabilityResponse.builder()
                        .date(date)
                        .slots(entry.getValue().stream().map(session -> {
                            int activeBookings = activeBookingMap.getOrDefault(session.getId(), 0);
                            int emptySpaces = Math.max(0, session.getEmptySpaces() - activeBookings);

                            boolean isAcceptable = isGymEnabled;
                            String rejectReason = null;
                            if (!isGymEnabled) {
                                rejectReason = "GYM_RESERVATION_DISABLED";
                            }

                            if (emptySpaces <= 0) {
                                isAcceptable = false;
                                rejectReason = "NO_EMPTY_SPACES";
                            }

                            if (userId != null && isAcceptable) {
                                if (!isSubscriptionActive) {
                                    isAcceptable = false;
                                    rejectReason = "SUBSCRIPTION_NOT_ACTIVE";
                                } else if (!isPackageSupported) {
                                    isAcceptable = false;
                                    rejectReason = "PACKAGE_NOT_SUPPORTED";
                                } else {
                                    boolean hasOverlap = overlapCheckReservations.stream().anyMatch(r -> 
                                            r.getReservationDate() != null &&
                                            r.getReservationDate().getDate().equals(date) &&
                                            r.getReservationDate().getStartTime().isBefore(session.getEndTime()) &&
                                            r.getReservationDate().getEndTime().isAfter(session.getStartTime())
                                    );
                                    if (hasOverlap) {
                                        isAcceptable = false;
                                        rejectReason = "RESERVATION_OVERLAP";
                                    }
                                }
                            }

                            if (!isAcceptable) {
                                log.info("[Availability] TimeSlot not acceptable: sessionId={}, date={}, time={}-{}, reason={}",
                                        session.getId(), date, session.getStartTime(), session.getEndTime(), rejectReason);
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
        List<ReservationRule> rules = new ArrayList<>();
        if (categoryId != null && lessonId != null) {
            rules = ruleRepository.findByGymIdAndCategoryIdAndLessonTypeIdAndStatus(gymId, categoryId, lessonId, "ACTIVE");
        }
        if (rules.isEmpty()) {
            // Fallback to gym-wide rules (both category and lesson null)
            rules = ruleRepository.findByGymIdAndCategoryIdIsNullAndLessonTypeIsNullAndStatus(gymId, "ACTIVE");
        }
        if (rules.isEmpty()) {
            rules = ruleRepository.findByGymIdAndStatusOrderBySortOrderAsc(gymId, "ACTIVE");
            if (lessonId != null) {
                rules = rules.stream()
                        .filter(r -> r.getLessonType() != null && r.getLessonType().getId().equals(lessonId))
                        .collect(Collectors.toList());
            } else if (categoryId != null) {
                rules = rules.stream()
                        .filter(r -> r.getCategory() != null && r.getCategory().getId().equals(categoryId))
                        .collect(Collectors.toList());
            }
        }

        String userLanguage = resolveUserLanguage();
        String htmlContent = rules.stream()
                .map(r -> {
                    String translated = translationService.getTranslatedValue("RESERVATION_RULE", String.valueOf(r.getId()), "description", userLanguage);
                    return (translated == null || translated.isEmpty()) ? r.getDescription() : translated;
                })
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
                .trainerId(reservation.getTrainer() != null ? reservation.getTrainer().getId() : null)
                .trainerName(reservation.getTrainer() != null ? reservation.getTrainer().getFirstName() + " " + reservation.getTrainer().getLastName() : null)
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

        List<ReservationRule> rules = new ArrayList<>();
        if (categoryId != null && classTypeId != null) {
            rules = ruleRepository.findByGymIdAndCategoryIdAndLessonTypeIdAndStatus(session.getGymId(), categoryId, classTypeId, "ACTIVE");
        }
        if (rules.isEmpty()) {
            rules = ruleRepository.findByGymIdAndCategoryIdIsNullAndLessonTypeIsNullAndStatus(session.getGymId(), "ACTIVE");
        }
        if (rules.isEmpty()) {
            rules = ruleRepository.findByGymIdAndStatusOrderBySortOrderAsc(session.getGymId(), "ACTIVE");
            if (classTypeId != null) {
                rules = rules.stream()
                        .filter(r -> r.getLessonType() != null && r.getLessonType().getId().equals(classTypeId))
                        .collect(Collectors.toList());
            } else if (categoryId != null) {
                rules = rules.stream()
                        .filter(r -> r.getCategory() != null && r.getCategory().getId().equals(categoryId))
                        .collect(Collectors.toList());
            }
        }

        String userLanguage = resolveUserLanguage();
        String htmlContent = rules.stream()
                .map(r -> {
                    String translated = translationService.getTranslatedValue("RESERVATION_RULE", String.valueOf(r.getId()), "description", userLanguage);
                    return (translated == null || translated.isEmpty()) ? r.getDescription() : translated;
                })
                .collect(Collectors.joining("<br/>"));

        String trainerName = session.getTrainer() != null 
                ? session.getTrainer().getFirstName() + " " + session.getTrainer().getLastName() 
                : null;

        String classTypeName = session.getClassType() != null 
                ? session.getClassType().getName() 
                : null;

        return ReservationPreviewResponse.builder()
                .date(session.getDate())
                .fromHour(session.getStartTime())
                .toHour(session.getEndTime())
                .trainerName(trainerName)
                .classType(classTypeName)
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

        LocalDateTime now = LocalDateTime.now(clock);

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
        boolean isEnabled = sessionRepository.existsByGymId(gymId);
        return new GymReservationStatusResponse(isEnabled);
    }

    private ReservationResponse mapToResponse(Reservation reservation) {
        return ReservationResponse.builder()
                .id(reservation.getId())
                .gymId(reservation.getGym().getId())
                .gymName(reservation.getGym().getName())
                .trainerId(reservation.getTrainer() != null ? reservation.getTrainer().getId() : null)
                .lessonType(reservation.getClassType() != null ? reservation.getClassType().getName() : reservation.getLessonType())
                .categoryName(reservation.getCategory() != null ? reservation.getCategory().getName() : null)
                .date(reservation.getReservationDate().getDate())
                .fromHour(reservation.getReservationDate().getStartTime())
                .toHour(reservation.getReservationDate().getEndTime())
                .status(reservation.getStatus())
                .sessionId(reservation.getReservationDate().getId())
                .build();
    }

    private String resolveUserLanguage() {
        return az.fitnest.catalog.util.UserContext.getUserLanguage();
    }

    private String resolveUserLanguage(Long userId) {
        return az.fitnest.catalog.util.UserContext.getUserLanguage();
    }

    private boolean isPackageSufficient(Long userPackageId, java.util.List<Long> gymPackageIds) {
        if (userPackageId == null || gymPackageIds == null || gymPackageIds.isEmpty()) {
            log.info("[isPackageSufficient] Invalid inputs: userPackageId={}, gymPackageIds={}", userPackageId, gymPackageIds);
            return false;
        }
        try {
            java.util.List<az.fitnest.order.grpc.SubscriptionPackageInfo> allPlans = orderServiceClient.getGymPlans();
            log.info("[isPackageSufficient] Retrieved allPlans count: {}", allPlans != null ? allPlans.size() : "null");
            if (allPlans != null) {
                for (var p : allPlans) {
                    log.info("[isPackageSufficient] Plan from allPlans: id={}, name={}", p.getPackageId(), p.getName());
                }
            }
            
            // Get user package rank
            String userPackageName = null;
            for (var plan : allPlans) {
                if (plan.getPackageId() == userPackageId.longValue()) {
                    userPackageName = plan.getName();
                    break;
                }
            }
            if (userPackageName == null) {
                java.util.List<az.fitnest.order.grpc.PackageNameInfo> nameInfos = orderServiceClient.getPackageNamesByIds(java.util.List.of(userPackageId));
                log.info("[isPackageSufficient] Fallback packageNames for user: {}", nameInfos);
                if (!nameInfos.isEmpty()) {
                    userPackageName = nameInfos.get(0).getName();
                }
            }
            int userRank = getPackageRank(userPackageName);
            log.info("[isPackageSufficient] User package rank: name={}, rank={}", userPackageName, userRank);

            // Check if any supported gym package has rank <= user package rank
            for (Long gymPkgId : gymPackageIds) {
                String gymPkgName = null;
                for (var plan : allPlans) {
                    if (plan.getPackageId() == gymPkgId.longValue()) {
                        gymPkgName = plan.getName();
                        break;
                    }
                }
                if (gymPkgName == null) {
                    java.util.List<az.fitnest.order.grpc.PackageNameInfo> nameInfos = orderServiceClient.getPackageNamesByIds(java.util.List.of(gymPkgId));
                    log.info("[isPackageSufficient] Fallback packageNames for gym pkg {}: {}", gymPkgId, nameInfos);
                    if (!nameInfos.isEmpty()) {
                        gymPkgName = nameInfos.get(0).getName();
                    }
                }
                int gymRank = getPackageRank(gymPkgName);
                log.info("[isPackageSufficient] Gym package rank for pkgId={}: name={}, rank={}", gymPkgId, gymPkgName, gymRank);
                if (gymRank > 0 && gymRank <= userRank) {
                    log.info("[isPackageSufficient] Match found! gymRank {} <= userRank {}", gymRank, userRank);
                    return true;
                }
            }
        } catch (Exception e) {
            log.error("[isPackageSufficient] Exception occurred during ranking checks: {}", e.getMessage(), e);
            return gymPackageIds.contains(userPackageId);
        }
        boolean exactContains = gymPackageIds.contains(userPackageId);
        log.info("[isPackageSufficient] No match by rank. Fallback to exact contains: {}", exactContains);
        return exactContains;
    }

    private int getPackageRank(String packageName) {
        if (packageName == null) {
            return 0;
        }
        String lower = packageName.toLowerCase();
        if (lower.contains("platinum")) {
            return 4;
        }
        if (lower.contains("gold")) {
            return 3;
        }
        if (lower.contains("silver")) {
            return 2;
        }
        if (lower.contains("bronze")) {
            return 1;
        }
        return 0;
    }
}
