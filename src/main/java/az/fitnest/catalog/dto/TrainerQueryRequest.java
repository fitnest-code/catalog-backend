package az.fitnest.catalog.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainerQueryRequest {
    @NotNull
    private Long gymId;
    @NotNull
    private Long classTypeId;
    @NotNull
    private LocalDate date;
}
