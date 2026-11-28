package az.fitnest.catalog.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

/**
 * Free-written service name. {@code name} is the AZ value stored on the service row;
 * EN/RU are stored as translations and fall back to AZ when blank.
 */
@Builder
public record LocalizedServiceNameRequest(
    @NotBlank(message = "Xidmət adı boş ola bilməz")
    @Size(max = 255)
    String name,

    @Size(max = 255)
    String nameEn,

    @Size(max = 255)
    String nameRu
) {}
