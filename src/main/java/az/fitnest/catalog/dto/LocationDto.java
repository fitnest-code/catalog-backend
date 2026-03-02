package az.fitnest.catalog.dto;

import lombok.Builder;

@Builder
public record LocationDto(
    Double latitude,
    Double longitude,
    String addressText,
    String city
) {}
