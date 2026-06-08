package az.fitnest.catalog.dto.response;

import lombok.Builder;
import java.util.List;

@Builder
public record GymPlanItemResponseV2(
    String plan_id,
    String packageName,
    Long categoryId,
    Double dailyPrice,
    List<GymPlanBenefitResponse> benefits
) {}
