package az.fitnest.catalog.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationRequest {
    @NotNull(message = "Lesson type is required")
    private String lessonType;

    @NotNull(message = "Trainer ID is required")
    private Long trainerId;

    @NotNull(message = "Date is required")
    private LocalDate date;

    @NotNull(message = "Time interval is required (e.g. 09:00-10:00)")
    private String timeInterval;
}
