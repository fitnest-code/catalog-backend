package az.fitnest.catalog.dto;

import lombok.Builder;

@Builder
public record GeocodingResponse(
    String addressText,
    String city
) {}
