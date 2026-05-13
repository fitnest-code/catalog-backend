package az.fitnest.catalog.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Mağaza yaradılması - Addım 2: Məkan, əlaqə və iş saatları")
public class StoreStep2Request {

    @NotNull
    @DecimalMin(value = "-90.0", message = "Latitude -90 ilə 90 arasında olmalıdır")
    @DecimalMax(value = "90.0", message = "Latitude -90 ilə 90 arasında olmalıdır")
    @Schema(description = "Mağazanın coğrafi enliyi", example = "40.4093")
    private Double latitude;

    @NotNull
    @DecimalMin(value = "-180.0", message = "Longitude -180 ilə 180 arasında olmalıdır")
    @DecimalMax(value = "180.0", message = "Longitude -180 ilə 180 arasında olmalıdır")
    @Schema(description = "Mağazanın coğrafi uzunluğu", example = "49.8671")
    private Double longitude;

    @Schema(description = "Mağazanın əlaqə nömrəsi", example = "+994501234567")
    private String phone;

    @Email(message = "Düzgün email formatı daxil edin")
    @Schema(description = "Mağazanın email ünvanı", example = "market@example.com")
    private String email;

    @Schema(description = "Mağazanın sosial media linki", example = "https://instagram.com/market")
    private String socialUrl;

    @Valid
    @Schema(description = "Mağazanın iş saatları")
    private StoreWorkHoursRequest workHours;
}
