package az.fitnest.catalog.dto;
import lombok.Builder;
@Builder
public record GymSubscriptionCountResponse(Long subscriptionId, String subscriptionName, long count) {}
