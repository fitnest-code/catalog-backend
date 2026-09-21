package az.fitnest.catalog.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

@Builder
@Schema(description = "Public gym detail for the landing gym page. Omits staff, QR, email, and admin fields.")
public record LandingGymDetailResponse(
        String gymId,
        String name,
        String coverImageUrl,
        List<String> galleryImageUrls,
        String location,
        String city,
        String rayon,
        Double latitude,
        Double longitude,
        String phone,
        List<String> workHours,
        String category,
        List<String> categories,
        List<LandingCategoryItem> categoryItems,
        String membership,
        List<String> accessMemberships,
        String description,
        List<String> amenities,
        String note
) {
}
