package az.fitnest.catalog.dto.admin;

import jakarta.validation.constraints.NotBlank;

public record AdminPanelCreateGymRequest(
        @NotBlank(message = "ad boş ola bilməz")
        String name
) {
}