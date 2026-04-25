package az.fitnest.catalog.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ResetPasswordRequest(
        @NotBlank(message = "newPassword boş ola bilməz")
        @Pattern(
                regexp = "^(?=.*[A-Z])(?=.*[0-9])(?=.*[^A-Za-z0-9]).{8,}$",
                message = "Parol minimum 8 simvol, 1 böyük hərf, 1 rəqəm və 1 xüsusi simvol içərməlidir"
        )
        String newPassword
) {
}