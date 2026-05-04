package az.fitnest.catalog.service;

import az.fitnest.catalog.dto.*;
import az.fitnest.catalog.model.entity.Trainer;
import az.fitnest.catalog.model.entity.TrainerReservationDate;
import az.fitnest.catalog.model.enums.ReservationStatus;
import java.time.LocalDate;
import java.util.List;

public interface ReservationQueryService {
    List<ReservationLessonResponse> getLessonsForReservation(Long gymId, Long categoryId);
    List<ReservationTeacherResponse> getTrainersForLesson(Long gymId, Long categoryId, Long lessonTypeId);
    List<DayAvailabilityResponse> getAvailabilityForTeacher(Long userId, Long gymId, Long categoryId, Long lessonTypeId, Long trainerId);
    List<Trainer> getTrainers(Long gymId, Long classTypeId, LocalDate date);
    List<TrainerReservationDate> getSessions(Long gymId, Long classTypeId, Long trainerId, LocalDate date);
    ReservationRuleResponse getRules(Long gymId, Long categoryId, Long lessonId);
    List<ReservationResponse> getMyReservations(Long userId, int page, int pageSize, ReservationStatus status);
    ReservationDetailResponse getReservationDetail(Long id, Long userId);
    ReservationWidgetResponse getReservationWidget(Long userId);
    ReservationPreviewResponse getReservationPreview(Long sessionId);
    TrainerDailyAvailabilityResponse getTrainerDailyAvailability(Long gymId, Long trainerId, LocalDate date);
    GymReservationDetailsResponse getReservationDetails(Long gymId);
    GymReservationStatusResponse getGymReservationStatus(Long gymId);
}
