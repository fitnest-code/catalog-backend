package az.fitnest.catalog.dto.admin;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record UpdateGymServiceRequest(
        @NotEmpty(message = "serviceTypeIds boş ola bilməz")
        List<Long> serviceTypeIds
) {
}
