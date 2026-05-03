package az.fitnest.catalog.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;

import java.util.Set;

@Builder
public record GymCreateStep1Request(
    @NotNull(message = "Kateqoriya tələb olunur")
    Long categoryId,

    @NotBlank(message = "Ad boş ola bilməz")
    String name,

    Double dailyPrice,

    String description,

    @NotBlank(message = "Telefon boş ola bilməz")
    @Pattern(regexp = "^(050|051|010|055|099|070|077|060)\\d{7}$", message = "Yanlış mobil nömrə formatı")
    String phone,

    @NotBlank(message = "Email boş ola bilməz")
    @Email(message = "Yanlış email formatı")
    String email
) {}
