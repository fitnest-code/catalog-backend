package az.fitnest.catalog.dto.response;
import az.fitnest.catalog.dto.request.*;
import az.fitnest.catalog.dto.*;

import lombok.Builder;

@Builder
public record LocationResponse(
    Double latitude,
    Double longitude,
    String addressText,
    String city
) {}
