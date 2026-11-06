package az.fitnest.catalog.dto.response;

import lombok.Builder;

/**
 * Paging counters for {@link GymListResponseV3}. {@code homeEligibleCount} uses the
 * same live rule as the home-gym filter and changes as soon as an admin switch does.
 */
@Builder
public record GymListMetaV3(
        long total,
        long homeEligibleCount,
        int page,
        int pageSize
) {
}
