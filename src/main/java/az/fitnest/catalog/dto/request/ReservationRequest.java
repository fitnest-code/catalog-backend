package az.fitnest.catalog.dto.request;
import az.fitnest.catalog.dto.response.*;
import az.fitnest.catalog.dto.*;

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

    private Long lessonId;

    private Long trainerId;

    @NotNull(message = "Session ID is required")
    private Long sessionId;
}
