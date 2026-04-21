package az.fitnest.catalog.dto.admin;

import az.fitnest.catalog.model.enums.GymAdminRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateGymAdminRequest(
        @NotNull GymAdminRole role,
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank String phoneNumber,
        @Email String email
) {
}