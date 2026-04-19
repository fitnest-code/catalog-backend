package az.fitnest.catalog.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationRulesRequest {
    @NotNull
    private Long gymId;
    @NotNull
    private String categoryName;
}
