package az.fitnest.catalog.dto.request;
import az.fitnest.catalog.dto.response.*;
import az.fitnest.catalog.dto.*;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
@Schema(description = "Request to create or update a profession")
public record ProfessionRequest(
    @NotBlank(message = "Peşə adı boş ola bilməz")
    @Schema(description = "Name of the profession", example = "CrossFit Coach")
    String name
) {}
