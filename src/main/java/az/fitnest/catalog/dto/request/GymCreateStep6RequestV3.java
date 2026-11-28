package az.fitnest.catalog.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;
import java.util.List;

@Builder
public record GymCreateStep6RequestV3(
    @NotEmpty @Valid List<GymCreateStep6SubscriptionRequestV3> subscriptions
) {}
