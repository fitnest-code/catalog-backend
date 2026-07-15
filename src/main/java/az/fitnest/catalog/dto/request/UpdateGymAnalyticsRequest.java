package az.fitnest.catalog.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.PositiveOrZero;

@Schema(description = "İdman zalı analitika xülasə kartlarını (ümumi gəlir, uğurlu/uğursuz girişlər) əl ilə yeniləmək üçün sorğu")
public record UpdateGymAnalyticsRequest(

        @Schema(description = "Ümumi gəlir (AZN). Verilmədikdə dəyişməz qalır.", example = "40.00")
        @PositiveOrZero(message = "error.total_profit_negative")
        Double totalProfit,

        @Schema(description = "Uğurlu girişlərin sayı. Verilmədikdə dəyişməz qalır.", example = "4")
        @PositiveOrZero(message = "error.successful_scans_negative")
        Long successfulScans,

        @Schema(description = "Uğursuz girişlərin sayı. Verilmədikdə dəyişməz qalır.", example = "2")
        @PositiveOrZero(message = "error.failed_scans_negative")
        Long failedScans
) {}
