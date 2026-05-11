package az.fitnest.catalog.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @author: nijataghayev
 */

@Data
public class StoreStep2Request {

    @NotNull
    private Double latitude;

    @NotNull
    private Double longitude;

    private String phone;
    private String email;
    private String socialUrl;

    @Valid
    private StoreWorkHoursRequest workHours;
}
