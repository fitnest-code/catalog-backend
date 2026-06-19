package az.fitnest.catalog.dto.response;
import az.fitnest.catalog.dto.request.*;
import az.fitnest.catalog.dto.*;

import lombok.Builder;

@Builder
public record LocationResponse(
    Double latitude,
    Double longitude,
    String addressText,
    String city,
    Double altitude
) {
    public LocationResponse(Double latitude, Double longitude, String addressText, String city) {
        this(latitude, longitude, addressText, city, null);
    }
}
