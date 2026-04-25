package az.fitnest.catalog.dto.admin;

import az.fitnest.catalog.model.enums.AdminPanelGymStatus;

public record AdminPanelGymResponse(
        Long id,
        String name,
        AdminPanelGymStatus status
) {
}
