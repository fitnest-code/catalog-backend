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
    String reason,

    // Home-gym decision payload (spec R5/R6/R8/R14/R18).
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String decision,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String messageKey,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String entryType,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    Long visitId,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    Long homeGymId,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    Long standardVisitsUsed,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    Integer standardVisitsLimit,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    Long homeVisitsThisPeriod,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String nextHomeEntryAt,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String unlocksAt,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String renewsAt,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String confirmationType,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String confirmationToken,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String confirmationExpiresAt,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    Long targetGymId,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String targetGymName,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String homeGymName,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String availableHours,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String packageName
) {}
