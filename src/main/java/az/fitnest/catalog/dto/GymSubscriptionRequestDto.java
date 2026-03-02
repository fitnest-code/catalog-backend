package az.fitnest.catalog.dto;

import lombok.Builder;
import java.util.List;

@Builder
public record GymSubscriptionRequestDto(
    Long planId,
    List<GymSubscriptionBenefitRequestDto> benefits
) {}
