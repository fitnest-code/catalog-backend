package az.fitnest.catalog.service.impl;

import az.fitnest.catalog.dto.response.GeocodingResponse;
import az.fitnest.catalog.dto.*;
import az.fitnest.catalog.dto.request.*;
import az.fitnest.catalog.dto.response.*;
import az.fitnest.catalog.service.ReverseGeocodingService;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;

@Service
public class ReverseGeocodingServiceImpl implements ReverseGeocodingService {
    private static final String BASE_URL = "https://nominatim.openstreetmap.org";
    private static final String USER_AGENT = "fitnest-catalog-backend";
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    @org.springframework.cache.annotation.Cacheable(cacheNames = "geocoding", key = "{#latitude, #longitude}")
    public GeocodingResponse reverseGeocode(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            return null;
        }
        URI uri = UriComponentsBuilder.fromUriString(BASE_URL)
                .path("/reverse")
                .queryParam("format", "json")
                .queryParam("lat", latitude)
                .queryParam("lon", longitude)
                .queryParam("zoom", 18)
                .queryParam("addressdetails", 1)
                .build(true).toUri();
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", USER_AGENT);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        try {
            ResponseEntity<Map> response = this.restTemplate.exchange(uri, HttpMethod.GET, entity, Map.class);
            Map<String, Object> body = response.getBody();
            if (body != null) {
                String displayName = (String) body.get("display_name");
                String city = null;
                Object addressObj = body.get("address");
                if (addressObj instanceof Map) {
                    Map<String, String> address = (Map<String, String>) addressObj;
                    city = address.get("city");
                    if (city == null) city = address.get("town");
                    if (city == null) city = address.get("village");
                    if (city == null) city = address.get("hamlet");
                    if (city == null) city = address.get("suburb");
                }
                return GeocodingResponse.builder()
                        .addressText(displayName)
                        .city(city)
                        .latitude(latitude)
                        .longitude(longitude)
                        .build();
            }
        } catch (Exception exception) {
        }
        return GeocodingResponse.builder()
                .addressText(String.format("%.5f, %.5f", latitude, longitude))
                .latitude(latitude)
                .longitude(longitude)
                .build();
    }

    @Override
    public java.util.List<GeocodingResponse> forwardGeocode(String query) {
        if (query == null || query.isBlank()) {
            return java.util.Collections.emptyList();
        }
        URI uri = UriComponentsBuilder.fromUriString(BASE_URL)
                .path("/search")
                .queryParam("format", "json")
                .queryParam("q", query)
                .queryParam("limit", 10)
                .queryParam("addressdetails", 1)
                .queryParam("countrycodes", "az")
                .build(true).toUri();
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", USER_AGENT);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        try {
            ResponseEntity<java.util.List> response = this.restTemplate.exchange(uri, HttpMethod.GET, entity, java.util.List.class);
            java.util.List<Map<String, Object>> list = response.getBody();
            if (list != null) {
                java.util.List<GeocodingResponse> results = new java.util.ArrayList<>();
                for (Map<String, Object> item : list) {
                    String displayName = (String) item.get("display_name");
                    Double lat = null;
                    Double lon = null;
                    try {
                        if (item.get("lat") != null) lat = Double.parseDouble(item.get("lat").toString());
                        if (item.get("lon") != null) lon = Double.parseDouble(item.get("lon").toString());
                    } catch (Exception e) {}

                    String city = null;
                    Object addressObj = item.get("address");
                    if (addressObj instanceof Map) {
                        Map<String, String> address = (Map<String, String>) addressObj;
                        city = address.get("city");
                        if (city == null) city = address.get("town");
                        if (city == null) city = address.get("village");
                        if (city == null) city = address.get("hamlet");
                        if (city == null) city = address.get("suburb");
                    }
                    results.add(GeocodingResponse.builder()
                            .addressText(displayName)
                            .city(city)
                            .latitude(lat)
                            .longitude(lon)
                            .build());
                }
                return results;
            }
        } catch (Exception exception) {
        }
        return java.util.Collections.emptyList();
    }
}
