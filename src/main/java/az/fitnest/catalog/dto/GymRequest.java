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
    String name,

    @Schema(description = "Description")
    String description,

    @NotNull(message = "Ünvan boş ola bilməz")
    @Valid
    AddressDto address,

    @NotBlank(message = "Telefon boş ola bilməz")
    @Pattern(regexp = "^(050|051|010|055|099|070|077|060)\\d{7}$", message = "Yanlış mobil nömrə formatı. 050, 051, 010, 055, 099, 070, 077 və ya 060 ilə başlamalı və 7 rəqəmlə davam etməlidir.")
    String phone,

    @NotBlank(message = "Email boş ola bilməz")
    @Email(message = "Yanlış email formatı")
    String email,

    @NotEmpty(message = "Kateqoriyalar boş ola bilməz")
    Set<Long> categoryIds,

    @NotNull(message = "Status boş ola bilməz")
    GymStatus status,

    java.util.List<GymWorkHourDto> workHours
) {}
