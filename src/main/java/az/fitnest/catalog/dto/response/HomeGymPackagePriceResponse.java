package az.fitnest.catalog.dto.response;

import lombok.Builder;

import java.time.LocalDate;

/**
 * Admin view of one versioned home-gym price row.
 */
@Builder
public record HomeGymPackagePriceResponse(
        Long id,
        Long packageId,
        String packageName,
        Boolean included,
        Boolean homeEligible,
        Double singlePrice,
        Double unlimitedPrice,
        LocalDate effectiveFrom
) {
}
