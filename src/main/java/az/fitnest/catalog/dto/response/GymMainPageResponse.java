package az.fitnest.catalog.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import java.util.List;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GymMainPageResponse(
        String gymId,
        String name,
        String coverImageUrl,
        double stars,
        boolean isNew,
        String location,
        String city,
        Double distanceKm,
        boolean isSaved,
        CategoryResponse category,
        List<GymPlanItemResponse> supportedSubscriptions,
        String workHoursText
) {
}