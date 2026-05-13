package az.fitnest.catalog.service;

import az.fitnest.catalog.dto.response.GeocodingResponse;
import az.fitnest.catalog.dto.*;
import az.fitnest.catalog.dto.request.*;
import az.fitnest.catalog.dto.response.*;

public interface ReverseGeocodingService {
    GeocodingResponse reverseGeocode(Double latitude, Double longitude);
    java.util.List<GeocodingResponse> forwardGeocode(String query);
}
