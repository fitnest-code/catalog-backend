package az.fitnest.catalog.dto.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AdminPanelTrainerRequest(
        @NotBlank(message = "firstName boş ola bilməz")
        String firstName,

        @NotBlank(message = "lastName boş ola bilməz")
        String lastName,

        @NotBlank(message = "specialization boş ola bilməz")
        String specialization,

        @NotBlank(message = "phoneNumber boş ola bilməz")
        String phoneNumber,

        @Email(message = "email formatı düzgün deyil")
        String email
) {
}
