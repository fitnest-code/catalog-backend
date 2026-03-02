package az.fitnest.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record CategoryRequest(
    @NotBlank
    String name
) {}
