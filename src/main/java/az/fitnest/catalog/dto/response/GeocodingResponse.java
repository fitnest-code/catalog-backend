package az.fitnest.catalog.dto.response;
import az.fitnest.catalog.dto.request.*;
import az.fitnest.catalog.dto.*;

import lombok.Builder;

@Builder
public record GeocodingResponse(
    String addressText,
    String city,
    String rayon,
    Double latitude,
    Double longitude
) {
    public GeocodingResponse(String addressText, String city, Double latitude, Double longitude) {
        this(addressText, city, null, latitude, longitude);
    }
}
