package az.fitnest.catalog.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record GymInfoUpdateRequest(
    @NotNull(message = "Kateqoriya ID boş ola bilməz")
    @Schema(description = "Kateqoriya ID")
    Long categoryId,

    @NotBlank(message = "Zal adı boş ola bilməz")
    @Schema(description = "Zal adı")
    String name,

    @Schema(description = "Haqqında / Təsvir")
    String description,

    @NotBlank(message = "Telefon nömrəsi boş ola bilməz")
    @Schema(description = "Telefon nömrəsi")
    String phone,

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
