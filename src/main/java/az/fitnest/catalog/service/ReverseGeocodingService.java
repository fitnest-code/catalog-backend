package az.fitnest.catalog.service;

import az.fitnest.catalog.dto.GeocodingResponse;

public interface ReverseGeocodingService {
    GeocodingResponse reverseGeocode(Double latitude, Double longitude);
}
