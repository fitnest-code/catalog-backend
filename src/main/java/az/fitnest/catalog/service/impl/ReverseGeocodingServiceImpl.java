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
    private final RestTemplate restTemplate;

    public ReverseGeocodingServiceImpl() {
        org.springframework.http.client.SimpleClientHttpRequestFactory factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(1000);
        factory.setReadTimeout(1500);
        this.restTemplate = new RestTemplate(factory);
    }

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
                .queryParam("accept-language", "az,ru,en")
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

    private String transliterateToAzerbaijani(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }
        
        String original = query.toLowerCase().trim();
        StringBuilder sb = new StringBuilder();
        
        for (int i = 0; i < original.length(); i++) {
            char c = original.charAt(i);
            
            if (c == 's' && i + 1 < original.length() && original.charAt(i + 1) == 'h') {
                sb.append('ş');
                i++;
            } else if (c == 'c' && i + 1 < original.length() && original.charAt(i + 1) == 'h') {
                sb.append('ç');
                i++;
            } else if (c == 'g' && i + 1 < original.length() && original.charAt(i + 1) == 'h') {
                sb.append('ğ');
                i++;
            } else if (c == 'k' && i + 1 < original.length() && original.charAt(i + 1) == 'h') {
                sb.append('x');
                i++;
            } else {
                switch (c) {
                    case 's':
                        sb.append('ş');
                        break;
                    case 'c':
                        sb.append('ç');
                        break;
                    case 'g':
                        sb.append('ğ');
                        break;
                    case 'u':
                        sb.append('ü');
                        break;
                    case 'o':
                        sb.append('ö');
                        break;
                    case 'i':
                        sb.append('ı');
                        break;
                    case 'e':
                        sb.append('ə');
                        break;
                    case 'a':
                        // convert 'a' to 'ə' if it looks like the start of a syllable/word, otherwise keep 'a'
                        if (i == 0 || original.charAt(i - 1) == ' ' || original.charAt(i - 1) == '-') {
                            sb.append('ə');
                        } else {
                            sb.append('a');
                        }
                        break;
                    default:
                        sb.append(c);
                        break;
                }
            }
        }
        return sb.toString();
    }

    private static final java.util.regex.Pattern STREET_KEYWORD_PATTERN = java.util.regex.Pattern.compile(
            "(?i)(küçəsi|küçesi|kucesi|kücəsi|prospekti|prospekt|pr\\.|bulvarı|bulvari|bulvar|blv\\.|" +
            "yolu|döngəsi|dongesi|məhəlləsi|mehellesi|sahəsi|sahesi|massivi|magistralı|magistrali|" +
            "şossesi|shossesi|street|st\\.|avenue|ave\\.|road|rd\\.)"
    );

    private static final java.util.regex.Pattern HOUSE_NUMBER_PATTERN = java.util.regex.Pattern.compile(
            "(?i)\\b(?:ev|bina|mənzil|menzil|no|no\\.|№|nömrə|nömre|apt|apartment)?\\s*(\\d+(?:/[a-zA-Z0-9]+|-[a-zA-Z0-9]+|[a-zA-Z])?)\\b"
    );

    private List<GeocodingResponse> queryGeocodingApis(String query) {
        CompletableFuture<List<GeocodingResponse>> nominatimFuture =
                CompletableFuture.supplyAsync(() -> forwardGeocodeNominatim(query));
        CompletableFuture<List<GeocodingResponse>> photonFuture =
                CompletableFuture.supplyAsync(() -> forwardGeocodePhoton(query));

        try {
            List<GeocodingResponse> nominatimResults = nominatimFuture.get(3, java.util.concurrent.TimeUnit.SECONDS);
            List<GeocodingResponse> photonResults = photonFuture.get(3, java.util.concurrent.TimeUnit.SECONDS);

            List<GeocodingResponse> merged = new ArrayList<>();
            Set<String> seen = new HashSet<>();

            for (GeocodingResponse r : photonResults) {
                if (seen.add(deduplicationKey(r))) {
                    merged.add(r);
                }
            }
            for (GeocodingResponse r : nominatimResults) {
                if (seen.add(deduplicationKey(r))) {
                    merged.add(r);
                }
            }
            return merged;
        } catch (Exception e) {
            return forwardGeocodeNominatim(query);
        }
    }

    /**
     * Nominatim structured search using street= and city= parameters.
     * Much more precise for actual street addresses than free-form q= search.
     */
    private List<GeocodingResponse> forwardGeocodeNominatimStructured(String streetWithNumber, String city) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(NOMINATIM_URL)
                .path("/search")
                .queryParam("format", "json")
                .queryParam("street", streetWithNumber)
                .queryParam("addressdetails", 1)
                .queryParam("countrycodes", "az")
                .queryParam("limit", 5);
        if (city != null && !city.isBlank()) {
            builder.queryParam("city", city);
        }
        URI uri = builder.build().encode().toUri();
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
                    String resultCity = extractCity(item.get("address"));
                    results.add(GeocodingResponse.builder()
                            .addressText(displayName)
                            .city(resultCity)
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
     * Parse Azerbaijani street keywords from query to enable structured search.
     * Returns [streetName, houseNumber] or null if no street pattern detected.
     */
    private String[] parseStreetQuery(String query) {
        java.util.regex.Matcher streetMatcher = STREET_KEYWORD_PATTERN.matcher(query);
        if (!streetMatcher.find()) {
            return null;
        }
        // Extract house number if present
        java.util.regex.Matcher numberMatcher = HOUSE_NUMBER_PATTERN.matcher(query);
        String houseNumber = null;
        String streetPart = query;
        if (numberMatcher.find()) {
            houseNumber = numberMatcher.group(1);
            streetPart = query.substring(0, numberMatcher.start()) + query.substring(numberMatcher.end());
            streetPart = streetPart.replaceAll("\\s+", " ").trim();
        }
        // Build the structured street string: "houseNumber streetName"
        String structuredStreet = houseNumber != null ? (houseNumber + " " + streetPart) : streetPart;
        return new String[]{structuredStreet, houseNumber, streetPart};
    }

    private List<GeocodingResponse> getGeocodingResults(String query) {
        if (query == null || query.isBlank()) {
            return Collections.emptyList();
        }

        // Run all search strategies concurrently
        CompletableFuture<List<GeocodingResponse>> freeFormFuture =
                CompletableFuture.supplyAsync(() -> queryGeocodingApis(query));

        // Try structured search if query looks like a street address
        String[] parsed = parseStreetQuery(query);
        CompletableFuture<List<GeocodingResponse>> structuredFuture = (parsed != null)
                ? CompletableFuture.supplyAsync(() -> {
                    List<GeocodingResponse> results = new ArrayList<>();
                    // Try structured with Baku first (most common)
                    results.addAll(forwardGeocodeNominatimStructured(parsed[0], "Bakı"));
                    if (results.isEmpty()) {
                        results.addAll(forwardGeocodeNominatimStructured(parsed[0], "Baku"));
                    }
                    if (results.isEmpty()) {
                        // Try without city constraint
                        results.addAll(forwardGeocodeNominatimStructured(parsed[0], null));
                    }
                    return results;
                })
                : CompletableFuture.completedFuture(Collections.emptyList());

        // Try transliterated query
        String transliterated = transliterateToAzerbaijani(query);
        CompletableFuture<List<GeocodingResponse>> transFuture =
                (transliterated != null && !transliterated.equalsIgnoreCase(query))
                        ? CompletableFuture.supplyAsync(() -> queryGeocodingApis(transliterated))
                        : CompletableFuture.completedFuture(Collections.emptyList());

        // Also try structured with transliterated query
        String[] parsedTrans = (transliterated != null) ? parseStreetQuery(transliterated) : null;
        CompletableFuture<List<GeocodingResponse>> structuredTransFuture = (parsedTrans != null)
                ? CompletableFuture.supplyAsync(() -> {
                    List<GeocodingResponse> results = new ArrayList<>();
                    results.addAll(forwardGeocodeNominatimStructured(parsedTrans[0], "Bakı"));
                    if (results.isEmpty()) {
                        results.addAll(forwardGeocodeNominatimStructured(parsedTrans[0], null));
                    }
                    return results;
                })
                : CompletableFuture.completedFuture(Collections.emptyList());

        try {
            List<GeocodingResponse> freeFormResults = freeFormFuture.get(4, java.util.concurrent.TimeUnit.SECONDS);
            List<GeocodingResponse> structuredResults = structuredFuture.get(4, java.util.concurrent.TimeUnit.SECONDS);
            List<GeocodingResponse> transResults = transFuture.get(4, java.util.concurrent.TimeUnit.SECONDS);
            List<GeocodingResponse> structuredTransResults = structuredTransFuture.get(4, java.util.concurrent.TimeUnit.SECONDS);

            // Merge: structured results first (most precise), then free-form, then transliterated
            List<GeocodingResponse> merged = new ArrayList<>();
            Set<String> seen = new HashSet<>();

            for (GeocodingResponse r : structuredResults) {
                if (seen.add(deduplicationKey(r))) merged.add(r);
            }
            for (GeocodingResponse r : structuredTransResults) {
                if (seen.add(deduplicationKey(r))) merged.add(r);
            }
            for (GeocodingResponse r : freeFormResults) {
                if (seen.add(deduplicationKey(r))) merged.add(r);
            }
            for (GeocodingResponse r : transResults) {
                if (seen.add(deduplicationKey(r))) merged.add(r);
            }

            // Score and sort: prioritize results whose addressText contains the search terms
            String queryLower = query.toLowerCase();
            merged.sort((a, b) -> {
                int scoreA = scoreResult(a, queryLower);
                int scoreB = scoreResult(b, queryLower);
                return Integer.compare(scoreB, scoreA); // Higher score first
            });

            return merged;
        } catch (Exception e) {
            return queryGeocodingApis(query);
        }
    }

    /**
     * Score a geocoding result for relevance to the query.
     * Higher = more relevant.
     */
    private int scoreResult(GeocodingResponse result, String queryLower) {
        if (result.addressText() == null) return 0;
        String addr = result.addressText().toLowerCase();
        int score = 0;

        // Split query into words and count matches
        String[] queryWords = queryLower.split("\\s+");
        for (String word : queryWords) {
            if (word.length() < 2) continue;
            if (addr.contains(word)) {
                score += 10;
            }
        }

        // Bonus for street-type results (contain küçə, prospekt, etc.)
        if (STREET_KEYWORD_PATTERN.matcher(addr).find()) {
            score += 15;
        }

        // Bonus if address starts with one of the query words (strong match)
        for (String word : queryWords) {
            if (word.length() >= 3 && addr.startsWith(word)) {
                score += 20;
                break;
            }
        }

        // Penalize very long addresses (usually distant/irrelevant locations)
        if (addr.length() > 120) {
            score -= 5;
        }

        return score;
    }

    @Override
    public List<GeocodingResponse> forwardGeocode(String query) {
        if (query == null || query.isBlank()) {
            return Collections.emptyList();
        }

        List<GeocodingResponse> originalResults = getGeocodingResults(query);

        // Try to detect and strip house/street number if present
        java.util.regex.Matcher matcher = HOUSE_NUMBER_PATTERN.matcher(query);
        String houseNumber = null;
        String strippedQuery = query;
        if (matcher.find()) {
            houseNumber = matcher.group(1);
            strippedQuery = query.substring(0, matcher.start()) + query.substring(matcher.end());
            strippedQuery = strippedQuery.replaceAll("\\s+", " ").replaceAll(",\\s*,", ",").trim();
            if (strippedQuery.endsWith(",")) {
                strippedQuery = strippedQuery.substring(0, strippedQuery.length() - 1).trim();
            }
        }

        if (houseNumber != null && !strippedQuery.isBlank() && !strippedQuery.equals(query)) {
            List<GeocodingResponse> strippedResults = getGeocodingResults(strippedQuery);
            if (!strippedResults.isEmpty()) {
                final String finalHouseNumber = houseNumber;
                List<GeocodingResponse> augmentedResults = new ArrayList<>();
                for (GeocodingResponse r : strippedResults) {
                    String addressText = r.addressText();
                    if (addressText != null && !addressText.contains(finalHouseNumber)) {
                        String[] parts = addressText.split(",", 2);
                        String firstPart = parts[0].trim();
                        String updatedFirstPart = firstPart + " " + finalHouseNumber;
                        String newAddressText = parts.length > 1 ? updatedFirstPart + ", " + parts[1].trim() : updatedFirstPart;
                        augmentedResults.add(GeocodingResponse.builder()
                                .addressText(newAddressText)
                                .city(r.city())
                                .latitude(r.latitude())
                                .longitude(r.longitude())
                                .build());
                    } else {
                        augmentedResults.add(r);
                    }
                }

                List<GeocodingResponse> merged = new ArrayList<>();
                Set<String> seen = new HashSet<>();

                for (GeocodingResponse r : augmentedResults) {
                    if (seen.add(deduplicationKey(r))) {
                        merged.add(r);
                    }
                }
                for (GeocodingResponse r : originalResults) {
                    if (seen.add(deduplicationKey(r))) {
                        merged.add(r);
                    }
                }
                return merged.stream().limit(12).collect(Collectors.toList());
            }
        }

        return originalResults.stream().limit(12).collect(Collectors.toList());
    }

    private String deduplicationKey(GeocodingResponse r) {
        if (r.latitude() == null || r.longitude() == null) {
            return r.addressText() != null ? r.addressText().toLowerCase().trim() : "";
        }
        return String.format("%.4f,%.4f", r.latitude(), r.longitude());
    }

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

    private List<GeocodingResponse> forwardGeocodePhoton(String query) {
        URI uri = UriComponentsBuilder.fromUriString(PHOTON_URL)
                .path("/api")
                .queryParam("q", query)
                .queryParam("limit", 8)
                .queryParam("countrycode", "az")
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

                Double lat = null, lon = null;
                Object coordsObj = geometry.get("coordinates");
                if (coordsObj instanceof List) {
                    List<Number> coords = (List<Number>) coordsObj;
                    if (coords.size() >= 2) {
                        lon = coords.get(0).doubleValue();
                        lat = coords.get(1).doubleValue();
                    }
                }

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
