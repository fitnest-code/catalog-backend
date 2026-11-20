package az.fitnest.catalog.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

@Builder
@Schema(description = "Public gym detail (v2). Omits phone, staff, QR, email, and admin fields.")
public record LandingGymDetailV2Response(
        String gymId,
        String name,
        String coverImageUrl,
        List<String> galleryImageUrls,
        String location,
        String city,
        String rayon,
        Double latitude,
        Double longitude,
        List<String> workHours,
        String category,
        List<String> categories,
        List<LandingCategoryItem> categoryItems,
        @Schema(description = "Lowest package tier that unlocks entry")
        String minEntryMembership,
        @Schema(description = "All tiers that grant entry, ascending")
        List<String> accessMemberships,
        String description,
        List<String> amenities,
        String note
) {
}
