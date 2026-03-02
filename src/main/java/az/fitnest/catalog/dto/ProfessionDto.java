package az.fitnest.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "Profession details")
public record ProfessionDto(
    @Schema(description = "Unique ID of the profession") Long id,
    @Schema(description = "Name of the profession", example = "CrossFit Coach") String name
) {}
