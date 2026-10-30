package az.fitnest.catalog.dto.response;

import lombok.Builder;

import java.util.List;

/**
 * Combined admin state for the "Subscription / Services" tab of a gym (spec mock 6.1).
 */
@Builder
public record HomeGymAdminStateResponse(
        Long gymId,
        String gymName,
        boolean homeEligible,
        List<HomeGymPackagePriceResponse> prices,
        List<HomeGymTimeSlotRuleResponse> timeSlots
) {
}
