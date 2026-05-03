package az.fitnest.catalog.dto;

import az.fitnest.catalog.model.enums.GymStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.ALWAYS)
@Schema(description = "Admin view of a gym, including name, address, owner, and status.")
public record AdminGymResponse(
    @Schema(description = "Unique identifier of the gym", example = "123")
    Long id,

    @Schema(description = "Name of the gym", example = "FitLife Premium")
    String name,

    @Schema(description = "Full address (city + address)", example = "Baku, 28 May str. 12")
    String fullAddress,

    @Schema(description = "Full name of the gym owner", example = "Ali Veliyev")
    String ownerName,

    @Schema(description = "Status of the gym", example = "ACTIVE")
    GymStatus status
) {}
