package az.fitnest.catalog.dto.admin;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record UpdateGymSubscriptionRequest(
        @NotEmpty(message = "subscriptionTypeIds boş ola bilməz")
        List<Long> subscriptionTypeIds
) {
}
