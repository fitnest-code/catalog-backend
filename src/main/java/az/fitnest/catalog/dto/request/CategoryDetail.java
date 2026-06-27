package az.fitnest.catalog.dto.request;

import jakarta.validation.constraints.NotNull;

public record CategoryDetail(
    @NotNull(message = "Kateqoriya ID boş ola bilməz")
    Long categoryId,
    String phone,
    String description
) {}
