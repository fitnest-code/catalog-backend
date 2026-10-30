package az.fitnest.catalog.dto.response;

import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

/**
 * Admin view of one home-gym time-slot rule.
 */
@Builder
public record HomeGymTimeSlotRuleResponse(
        Long id,
        List<String> weekdays,
        String startTime,
        String endTime,
        List<Long> allowedPackageIds,
        Long pricePackageId,
        String pricePackageName,
        LocalDate effectiveFrom,
        LocalDate effectiveTo
) {
}
