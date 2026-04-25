package az.fitnest.catalog.dto.admin;

import az.fitnest.catalog.model.enums.AdminPanelGymStatus;

public record AdminPanelGymDetailDto(
        Long id,
        String name,
        String description,
        AdminPanelGymStatus status,
        String phoneNumber,
        String email,
        String address,
        Double latitude,
        Double longitude,
        String coverImageUrl
) {
}
