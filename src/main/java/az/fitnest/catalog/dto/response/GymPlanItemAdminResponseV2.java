package az.fitnest.catalog.dto.response;

import lombok.Builder;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Builder
public record GymPlanItemAdminResponseV2(
    @Schema(description = "Paket (Abunəlik) ID")
    Long packageId,
    @Schema(description = "Paket adı (Bronze, Silver, etc.)")
    String packageName,
    @Schema(description = "Kateqoriya ID")
    Long categoryId,
    @Schema(description = "Günlük qiymət")
    Double dailyPrice,
    @Schema(description = "Bu paketə daxil olan xidmətlər")
    List<GymPlanBenefitAdminResponse> benefits
) {}
