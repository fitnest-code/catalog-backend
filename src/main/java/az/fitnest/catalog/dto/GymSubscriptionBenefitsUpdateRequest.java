package az.fitnest.catalog.dto;

import lombok.Builder;
import java.util.List;

@Builder
public record GymSubscriptionBenefitsUpdateRequest(
    List<Long> benefitIds,
    List<GymSubscriptionBenefitRequestDto> benefits
) {}
