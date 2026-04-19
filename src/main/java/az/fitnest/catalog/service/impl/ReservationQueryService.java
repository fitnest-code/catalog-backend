package az.fitnest.catalog.service.impl;

import az.fitnest.catalog.dto.*;
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
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReservationQueryService {

    private final CategoryRepository categoryRepository;
    private final GymLessonTypeRepository gymLessonTypeRepository;
    private final TrainerReservationDateRepository sessionRepository;
    private final ReservationRuleRepository ruleRepository;
    private final ReservationRepository reservationRepository;
    private final GymRepository gymRepository;
    private final TrainerRepository trainerRepository;

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
        // Here we assume trainers are linked to the gym and specialization might match lesson type
        // For now, returning all trainers in the gym as requested flow implies they are available for the selected lesson context
        return trainerRepository.findByGymId(gymId, PageRequest.of(0, 100)).getContent().stream()
                .map(t -> ReservationTeacherResponse.builder()
                        .teacherId(t.getId())
                        .teacherName(t.getFirstName() + " " + t.getLastName())
                        .teacherImageProfileUrl(t.getProfileImageUrl())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DayAvailabilityResponse> getAvailabilityForTeacher(Long gymId, Long categoryId, Long lessonTypeId, Long trainerId) {
        List<TrainerReservationDate> sessions = sessionRepository.findByTrainerIdOrderByDateAscStartTimeAsc(trainerId);
        
        return sessions.stream()
                .collect(Collectors.groupingBy(TrainerReservationDate::getDate))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> DayAvailabilityResponse.builder()
                        .date(entry.getKey())
                        .slots(entry.getValue().stream().map(session -> {
                            int activeBookings = reservationRepository.countActiveReservations(session.getId(), 
                                List.of(ReservationStatus.PENDING, ReservationStatus.APPROVED));
                            int emptySpaces = Math.max(0, session.getEmptySpaces() - activeBookings);
                            
                            return TimeSlotResponse.builder()
                                    .startTime(session.getStartTime())
                                    .endTime(session.getEndTime())
                                    .emptySpaces(emptySpaces)
                                    .build();
                        }).collect(Collectors.toList()))
                        .build())
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
        // You mentioned adding lessonId to the body, currently fetching rules by gym and category
        List<ReservationRule> rules = ruleRepository.findByGymIdAndCategoryIdAndStatus(gymId, categoryId, "ACTIVE");

        String htmlContent = rules.stream()
                .map(ReservationRule::getDescription)
                .collect(Collectors.joining("\n"));

        return new ReservationRuleResponse(htmlContent);
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> getMyReservations(Long userId, int page, int pageSize) {
        return reservationRepository.findByUserId(userId, PageRequest.of(page - 1, pageSize, Sort.by("createdDate").descending()))
                .stream()
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
        List<ReservationResponse> active = reservationRepository.findByUserId(userId, PageRequest.of(0, 2, Sort.by("reservationDate.date").ascending()))
                .stream()
                .filter(r -> activeStatuses.contains(r.getStatus()))
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return new ReservationWidgetResponse(active);
    }

    @Transactional(readOnly = true)
    public ReservationPreviewResponse getReservationPreview(Long sessionId) {
        TrainerReservationDate session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("SESSION_NOT_FOUND", "error.session_not_found"));

        Long categoryId = session.getClassType() != null && session.getClassType().getCategory() != null
                ? session.getClassType().getCategory().getId() : null;

        List<String> rules = ruleRepository.findByGymIdAndCategoryIdAndStatus(session.getGymId(), categoryId, "ACTIVE")
                .stream()
                .map(ReservationRule::getDescription)
                .collect(Collectors.toList());

        return ReservationPreviewResponse.builder()
                .date(session.getDate())
                .fromHour(session.getStartTime())
                .toHour(session.getEndTime())
                .trainerName(session.getTrainer().getFirstName() + " " + session.getTrainer().getLastName())
                .classType(session.getClassType().getName())
                .rules(rules)
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

        List<GymReservationDetailsResponse.TimeSlotDto> timeSlots = slots.stream()
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

    private ReservationResponse mapToResponse(Reservation reservation) {
        return ReservationResponse.builder()
                .id(reservation.getId())
                .gymId(reservation.getGym().getId())
                .trainerId(reservation.getTrainer().getId())
                .lessonType(reservation.getClassType() != null ? reservation.getClassType().getName() : reservation.getLessonType())
                .date(reservation.getReservationDate().getDate())
                .fromHour(reservation.getReservationDate().getStartTime())
                .toHour(reservation.getReservationDate().getEndTime())
                .status(reservation.getStatus())
                .build();
    }
}
