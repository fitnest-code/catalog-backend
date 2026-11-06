package az.fitnest.catalog.dto.response;

import az.fitnest.catalog.dto.v4.GymMembershipPolicyV4;

/**
 * Per-tier home-gym availability shown on the gym detail screen (mock 10.4/10.5).
 */
public record GymPackageHomeEligibilityResponse(
        GymMembershipPolicyV4.Tier tier,
        boolean homeEligible
) {
}
