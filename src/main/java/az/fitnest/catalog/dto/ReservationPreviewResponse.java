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
public class ReservationPreviewResponse {
    private LocalDate date;
    @Schema(type = "string", example = "22:00")
    private LocalTime fromHour;
    @Schema(type = "string", example = "23:00")
    private LocalTime toHour;
    private String trainerName;
    private String classType;
    private String htmlContent;
}
