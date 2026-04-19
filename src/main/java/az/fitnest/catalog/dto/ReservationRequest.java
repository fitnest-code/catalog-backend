package az.fitnest.catalog.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationRequest {
    @NotNull(message = "Gym ID is required")
    private Long gymId;

    @NotNull(message = "Category ID is required")
    private Long categoryId;

    @NotNull(message = "Class type ID is required")
    private Long classTypeId;

    @NotNull(message = "Trainer ID is required")
    private Long trainerId;

    @NotNull(message = "Session ID is required")
    private Long sessionId;

    private String lessonType;
    @Schema(example = "2024-04-25")
    private java.time.LocalDate date;
    @NotNull
    @Schema(type = "string", example = "22:00")
    private LocalTime fromHour;
    @NotNull
    @Schema(type = "string", example = "23:00")
    private LocalTime toHour;
}
