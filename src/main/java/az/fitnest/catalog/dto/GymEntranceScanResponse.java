package az.fitnest.catalog.dto;

import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GymEntranceScanResponse(
    @Schema(description = "Gym name")
    String gymName,
    @Schema(description = "Gym address")
    String gymAddress,
    @Schema(description = "Date of entrance (yyyy-MM-dd)")
    String enterDate,
    @Schema(description = "Hour of entrance (HH:mm)")
    String enterHour,
    @Schema(description = "Whether entrance is allowed")
    boolean notAllowed
) {}
