package az.fitnest.catalog.dto.request;
import az.fitnest.catalog.dto.response.*;
import az.fitnest.catalog.dto.*;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservationRejectRequest {
    private String reason;
}
