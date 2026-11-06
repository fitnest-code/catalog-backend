package az.fitnest.catalog.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.util.List;

/**
 * Gym list item with the pre-purchase home-gym hierarchy. The other fields mirror
 * {@link GymMainPageResponseV2} so existing clients can reuse their parsing.
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GymMainPageResponseV3(
        String gymId,
        String name,
        String coverImageUrl,
        double stars,
        boolean isNew,
        String location,
        String city,
        Double distanceKm,
        boolean isSaved,
        List<CategoryResponse> categories,
        List<GymPlanItemResponseV2> supportedSubscriptions,
        String workHoursText,
        HomeGymInfoResponse homeGym
) {
}
