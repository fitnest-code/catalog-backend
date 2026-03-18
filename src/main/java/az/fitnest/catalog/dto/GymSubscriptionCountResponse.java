package az.fitnest.catalog.dto;
import lombok.Builder;
@Builder
public record GymSubscriptionCountResponse(String subscriptionName, long count) {}
