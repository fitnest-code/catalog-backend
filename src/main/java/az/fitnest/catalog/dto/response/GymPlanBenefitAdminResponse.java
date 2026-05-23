package az.fitnest.catalog.dto.response;

import lombok.Builder;
import io.swagger.v3.oas.annotations.media.Schema;

@Builder
public record GymPlanBenefitAdminResponse(
    @Schema(description = "Xidmət ID")
    Long id,
    @Schema(description = "Xidmət adı / təsviri")
    String name,
    @Schema(description = "Xidmət ikonu URL-i")
    String iconImageUrl
) {}
