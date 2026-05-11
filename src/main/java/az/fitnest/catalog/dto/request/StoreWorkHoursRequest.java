package az.fitnest.catalog.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @author: nijataghayev
 */

@Data
public class StoreWorkHoursRequest {

    @NotBlank
    private String from;
    @NotBlank
    private String to;
}
