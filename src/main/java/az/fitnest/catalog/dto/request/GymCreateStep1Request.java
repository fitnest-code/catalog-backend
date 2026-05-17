package az.fitnest.catalog.dto.request;
import az.fitnest.catalog.dto.response.*;
import az.fitnest.catalog.dto.*;

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
    @jakarta.validation.constraints.Size(min = 2, max = 100)
    String name,

    @NotBlank(message = "Telefon boş ola bilməz")
    @Pattern(regexp = "^(\\+994|0)?\\s?(10|50|51|55|60|70|77|99)(\\s?\\d){7}$", message = "Yanlış mobil nömrə formatı")
    String phone,

    @jakarta.validation.constraints.Size(max = 2000)
    String description,

    @Email(message = "Yanlış email formatı")
    String email,

    java.util.List<Long> lessonTypeIds
) {}
