package az.fitnest.catalog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainerDailyAvailabilityResponse {
    private Long trainerId;
    @Schema(example = "2024-04-25")
    private LocalDate date;
    private List<GymReservationDetailsResponse.TimeSlotDto> timeSlots;
}
