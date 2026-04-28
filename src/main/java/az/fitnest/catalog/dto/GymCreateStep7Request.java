package az.fitnest.catalog.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record GymCreateStep7Request(
    @NotBlank String name,
    @NotBlank String surname,
    @NotBlank String phoneNumber,
    @NotBlank @Email String email,
    @NotBlank String password
) {}
