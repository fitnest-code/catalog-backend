package az.fitnest.catalog.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record AddDiscountRequest(
        @NotNull(message = "error.package_id_required")
        Long packageId,

        @NotNull(message = "error.percent_required")
        @Min(value = 1, message = "error.percent_min")
        @Max(value = 100, message = "error.percent_max")
        Integer percent
) {
}
