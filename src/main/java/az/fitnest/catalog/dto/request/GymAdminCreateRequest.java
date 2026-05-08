package az.fitnest.catalog.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record GymAdminCreateRequest(
    @NotBlank @jakarta.validation.constraints.Size(min = 2, max = 50) String name,
    @NotBlank @jakarta.validation.constraints.Size(min = 2, max = 50) String surname,
    @NotBlank @jakarta.validation.constraints.Pattern(regexp = "^(050|051|010|055|099|070|077|060)\\d{7}$") String phoneNumber,
    @NotBlank @Email String email,
    @NotBlank @jakarta.validation.constraints.Size(min = 8, max = 50) String password
) {}
