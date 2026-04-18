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
public class ReservationPreviewResponse {
    private LocalDate date;
    private String timeInterval;
    private String trainerName;
    private String classType;
    private List<String> rules;
}
