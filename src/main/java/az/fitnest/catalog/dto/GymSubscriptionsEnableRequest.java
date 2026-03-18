package az.fitnest.catalog.dto;

import lombok.Builder;
import java.util.List;

@Builder
public record GymSubscriptionsEnableRequest(
    List<Long> packageIds
) {}
