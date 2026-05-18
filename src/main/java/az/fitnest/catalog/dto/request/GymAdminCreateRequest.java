package az.fitnest.catalog.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record GymAdminCreateRequest(
    @NotBlank @jakarta.validation.constraints.Size(min = 2, max = 50) String name,
    @NotBlank @jakarta.validation.constraints.Size(min = 2, max = 50) String surname,
    @NotBlank @jakarta.validation.constraints.Pattern(regexp = "^(\\+994|0)?\\s?(10|50|51|55|60|70|77|99)(\\s?\\d){7}$", message = "Yanlış mobil nömrə formatı") String phoneNumber,
    @Email String email,
    @NotBlank String password
) {}
