package az.fitnest.catalog.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

import java.util.List;

@Builder
public record GymInfoUpdateRequestV2(

        @NotEmpty(message = "Kateqoriyalar boş ola bilməz")
        @Valid
        @Schema(description = "Kateqoriyalar — hər biri isMain field-i ilə")
        List<GymCategoryRequest> categories,

        @NotBlank(message = "Zal adı boş ola bilməz")
        @Schema(description = "Zal adı")
        String name,

        @Schema(description = "Haqqında / Təsvir")
        String description,

        @NotBlank(message = "Telefon nömrəsi boş ola bilməz")
        @Schema(description = "Telefon nömrəsi")
        String phone,

        @Email(message = "Yanlış email formatı")
        @Schema(description = "E-poçt ünvanı")
        String email,

        @Schema(description = "Şəhər")
        String city,

        @NotBlank(message = "Ünvan boş ola bilməz")
        @Schema(description = "Ünvan")
        String address,

        @Schema(description = "Enlik")
        Double latitude,

        @Schema(description = "Uzunluq")
        Double longitude

) {}
