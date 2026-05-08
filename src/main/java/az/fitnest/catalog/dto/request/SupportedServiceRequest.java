package az.fitnest.catalog.dto.request;
import az.fitnest.catalog.dto.response.*;
import az.fitnest.catalog.dto.*;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record SupportedServiceRequest(
    @NotBlank String name,
    Long gymId
) {}
