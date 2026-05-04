package az.fitnest.catalog.dto.response;
import az.fitnest.catalog.dto.request.*;
import az.fitnest.catalog.dto.*;

import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@Builder
public record GymEntranceScanResponse(
    @Schema(description = "Gym name")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String gymName,
    @Schema(description = "Gym address")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String gymAddress,
    @Schema(description = "Date of entrance (yyyy-MM-dd)")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String enterDate,
    @Schema(description = "Hour of entrance (HH:mm)")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String enterHour,
    @Schema(description = "Whether entrance is allowed")
    boolean isAllowed,
    @Schema(description = "Short status of the scan (e.g., ELIGIBLE, INELIGIBLE)")
    String status,
    @Schema(description = "Reason for failure (e.g., SUBSCRIPTION_NOT_SUPPORTED)")
    String reason
) {}
