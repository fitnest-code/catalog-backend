package az.fitnest.catalog.dto.request;
import az.fitnest.catalog.dto.response.*;
import az.fitnest.catalog.dto.*;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

@Builder
public record GymCreateStep6Request(
    @NotEmpty @Valid List<GymCreateStep6SubscriptionRequest> subscriptions
) {}
