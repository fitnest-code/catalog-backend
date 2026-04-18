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
    @NotNull(message = "Gym ID is required")
    private Long gymId;

    @NotNull(message = "Category name is required (PILATES/YOGA)")
    private String categoryName;

    @NotNull(message = "Class type ID is required")
    private Long classTypeId;

    @NotNull(message = "Trainer ID is required")
    private Long trainerId;

    @NotNull(message = "Session ID is required")
    private Long sessionId;

    private String lessonType;
    private java.time.LocalDate date;
    private String timeInterval;
}
