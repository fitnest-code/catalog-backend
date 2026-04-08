package az.fitnest.catalog.dto;

import lombok.Builder;

@Builder
public record StoreSocialDto(
    String name,
    String url
) {}
