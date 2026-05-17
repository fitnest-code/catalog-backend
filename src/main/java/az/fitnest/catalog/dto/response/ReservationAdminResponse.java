package az.fitnest.catalog.dto.response;

import az.fitnest.catalog.model.enums.ReservationStatus;
import java.time.LocalDateTime;

public record ReservationAdminResponse(
    Long id,
    String userFullName,
    String date,
    String timeRange,
    ReservationStatus status,
    String trainerName
) {}
