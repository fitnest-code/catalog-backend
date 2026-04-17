package az.fitnest.catalog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GymReservationDetailsResponse {
    private Long gymId;
    private boolean reservationEnabled;
    private List<String> lessonTypes;
    private List<TrainerAvailabilityDto> trainers;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrainerAvailabilityDto {
        private Long trainerId;
        private String trainerName;
        private String trainerSurname;
        private String profileImageUrl;
        private boolean reservationEnabled;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeSlotDto {
        private Long slotId;
        private LocalTime startTime;
        private LocalTime endTime;
        private int totalSpaces;
        private int bookedSpaces;
        private int availableSpaces;
    }
}
