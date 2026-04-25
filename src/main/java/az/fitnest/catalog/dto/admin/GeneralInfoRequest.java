package az.fitnest.catalog.dto.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record GeneralInfoRequest(
        @NotBlank(message = "ad boş ola bilməz")
        String name,

        String description,

        @NotBlank(message = "telefon nömrəsi boş ola bilməz")
        String phoneNumber,

        @Email(message = "email formatı düzgün deyil")
        String email,

        String address,

        Long cityId,
        Long districtId,

        Double latitude,
        Double longitude
) {
}
