package az.fitnest.catalog.dto;

import az.fitnest.catalog.model.enums.GymStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.Set;

@Builder
public record GymRequest(
    @NotBlank(message = "Ad boş ola bilməz")
    @Schema(description = "İdman zalının adı", example = "FitLife Premium")
    String name,

    @Schema(description = "İdman zalının təsviri", example = "Bakının mərkəzində müasir idman zalı")
    String description,

    @NotNull(message = "Ünvan boş ola bilməz")
    @Valid
    @Schema(description = "İdman zalının ünvanı")
    AddressDto address,

    @NotBlank(message = "Telefon boş ola bilməz")
    @Pattern(regexp = "^(050|051|010|055|099|070|077|060)\\d{7}$", message = "Yanlış mobil nömrə formatı. 050, 051, 010, 055, 099, 070, 077 və ya 060 ilə başlamalı və 7 rəqəmlə davam etməlidir.")
    @Schema(description = "Əlaqə telefonu", example = "0501234567")
    String phone,

    @NotBlank(message = "Email boş ola bilməz")
    @Email(message = "Yanlış email formatı")
    @Schema(description = "Əlaqə emaili", example = "info@fitlife.az")
    String email,

    @NotEmpty(message = "Kateqoriyalar boş ola bilməz")
    @Schema(description = "Kateqoriya ID-ləri", example = "[1, 2]")
    Set<Long> categoryIds,

    @NotNull(message = "Status boş ola bilməz")
    @Schema(description = "İdman zalının statusu", example = "ACTIVE")
    GymStatus status,

    @Schema(description = "Ümumi iş saatları")
    java.util.List<GymWorkHourDto> generalWorkHours,
    @Schema(description = "Qadınlar zalı iş saatları")
    java.util.List<GymWorkHourDto> workHoursWoman,
    @Schema(description = "Kişilər zalı iş saatları")
    java.util.List<GymWorkHourDto> workHoursMan
) {}
