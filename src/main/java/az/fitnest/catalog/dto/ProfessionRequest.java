package az.fitnest.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to create or update a profession")
public class ProfessionRequest {
    @NotBlank(message = "Profession name cannot be blank")
    @Schema(description = "Name of the profession", example = "CrossFit Coach")
    private String name;
}
