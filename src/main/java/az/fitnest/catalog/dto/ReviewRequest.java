package az.fitnest.catalog.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import az.fitnest.catalog.validation.AtLeastOneNotNull;
import lombok.Builder;

@AtLeastOneNotNull(fields = {"rating", "comment"}, message = "At least one of rating or comment must be provided")
@Builder
public record ReviewRequest(
    @Min(value = 1L)
    @Max(value = 5L)
    Integer rating,

    @Size(max = 1000)
    String comment
) {}
