package az.fitnest.catalog.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationRequest {
    @NotNull(message = "Gym ID is required")
    private Long gymId;

    @NotNull(message = "Category ID is required")
    private Long categoryId;

    @NotNull(message = "Lesson ID is required")
    private Long lessonId;

    @NotNull(message = "Trainer ID is required")
    private Long trainerId;

    @NotNull(message = "Session ID is required")
    private Long sessionId;
}
