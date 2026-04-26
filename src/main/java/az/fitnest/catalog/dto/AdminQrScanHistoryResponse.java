package az.fitnest.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "İstifadəçinin QR skan tarixçəsi (Admin üçün)")
public record AdminQrScanHistoryResponse(
    @Schema(description = "Tarix və saat", example = "25.10.2023 14:30")
    String dateTime,

    @Schema(description = "İdman zalının adı", example = "Fitnest Academy")
    String gymName,

    @Schema(description = "Skan statusu", example = "ELIGIBLE")
    String status,

    @Schema(description = "Uğursuzluq səbəbi (əgər varsa)", example = "TOO_FAR_FROM_GYM")
    String failedReason,

    @Schema(description = "Platforma", example = "iOS")
    String platform,

    @com.fasterxml.jackson.annotation.JsonIgnore
    java.time.LocalDateTime rawDate
) {}
