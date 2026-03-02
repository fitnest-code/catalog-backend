package az.fitnest.catalog.dto;

import lombok.Builder;

@Builder
public record GymImageItemDto(
    String image_id,
    String type,
    String title,
    String url
) {}
