package az.fitnest.catalog.dto.request;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

@Builder
public record GymCreateStep6RequestV2(
    @NotEmpty @Valid List<GymCreateStep6SubscriptionRequestV2> subscriptions
) {}
