package az.fitnest.catalog.service;

import az.fitnest.catalog.dto.GeocodingResponse;

import java.net.URI;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class ReverseGeocodingService {
    private static final String BASE_URL = "https://nominatim.openstreetmap.org";
    private static final String USER_AGENT = "fitnest-catalog-service";
    private final RestTemplate restTemplate = new RestTemplate();

    public GeocodingResponse reverseGeocode(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            return null;
        }
        URI uri = UriComponentsBuilder.fromUriString((String) BASE_URL)
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
                        .build();
            }
        } catch (Exception exception) {
        }
        return GeocodingResponse.builder()
                .addressText(String.format("%.5f, %.5f", latitude, longitude))
                .build();
    }
}
