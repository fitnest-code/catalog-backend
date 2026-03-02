package az.fitnest.catalog.dto;

import lombok.Builder;

@Builder
public record GymMainPageDto(
    String gymId,
    String name,
    String coverImageUrl,
    double stars,
    boolean isNew,
    String location,
    String city,
    Double distanceKm,
    boolean isSaved
) {}
