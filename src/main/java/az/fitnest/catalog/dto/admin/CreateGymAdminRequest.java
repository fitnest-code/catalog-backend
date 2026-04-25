package az.fitnest.catalog.dto.admin;

import az.fitnest.catalog.model.enums.GymAdminRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CreateGymAdminRequest(
        @NotNull(message = "role boş ola bilməz")
        GymAdminRole role,

        @NotBlank(message = "firstName boş ola bilməz")
        String firstName,

        @NotBlank(message = "lastName boş ola bilməz")
        String lastName,

        @NotBlank(message = "phoneNumber boş ola bilməz")
        String phoneNumber,

        @NotBlank(message = "email boş ola bilməz")
        @Email(message = "email formatı düzgün deyil")
        String email,

        @NotBlank(message = "password boş ola bilməz")
        @Pattern(
                regexp = "^(?=.*[A-Z])(?=.*[0-9])(?=.*[^A-Za-z0-9]).{8,}$",
                message = "Parol minimum 8 simvol, 1 böyük hərf, 1 rəqəm və 1 xüsusi simvol içərməlidir"
        )
        String password
) {}
