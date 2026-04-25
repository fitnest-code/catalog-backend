package az.fitnest.catalog.dto.admin;

import lombok.Builder;

@Builder
public record AdminPanelGeocodingResponse(
        String addressText,
        String city,
        String district
) {}