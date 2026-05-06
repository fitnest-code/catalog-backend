package az.fitnest.catalog.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record GymAdminCreateRequest(
    @NotBlank String name,
    @NotBlank String surname,
    @NotBlank String phoneNumber,
    @NotBlank @Email String email,
    @NotBlank String password
) {}
