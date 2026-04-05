package az.fitnest.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request containing the gym ID to check entry eligibility for")
public record GymEntranceEligibilityRequest(
    @Schema(description = "ID of the gym", example = "1")
    Long gymId
) {}
