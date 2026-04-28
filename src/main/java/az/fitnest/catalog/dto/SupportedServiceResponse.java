package az.fitnest.catalog.dto;

import lombok.Builder;

@Builder
public record SupportedServiceResponse(
    Long id,
    String name
) {}
