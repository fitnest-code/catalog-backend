package az.fitnest.catalog.dto;

import az.fitnest.catalog.model.enums.ReservationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservationStatusUpdateRequest {
    @NotNull(message = "Status is required")
    private ReservationStatus status;
}
