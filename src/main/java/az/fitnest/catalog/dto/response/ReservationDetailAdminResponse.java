package az.fitnest.catalog.dto.response;

import az.fitnest.catalog.model.enums.ReservationStatus;
import java.time.LocalDateTime;

public record ReservationDetailAdminResponse(
    Long id,
    Long userId,
    String userFullName,
    String userPhone,
    String userEmail,
    String birthDate,
    String registrationDate,
    String platform,
    ReservationStatus status,
    String trainerName,
    String lessonType,
    String date,
    String timeRange,
    String cancelReason
) {}
