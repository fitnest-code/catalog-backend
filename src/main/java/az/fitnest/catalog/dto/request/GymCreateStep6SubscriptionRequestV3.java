package az.fitnest.catalog.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import java.util.List;

@Builder
public record GymCreateStep6SubscriptionRequestV3(
    @NotNull Long packageId,
    @NotNull Long categoryId,
    @NotNull Double dailyPrice,
    List<Long> supportedServicesId,
    @Valid List<LocalizedServiceNameRequest> customServices
) {}
