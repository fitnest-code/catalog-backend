/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  io.swagger.v3.oas.annotations.media.Schema
 */
package az.fitnest.catalog.dto;

import az.fitnest.catalog.dto.AddressDto;

import java.util.Set;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@lombok.Data
@lombok.Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class GymRequest {
    @NotBlank(message = "Ad boş ola bilməz")
    private String name;

    private String description;

    @NotNull(message = "Ünvan boş ola bilməz")
    @Valid
    private AddressDto address;

    @NotBlank(message = "Telefon boş ola bilməz")
    @Pattern(regexp = "^(050|051|010|055|099|070|077|060)\\d{7}$", message = "Yanlış mobil nömrə formatı. 050, 051, 010, 055, 099, 070, 077 və ya 060 ilə başlamalı və 7 rəqəmlə davam etməlidir.")
    private String phone;

    @NotBlank(message = "Email boş ola bilməz")
    @Email(message = "Yanlış email formatı")
    private String email;

    @NotEmpty(message = "Kateqoriyalar boş ola bilməz")
    private Set<Long> categoryIds;

    private String responsiblePerson;
    @NotNull(message = "Status boş ola bilməz")
    private az.fitnest.catalog.model.enums.GymStatus status;


}
