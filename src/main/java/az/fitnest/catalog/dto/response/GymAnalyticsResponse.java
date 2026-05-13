package az.fitnest.catalog.dto.response;

import az.fitnest.catalog.dto.PaginatedResponse;
import lombok.Builder;
import io.swagger.v3.oas.annotations.media.Schema;

@Builder
public record GymAnalyticsResponse(
    @Schema(description = "Total profit (sum of amounts from successful scans)")
    Double totalProfit,

    @Schema(description = "Count of successful scans")
    Long successfulScans,

    @Schema(description = "Count of failed scans")
    Long failedScans,

    @Schema(description = "Paginated list of scan history")
    PaginatedResponse<GymEntranceHistoryAdminResponse> history
) {}
