package az.fitnest.catalog.dto.response;
import az.fitnest.catalog.dto.request.*;
import az.fitnest.catalog.dto.*;

import lombok.Builder;
import io.swagger.v3.oas.annotations.media.Schema;

@Builder
public record GymEntranceHistoryAdminResponse(
    @Schema(description = "User ID")
    Long userId,
    @Schema(description = "User first name")
    String firstName,
    @Schema(description = "User last name")
    String lastName,
    @Schema(description = "User phone number")
    String phone,
    @Schema(description = "Scan date and time")
    String scanDateTime,
    @Schema(description = "Status of the scan (e.g., SUCCESSFUL, UNSUCCESSFUL)")
    String status,
    @Schema(description = "Reason for failure if unsuccessful")
    String reason
) {}
