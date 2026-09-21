package az.fitnest.catalog.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;
import java.util.Map;

@Builder
@Schema(description = "Filter options for the public landing FitStore listing.")
public record LandingStoreFiltersResponse(
        List<String> cities,
        Map<String, List<String>> rayonsByCity,
        List<String> categories,
        List<String> memberships
) {
}
