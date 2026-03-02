package az.fitnest.catalog.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record ReviewRequest(
    @NotNull
    @Min(value = 1L)
    @Max(value = 5L)
    Integer rating,

    @Size(max = 1000)
    String comment
) {}
