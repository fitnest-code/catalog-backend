package az.fitnest.catalog.dto.request;
import az.fitnest.catalog.dto.response.*;
import az.fitnest.catalog.dto.*;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record GymCreateStep3Request(
    @NotNull Double latitude,
    @NotNull Double longitude
) {}
