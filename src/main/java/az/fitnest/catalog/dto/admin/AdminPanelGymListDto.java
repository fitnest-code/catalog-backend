package az.fitnest.catalog.dto.admin;

import az.fitnest.catalog.model.enums.AdminPanelGymStatus;

public record AdminPanelGymListDto(
        Long id,
        String name,
        String city,
        String district,
        String managerFullName,
        AdminPanelGymStatus status
) {
}