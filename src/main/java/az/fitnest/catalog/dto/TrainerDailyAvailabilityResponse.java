package az.fitnest.catalog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainerDailyAvailabilityResponse {
    private Long trainerId;
    private LocalDate date;
    private List<GymReservationDetailsResponse.TimeSlotDto> timeSlots;
}
