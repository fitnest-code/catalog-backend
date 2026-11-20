package az.fitnest.catalog.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

@Builder
@Schema(description = "Public gym card (v2). No phone or contact data; badge is the lowest entry tier.")
public record LandingGymCardV2Response(
        String gymId,
        String name,
        String coverImageUrl,
        String location,
        String city,
        String rayon,
        String category,
        List<String> categories,
        List<LandingCategoryItem> categoryItems,
        @Schema(description = "Lowest package tier that unlocks entry: bronze, silver, gold, or platinum")
        String minEntryMembership
) {
}
