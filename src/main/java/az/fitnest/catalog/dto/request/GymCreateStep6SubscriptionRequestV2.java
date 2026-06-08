package az.fitnest.catalog.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Builder;

@Builder
public record GymCreateStep6SubscriptionRequestV2(
    @NotNull Long packageId,
    @NotNull Long categoryId,
    @NotNull Double dailyPrice,
    List<Long> supportedServicesId,
    List<String> customServices
) {}
