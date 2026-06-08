package az.fitnest.catalog.dto.response;

import lombok.Builder;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Builder
public record GymSubscriptionsAdminResponseV2(
    @Schema(description = "Zal ID")
    Long gymId,

    @Schema(description = "Aktiv abunəlik planları və onların xidmətləri")
    List<GymPlanItemAdminResponseV2> subscriptions
) {}
