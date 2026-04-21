package az.fitnest.catalog.dto.admin;

import az.fitnest.catalog.model.enums.GymAdminRole;
import az.fitnest.catalog.model.enums.GymAdminStatus;

public record GymAdminListDto(
        Long id,
        String firstName,
        String lastName,
        String phoneNumber,
        String email,
        GymAdminRole role,
        GymAdminStatus status
) {}