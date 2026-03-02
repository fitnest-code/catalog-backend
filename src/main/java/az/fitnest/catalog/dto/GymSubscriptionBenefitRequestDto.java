package az.fitnest.catalog.dto;

import lombok.Builder;

@Builder
public record GymSubscriptionBenefitRequestDto(
    String benefit,
    String benefitLogo
) {}
