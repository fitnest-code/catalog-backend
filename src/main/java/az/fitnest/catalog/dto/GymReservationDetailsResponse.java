package az.fitnest.catalog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

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
        @Schema(type = "string", example = "22:00")
        private LocalTime startTime;
        @Schema(type = "string", example = "23:00")
        private LocalTime endTime;
        private int totalSpaces;
        private int bookedSpaces;
        private int availableSpaces;
    }
}
