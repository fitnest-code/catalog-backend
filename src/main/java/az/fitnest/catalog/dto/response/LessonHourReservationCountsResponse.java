package az.fitnest.catalog.dto.response;

public record LessonHourReservationCountsResponse(
    long pendingCount,
    long approvedCount
) {}
