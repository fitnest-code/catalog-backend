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
public class SessionQueryRequest {
    @NotNull
    private Long gymId;
    @NotNull
    private Long classTypeId;
    @NotNull
    private Long trainerId;
    @NotNull
    private LocalDate date;
}
