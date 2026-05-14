package az.fitnest.catalog.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(description = "Mağazanın iş saatları")
public class StoreWorkHoursRequest {

    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "Format HH:mm olmalıdır")
    @Schema(description = "Açılış saatı", example = "09:00")
    private String from;

    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "Format HH:mm olmalıdır")
    @Schema(description = "Bağlanış saatı", example = "22:00")
    private String to;
}
