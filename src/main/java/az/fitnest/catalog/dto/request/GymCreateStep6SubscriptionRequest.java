package az.fitnest.catalog.dto.request;
import az.fitnest.catalog.dto.response.*;
import az.fitnest.catalog.dto.*;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Builder;

@Builder
public record GymCreateStep6SubscriptionRequest(
    @NotNull Long packageId,
    @NotNull Double price,
    List<Long> supportedServicesId
) {}
