package az.fitnest.catalog.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "Gym category name with optional public icon.")
public record LandingCategoryItem(
        String name,
        String iconUrl
) {
}
