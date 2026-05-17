package az.fitnest.catalog.dto.request;
import az.fitnest.catalog.dto.response.*;
import az.fitnest.catalog.dto.*;

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
public class TrainerAvailabilityRequest {
    @NotNull(message = "Date is required")
    @Schema(example = "2024-04-25")
    private LocalDate date;

    @NotNull(message = "Start time is required")
    @Schema(type = "string", example = "22:00")
    private LocalTime startTime;

    @NotNull(message = "End time is required")
    @Schema(type = "string", example = "23:00")
    private LocalTime endTime;

    @NotNull(message = "Empty spaces is required")
    private Integer emptySpaces;
}
