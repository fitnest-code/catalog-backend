package az.fitnest.catalog.dto.admin;

import az.fitnest.catalog.model.enums.GymStatus;

public record AdminPanelGymResponse(
        Long id,
        String name,
        GymStatus status
) {
}
