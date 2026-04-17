package az.fitnest.catalog.service.impl;

import az.fitnest.catalog.dto.ReservationRequest;
import az.fitnest.catalog.dto.ReservationResponse;
import az.fitnest.catalog.dto.ReservationStatusUpdateRequest;
import az.fitnest.catalog.exception.BadRequestException;
import az.fitnest.catalog.exception.ResourceNotFoundException;
import az.fitnest.catalog.model.entity.Gym;
import az.fitnest.catalog.model.entity.Reservation;
import az.fitnest.catalog.model.entity.Trainer;
import az.fitnest.catalog.model.entity.TrainerReservationDate;
import az.fitnest.catalog.model.enums.ReservationStatus;
import az.fitnest.catalog.repository.GymRepository;
import az.fitnest.catalog.repository.ReservationRepository;
import az.fitnest.catalog.repository.TrainerRepository;
import az.fitnest.catalog.repository.TrainerReservationDateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final TrainerReservationDateRepository trainerReservationDateRepository;
    private final GymRepository gymRepository;
    private final TrainerRepository trainerRepository;
    private final az.fitnest.catalog.repository.GymLessonTypeRepository gymLessonTypeRepository;

    @Transactional
    public ReservationResponse createReservation(Long userId, Long gymId, ReservationRequest request) {
        Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));

        if (!Boolean.TRUE.equals(gym.getIsReservationEnabled())) {
            throw new BadRequestException("GYM_RESERVATION_DISABLED", "error.gym_reservation_disabled");
        }

        Trainer trainer = trainerRepository.findById(request.getTrainerId())
                .orElseThrow(() -> new ResourceNotFoundException("TRAINER_NOT_FOUND", "error.trainer_not_found"));

        if (!trainer.getGymId().equals(gymId)) {
            throw new BadRequestException("TRAINER_NOT_IN_GYM", "error.trainer_not_in_gym");
        }

        if (!Boolean.TRUE.equals(trainer.getIsReservationEnabled())) {
            throw new BadRequestException("TRAINER_RESERVATION_DISABLED", "error.trainer_reservation_disabled");
        }

        String[] times = request.getTimeInterval().split("-");
        if (times.length != 2) {
            throw new BadRequestException("INVALID_TIME_INTERVAL", "error.invalid_time_interval");
        }

        LocalTime startTime = LocalTime.parse(times[0].trim());
        LocalTime endTime = LocalTime.parse(times[1].trim());

        TrainerReservationDate reservationDate = trainerReservationDateRepository
                .findByTrainerIdAndDateAndStartTimeAndEndTime(request.getTrainerId(), request.getDate(), startTime, endTime)
                .orElseThrow(() -> new ResourceNotFoundException("TRAINER_AVAILABILITY_NOT_FOUND", "error.trainer_availability_not_found"));

        int currentBookings = reservationRepository.findByTrainerIdAndReservationDateId(trainer.getId(), reservationDate.getId()).size();
        if (currentBookings >= reservationDate.getEmptySpaces()) {
            throw new BadRequestException("NO_EMPTY_SPACES", "error.no_empty_spaces");
        }

        if (reservationRepository.existsByUserIdAndReservationDateId(userId, reservationDate.getId())) {
            throw new BadRequestException("ALREADY_RESERVED", "error.already_reserved");
        }

        Reservation reservation = Reservation.builder()
                .userId(userId)
                .gym(gym)
                .trainer(trainer)
                .reservationDate(reservationDate)
                .lessonType(request.getLessonType())
                .status(ReservationStatus.PENDING)
                .build();

        reservation = reservationRepository.save(reservation);

        return mapToResponse(reservation);
    }

    @Transactional(readOnly = true)
    public Page<ReservationResponse> getUserReservations(Long userId, int page, int pageSize) {
        PageRequest pageRequest = PageRequest.of(Math.max(0, page - 1), pageSize, Sort.by(Sort.Direction.DESC, "createdDate"));
        return reservationRepository.findByUserId(userId, pageRequest).map(this::mapToResponse);
    }

    @Transactional
    public void updateReservationStatus(Long reservationId, ReservationStatusUpdateRequest request) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("RESERVATION_NOT_FOUND", "error.reservation_not_found"));

        reservation.setStatus(request.getStatus());
        reservationRepository.save(reservation);
    }

    @Transactional
    public void cancelReservation(Long userId, Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("RESERVATION_NOT_FOUND", "error.reservation_not_found"));

        if (!reservation.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("RESERVATION_NOT_FOUND", "error.reservation_not_found");
        }

        // 24 hour rule check
        java.time.LocalDateTime lessonStart = java.time.LocalDateTime.of(
                reservation.getReservationDate().getDate(),
                reservation.getReservationDate().getStartTime()
        );

        if (java.time.LocalDateTime.now().plusHours(24).isAfter(lessonStart)) {
            throw new BadRequestException("LATE_CANCELLATION", "error.late_cancellation");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(reservation);
    }

    @Transactional(readOnly = true)
    public az.fitnest.catalog.dto.TrainerDailyAvailabilityResponse getTrainerDailyAvailability(Long gymId, Long trainerId, LocalDate date) {
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

        if (!Boolean.TRUE.equals(trainer.getIsReservationEnabled())) {
            throw new BadRequestException("TRAINER_RESERVATION_DISABLED", "error.trainer_reservation_disabled");
        }

        List<TrainerReservationDate> slots = trainerReservationDateRepository.findByTrainerIdAndDate(trainerId, date);

        List<az.fitnest.catalog.dto.GymReservationDetailsResponse.TimeSlotDto> timeSlots = slots.stream()
                .sorted(java.util.Comparator.comparing(TrainerReservationDate::getStartTime))
                .map(slot -> {
                    int booked = reservationRepository.countByReservationDateId(slot.getId());
                    return az.fitnest.catalog.dto.GymReservationDetailsResponse.TimeSlotDto.builder()
                            .slotId(slot.getId())
                            .startTime(slot.getStartTime())
                            .endTime(slot.getEndTime())
                            .totalSpaces(slot.getEmptySpaces())
                            .bookedSpaces(booked)
                            .availableSpaces(Math.max(0, slot.getEmptySpaces() - booked))
                            .build();
                })
                .collect(Collectors.toList());

        return az.fitnest.catalog.dto.TrainerDailyAvailabilityResponse.builder()
                .trainerId(trainerId)
                .date(date)
                .timeSlots(timeSlots)
                .build();
    }

    @Transactional(readOnly = true)
    public az.fitnest.catalog.dto.GymReservationDetailsResponse getReservationDetails(Long gymId) {
        Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));

        // Lesson types
        List<String> lessonTypes = gymLessonTypeRepository.findByGymId(gymId).stream()
                .map(az.fitnest.catalog.model.entity.GymLessonType::getName)
                .collect(Collectors.toList());

        // Trainers with reservation status info
        List<Trainer> trainers = gym.getTrainers() != null ? new ArrayList<>(gym.getTrainers()) : List.of();

        List<az.fitnest.catalog.dto.GymReservationDetailsResponse.TrainerAvailabilityDto> trainerDtos = trainers.stream()
                .map(trainer -> az.fitnest.catalog.dto.GymReservationDetailsResponse.TrainerAvailabilityDto.builder()
                        .trainerId(trainer.getId())
                        .trainerName(trainer.getName())
                        .trainerSurname(trainer.getSurname())
                        .profileImageUrl(trainer.getProfileImageUrl())
                        .reservationEnabled(Boolean.TRUE.equals(trainer.getIsReservationEnabled()))
                        .build())
                .collect(Collectors.toList());

        return az.fitnest.catalog.dto.GymReservationDetailsResponse.builder()
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
                .lessonType(reservation.getLessonType())
                .date(reservation.getReservationDate().getDate())
                .timeInterval(reservation.getReservationDate().getStartTime() + " - " + reservation.getReservationDate().getEndTime())
                .status(reservation.getStatus())
                .build();
    }
}

