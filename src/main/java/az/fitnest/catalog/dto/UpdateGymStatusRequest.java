package az.fitnest.catalog.dto;

import az.fitnest.catalog.model.enums.GymStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;

@Builder
public record UpdateGymStatusRequest(

        @NotNull(message = "Status boş ola bilməz")
        @Schema(description = "İdman zalının statusu", example = "ACTIVE")
        @Pattern(regexp = "ACTIVE|INACTIVE", message = "status yalnız ACTIVE və ya INACTIVE ola bilər")
        GymStatus status

) {
}