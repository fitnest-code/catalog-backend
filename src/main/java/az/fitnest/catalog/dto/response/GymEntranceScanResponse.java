package az.fitnest.catalog.dto.response;
import az.fitnest.catalog.dto.request.*;
import az.fitnest.catalog.dto.*;

import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@Builder
public record GymEntranceScanResponse(
    @Schema(description = "The name of the gym where the scan occurred")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String gymName,
    @Schema(description = "The physical address of the gym")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String gymAddress,
    @Schema(description = "The date the entrance was scanned (yyyy-MM-dd)")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String enterDate,
    @Schema(description = "The time the entrance was scanned (HH:mm)")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String enterHour,
    @Schema(description = "Whether the user is permitted to enter the gym")
    boolean isAllowed,
    @Schema(description = "The result status of the scan (e.g., ELIGIBLE, SUCCESSFUL, UNSUCCESSFUL)")
    String status,
    @Schema(description = "The specific reason for denial if isAllowed is false (e.g., NO_ACTIVE_SUBSCRIPTION, VISIT_LIMIT_EXCEEDED, GYM_NOT_SUPPORTED, OUT_OF_WORKING_HOURS, TOO_FAR_FROM_GYM, CHECKIN_FAILED)")
    String reason
) {}
