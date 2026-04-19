package az.fitnest.catalog.dto;

import az.fitnest.catalog.model.enums.ReservationStatus;
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
public class ReservationResponse {
    private Long id;
    private Long gymId;
    private String gymName;
    private Long trainerId;
    private String lessonType;
    private String categoryName;
    @Schema(example = "2024-04-25")
    private LocalDate date;
    @Schema(type = "string", example = "22:00")
    private LocalTime fromHour;
    @Schema(type = "string", example = "23:00")
    private LocalTime toHour;
    private ReservationStatus status;
}
