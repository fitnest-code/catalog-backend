package az.fitnest.catalog.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

@Builder
@Schema(description = "Filter options for the public landing gym listing.")
public record LandingGymFiltersResponse(
        List<String> cities,
        List<String> categories,
        List<String> memberships
) {
}
