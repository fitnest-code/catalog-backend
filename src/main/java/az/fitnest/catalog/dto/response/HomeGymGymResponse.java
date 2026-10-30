package az.fitnest.catalog.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

/**
 * Item of the home-gym selection list (spec screen 1.3/4.1).
 */
@Builder
public record HomeGymGymResponse(
        String gymId,
        String name,
        String coverImageUrl,
        String city,
        String location,
        Double latitude,
        Double longitude,
        Double distanceKm,
        Double rating,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String availableHours
) {
}
