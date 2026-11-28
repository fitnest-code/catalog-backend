package az.fitnest.catalog.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

/**
 * Per-category free text. {@code description} is the existing AZ value;
 * {@code descriptionEn} / {@code descriptionRu} are the optional localized values.
 */
@Builder
public record CategoryDetailV3(
    @NotNull(message = "Kateqoriya ID boş ola bilməz")
    Long categoryId,

    String phone,

    @Size(max = 2000)
    String description,

    @Size(max = 2000)
    String descriptionEn,

    @Size(max = 2000)
    String descriptionRu,

    String coverImageUrl
) {}
