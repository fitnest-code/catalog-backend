package az.fitnest.catalog.dto.admin;

public record GymSubscriptionDto(
        Long id,
        Long subscriptionTypeId,
        String name,
        Boolean isAvailable
) {
}
