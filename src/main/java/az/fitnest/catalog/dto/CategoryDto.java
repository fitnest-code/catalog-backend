package az.fitnest.catalog.dto;

import lombok.Builder;

@Builder
public record CategoryDto(
    Long id,
    String name,
    String photoUrl
) {}
