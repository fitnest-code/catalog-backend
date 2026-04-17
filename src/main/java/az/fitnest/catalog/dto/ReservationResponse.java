package az.fitnest.catalog.dto;

import az.fitnest.catalog.model.enums.ReservationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationResponse {
    private Long id;
    private Long gymId;
    private Long trainerId;
    private String lessonType;
    private LocalDate date;
    private String timeInterval;
    private ReservationStatus status;
}
