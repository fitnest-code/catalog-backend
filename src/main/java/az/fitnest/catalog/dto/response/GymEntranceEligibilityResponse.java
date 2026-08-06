package az.fitnest.catalog.dto.response;
import az.fitnest.catalog.dto.request.*;
import az.fitnest.catalog.dto.*;

import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Eligibility check result for gym entrance.")
public record GymEntranceEligibilityResponse(
    @Schema(description = "Whether the user is eligible to enter the gym.", example = "true")
    boolean allowed,
    @Schema(description = "Status of the eligibility (e.g., ELIGIBLE, INELIGIBLE)", example = "ELIGIBLE")
    String status,
    @Schema(description = "Localized reason for failure if not allowed", example = "Aktiv abunəliyiniz yoxdur")
    String reason
) {}
