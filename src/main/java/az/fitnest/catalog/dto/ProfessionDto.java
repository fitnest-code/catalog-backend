package az.fitnest.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Profession details")
public class ProfessionDto {
    @Schema(description = "Unique ID of the profession")
    private Long id;
    
    @Schema(description = "Name of the profession", example = "CrossFit Coach")
    private String name;
}
