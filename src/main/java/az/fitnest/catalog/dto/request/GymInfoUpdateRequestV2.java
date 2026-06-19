package az.fitnest.catalog.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import java.util.List;

@Builder
public record GymInfoUpdateRequestV2(
    @NotNull(message = "Kateqoriya tələb olunur")
    @Schema(description = "Əsas kateqoriya ID")
    Long mainCategoryId,

    @Schema(description = "Sub kateqoriya ID")
    Long subCategoryId,

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
    Double longitude,

    @Schema(description = "Hündürlük (Altitude)")
    Double altitude
) {}
