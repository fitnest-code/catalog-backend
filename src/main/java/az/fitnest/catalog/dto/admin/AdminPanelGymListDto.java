package az.fitnest.catalog.dto.admin;

import az.fitnest.catalog.model.enums.GymStatus;

public record AdminPanelGymListDto(
        Long id,
        String name,
        String city,
        String district,
        String managerFullName,
        GymStatus status
) {}