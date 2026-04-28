package az.fitnest.catalog.dto;

import lombok.Builder;

@Builder
public record GymCreateStep1Response(
    Long gymId
) {}
