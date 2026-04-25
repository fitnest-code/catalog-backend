package az.fitnest.catalog.dto.admin;

public record GymServiceItemDto(
        Long id,
        Long subscriptionTypeId,
        String name,
        Boolean isAvailable
) {
}
