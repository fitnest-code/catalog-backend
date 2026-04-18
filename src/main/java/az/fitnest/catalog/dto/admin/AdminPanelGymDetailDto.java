package az.fitnest.catalog.dto.admin;

import az.fitnest.catalog.model.enums.GymStatus;

public record AdminPanelGymDetailDto(
        Long id,
        String name,
        String description,
        GymStatus status,
        String phoneNumber,
        String email,
        String address,
        Double latitude,
        Double longitude,
        String coverImageUrl
) {
}
