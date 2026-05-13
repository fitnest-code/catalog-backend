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
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class ReverseGeocodingServiceImpl implements ReverseGeocodingService {
    private static final String NOMINATIM_URL = "https://nominatim.openstreetmap.org";
    private static final String PHOTON_URL = "https://photon.komoot.io";
    private static final String USER_AGENT = "fitnest-catalog-backend";
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    @org.springframework.cache.annotation.Cacheable(cacheNames = "geocoding", key = "{#latitude, #longitude}")
    public GeocodingResponse reverseGeocode(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            return null;
        }
        URI uri = UriComponentsBuilder.fromUriString(NOMINATIM_URL)
                .path("/reverse")
                .queryParam("format", "json")
                .queryParam("lat", latitude)
                .queryParam("lon", longitude)
                .queryParam("zoom", 18)
                .queryParam("addressdetails", 1)
                .build().encode().toUri();
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
    public List<GeocodingResponse> forwardGeocode(String query) {
        if (query == null || query.isBlank()) {
            return Collections.emptyList();
        }

        // Query both Nominatim and Photon concurrently for better coverage
        CompletableFuture<List<GeocodingResponse>> nominatimFuture =
                CompletableFuture.supplyAsync(() -> forwardGeocodeNominatim(query));
        CompletableFuture<List<GeocodingResponse>> photonFuture =
                CompletableFuture.supplyAsync(() -> forwardGeocodePhoton(query));

        try {
            List<GeocodingResponse> nominatimResults = nominatimFuture.get(5, java.util.concurrent.TimeUnit.SECONDS);
            List<GeocodingResponse> photonResults = photonFuture.get(5, java.util.concurrent.TimeUnit.SECONDS);

            // Merge and deduplicate results (Photon first as it has better autocomplete)
            List<GeocodingResponse> merged = new ArrayList<>();
            Set<String> seen = new HashSet<>();

            // Add Photon results first (typically better for autocomplete)
            for (GeocodingResponse r : photonResults) {
                String key = deduplicationKey(r);
                if (seen.add(key)) {
                    merged.add(r);
                }
            }
            // Then add Nominatim results that aren't duplicates
            for (GeocodingResponse r : nominatimResults) {
                String key = deduplicationKey(r);
                if (seen.add(key)) {
                    merged.add(r);
                }
            }

            // Limit total results
            return merged.stream().limit(12).collect(Collectors.toList());
        } catch (Exception e) {
            // If concurrent fetch fails, fall back to Nominatim only
            return forwardGeocodeNominatim(query);
        }
    }

    private String deduplicationKey(GeocodingResponse r) {
        // Deduplicate by rounding coordinates to ~11m precision
        if (r.latitude() == null || r.longitude() == null) {
            return r.addressText() != null ? r.addressText().toLowerCase().trim() : "";
        }
        return String.format("%.4f,%.4f", r.latitude(), r.longitude());
    }

    /**
     * Nominatim (OpenStreetMap) forward geocoding - free, no API key
     */
    private List<GeocodingResponse> forwardGeocodeNominatim(String query) {
        URI uri = UriComponentsBuilder.fromUriString(NOMINATIM_URL)
                .path("/search")
                .queryParam("format", "json")
                .queryParam("q", query)
                .queryParam("limit", 8)
                .queryParam("addressdetails", 1)
                .queryParam("countrycodes", "az")
                .build().encode().toUri();
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", USER_AGENT);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        try {
            ResponseEntity<List> response = this.restTemplate.exchange(uri, HttpMethod.GET, entity, List.class);
            List<Map<String, Object>> list = response.getBody();
            if (list != null) {
                List<GeocodingResponse> results = new ArrayList<>();
                for (Map<String, Object> item : list) {
                    String displayName = (String) item.get("display_name");
                    Double lat = parseDouble(item.get("lat"));
                    Double lon = parseDouble(item.get("lon"));
                    String city = extractCity(item.get("address"));
                    results.add(GeocodingResponse.builder()
                            .addressText(displayName)
                            .city(city)
                            .latitude(lat)
                            .longitude(lon)
                            .build());
                }
                return results;
            }
        } catch (Exception e) {
        }
        return Collections.emptyList();
    }

    /**
     * Photon (Komoot) forward geocoding - free, no API key, powered by OSM data
     * Better autocomplete results and faster response times
     */
    private List<GeocodingResponse> forwardGeocodePhoton(String query) {
        URI uri = UriComponentsBuilder.fromUriString(PHOTON_URL)
                .path("/api")
                .queryParam("q", query)
                .queryParam("limit", 8)
                .queryParam("lang", "en")
                // Bias towards Azerbaijan (Baku center)
                .queryParam("lat", 40.4093)
                .queryParam("lon", 49.8671)
                .build().encode().toUri();
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", USER_AGENT);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        try {
            ResponseEntity<Map> response = this.restTemplate.exchange(uri, HttpMethod.GET, entity, Map.class);
            Map<String, Object> body = response.getBody();
            if (body == null) return Collections.emptyList();

            Object featuresObj = body.get("features");
            if (!(featuresObj instanceof List)) return Collections.emptyList();

            List<Map<String, Object>> features = (List<Map<String, Object>>) featuresObj;
            List<GeocodingResponse> results = new ArrayList<>();

            for (Map<String, Object> feature : features) {
                Map<String, Object> properties = (Map<String, Object>) feature.get("properties");
                Map<String, Object> geometry = (Map<String, Object>) feature.get("geometry");

                if (properties == null || geometry == null) continue;

                // Build address text from Photon properties
                String name = (String) properties.get("name");
                String street = (String) properties.get("street");
                String houseNumber = (String) properties.get("housenumber");
                String city = (String) properties.get("city");
                String state = (String) properties.get("state");
                String country = (String) properties.get("country");

                StringBuilder addressBuilder = new StringBuilder();
                if (name != null) addressBuilder.append(name);
                if (street != null) {
                    if (addressBuilder.length() > 0) addressBuilder.append(", ");
                    if (houseNumber != null) addressBuilder.append(houseNumber).append(" ");
                    addressBuilder.append(street);
                }
                if (city != null) {
                    if (addressBuilder.length() > 0) addressBuilder.append(", ");
                    addressBuilder.append(city);
                }
                if (state != null) {
                    if (addressBuilder.length() > 0) addressBuilder.append(", ");
                    addressBuilder.append(state);
                }
                if (country != null) {
                    if (addressBuilder.length() > 0) addressBuilder.append(", ");
                    addressBuilder.append(country);
                }

                String addressText = addressBuilder.toString();
                if (addressText.isBlank()) continue;

                // Extract coordinates from GeoJSON geometry
                Double lat = null, lon = null;
                Object coordsObj = geometry.get("coordinates");
                if (coordsObj instanceof List) {
                    List<Number> coords = (List<Number>) coordsObj;
                    if (coords.size() >= 2) {
                        lon = coords.get(0).doubleValue(); // GeoJSON: [lon, lat]
                        lat = coords.get(1).doubleValue();
                    }
                }

                // Filter to Azerbaijan results only
                if ("Azerbaijan".equalsIgnoreCase(country) || "Azərbaycan".equalsIgnoreCase(country)
                        || country == null) {
                    results.add(GeocodingResponse.builder()
                            .addressText(addressText)
                            .city(city)
                            .latitude(lat)
                            .longitude(lon)
                            .build());
                }
            }
            return results;
        } catch (Exception e) {
        }
        return Collections.emptyList();
    }

    private Double parseDouble(Object value) {
        if (value == null) return null;
        try {
            return Double.parseDouble(value.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private String extractCity(Object addressObj) {
        if (!(addressObj instanceof Map)) return null;
        Map<String, String> address = (Map<String, String>) addressObj;
        String city = address.get("city");
        if (city == null) city = address.get("town");
        if (city == null) city = address.get("village");
        if (city == null) city = address.get("hamlet");
        if (city == null) city = address.get("suburb");
        return city;
    }
}

