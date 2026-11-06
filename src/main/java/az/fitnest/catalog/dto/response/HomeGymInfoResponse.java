package az.fitnest.catalog.dto.response;

import az.fitnest.catalog.dto.v4.GymMembershipPolicyV4;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.util.List;

/**
 * Read-only home-gym hierarchy returned before purchase.
 * {@code eligibleForRequestedTier} is omitted when the request did not send a package/tier.
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record HomeGymInfoResponse(
        GymMembershipPolicyV4.Tier minTier,
        List<GymMembershipPolicyV4.Tier> tiers,
        Boolean eligibleForRequestedTier
) {
}
