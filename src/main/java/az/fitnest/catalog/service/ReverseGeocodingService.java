/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.http.HttpEntity
 *  org.springframework.http.HttpHeaders
 *  org.springframework.http.HttpMethod
 *  org.springframework.http.ResponseEntity
 *  org.springframework.stereotype.Service
 *  org.springframework.util.MultiValueMap
 *  org.springframework.web.client.RestTemplate
 *  org.springframework.web.util.UriComponentsBuilder
 */
package az.fitnest.catalog.service;

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

    public String reverseGeocode(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            return null;
        }
        URI uri = UriComponentsBuilder.fromHttpUrl((String)BASE_URL).path("/reverse").queryParam("format", new Object[]{"json"}).queryParam("lat", new Object[]{latitude}).queryParam("lon", new Object[]{longitude}).queryParam("zoom", new Object[]{"18"}).queryParam("addressdetails", new Object[]{"0"}).build(true).toUri();
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", USER_AGENT);
        HttpEntity entity = new HttpEntity((MultiValueMap)headers);
        try {
            Object displayName;
            ResponseEntity response = this.restTemplate.exchange(uri, HttpMethod.GET, entity, Map.class);
            Object object = displayName = response.getBody() != null ? (Object)((Map)response.getBody()).get("display_name") : null;
            if (displayName != null) {
                return displayName.toString();
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        return String.format("%.5f, %.5f", latitude, longitude);
    }
}

