package az.fitnest.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record SupportedServiceRequest(
    @NotBlank String name
) {}
