package az.fitnest.catalog.dto.admin;

import az.fitnest.catalog.model.enums.GymFilterStatus;
import jakarta.validation.constraints.NotNull;

public record AdminPanelUpdateGymStatusRequest (
        @NotNull(message = "status boş ola bilməz")
        GymFilterStatus status
) {
}