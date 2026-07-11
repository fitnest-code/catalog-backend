package az.fitnest.catalog.dto.response;

import az.fitnest.catalog.model.enums.SessionStatus;
import java.time.LocalDate;
import java.time.LocalTime;

public record LessonHourResponse(
    Long id,
    String trainerName,
    String lessonTypeName,
    LocalDate date,
    String timeRange,
    Integer emptySpaces,
    SessionStatus status,
    Integer pendingReservations,
    Integer approvedReservations
) {}
