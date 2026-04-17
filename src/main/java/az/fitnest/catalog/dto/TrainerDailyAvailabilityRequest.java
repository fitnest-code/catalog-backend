package az.fitnest.catalog.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainerDailyAvailabilityRequest {
    private Long gymId;
    private Long trainerId;
    
    @JsonFormat(pattern = "dd.MM.yyyy")
    private LocalDate date;
}
