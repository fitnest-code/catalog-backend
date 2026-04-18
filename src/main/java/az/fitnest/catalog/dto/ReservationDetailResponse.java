package az.fitnest.catalog.dto;

import az.fitnest.catalog.model.enums.ReservationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationDetailResponse {
    private Long id;
    private Long gymId;
    private String gymName;
    private Long trainerId;
    private String trainerName;
    private String classType;
    private String categoryName;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private ReservationStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime approvedAt;
    private LocalDateTime cancelledAt;
    private String cancelReasonCode;
    private String cancelReasonText;
    private String cancelAdditionalNote;
}
