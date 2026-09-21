package az.fitnest.catalog.service.impl;

import az.fitnest.catalog.dto.response.GeocodingResponse;
import az.fitnest.catalog.dto.*;
import az.fitnest.catalog.dto.request.*;
import az.fitnest.catalog.dto.response.*;
import az.fitnest.catalog.service.ReverseGeocodingService;
import az.fitnest.catalog.util.AzerbaijanLocations;
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
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(3000);
        this.restTemplate = new RestTemplate(factory);
        this.restTemplate.getMessageConverters().add(0,
                new org.springframework.http.converter.StringHttpMessageConverter(java.nio.charset.StandardCharsets.UTF_8));
        this.restTemplate.getMessageConverters().stream()
                .filter(org.springframework.http.converter.json.MappingJackson2HttpMessageConverter.class::isInstance)
                .map(org.springframework.http.converter.json.MappingJackson2HttpMessageConverter.class::cast)
                .forEach(converter -> converter.setDefaultCharset(java.nio.charset.StandardCharsets.UTF_8));
    }

    private GeocodingResponse reverseGeocodePhoton(Double latitude, Double longitude) {
        URI uri = UriComponentsBuilder.fromUriString(PHOTON_URL)
                .path("/reverse")
                .queryParam("lat", latitude)
                .queryParam("lon", longitude)
                .build().encode().toUri();
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", USER_AGENT);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        try {
            ResponseEntity<Map> response = this.restTemplate.exchange(uri, HttpMethod.GET, entity, Map.class);
            Map<String, Object> body = response.getBody();
            if (body != null) {
                Object featuresObj = body.get("features");
                if (featuresObj instanceof List) {
                    List<Map<String, Object>> features = (List<Map<String, Object>>) featuresObj;
                    if (!features.isEmpty()) {
                        Map<String, Object> feature = features.get(0);
                        Map<String, Object> properties = (Map<String, Object>) feature.get("properties");
                        if (properties != null) {
                            return buildFromPhotonProperties(properties, latitude, longitude);
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Ignore and fall back
        }
        return null;
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
                Object addressObj = body.get("address");
                String street = buildStreetAddress(addressObj, (String) body.get("display_name"));
                String city = extractCity(addressObj);
                String rayon = extractRayon(addressObj, city);
                String[] normalized = AzerbaijanLocations.normalizeCityAndRayon(city, rayon);
                return GeocodingResponse.builder()
                        .addressText(street)
                        .city(normalized[0])
                        .rayon(normalized[1])
                        .latitude(latitude)
                        .longitude(longitude)
                        .build();
            }
        } catch (Exception exception) {
            // Fall through to Photon
        }

        GeocodingResponse photonFallback = reverseGeocodePhoton(latitude, longitude);
        if (photonFallback != null) {
            return photonFallback;
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
                    Double lat = parseDouble(item.get("lat"));
                    Double lon = parseDouble(item.get("lon"));
                    results.add(fromNominatimItem(item, lat, lon));
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
                                .rayon(r.rayon())
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
                    Double lat = parseDouble(item.get("lat"));
                    Double lon = parseDouble(item.get("lon"));
                    results.add(fromNominatimItem(item, lat, lon));
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

                String country = (String) properties.get("country");
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
                    GeocodingResponse built = buildFromPhotonProperties(properties, lat, lon);
                    if (built != null) {
                        results.add(built);
                    }
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

    private GeocodingResponse fromNominatimItem(Map<String, Object> item, Double lat, Double lon) {
        Object addressObj = item.get("address");
        String street = buildStreetAddress(addressObj, (String) item.get("display_name"));
        String city = extractCity(addressObj);
        String rayon = extractRayon(addressObj, city);
        String[] normalized = AzerbaijanLocations.normalizeCityAndRayon(city, rayon);
        return GeocodingResponse.builder()
                .addressText(street)
                .city(normalized[0])
                .rayon(normalized[1])
                .latitude(lat)
                .longitude(lon)
                .build();
    }

    private GeocodingResponse buildFromPhotonProperties(Map<String, Object> properties, Double lat, Double lon) {
        String name = (String) properties.get("name");
        String street = (String) properties.get("street");
        String houseNumber = (String) properties.get("housenumber");
        String cityRaw = (String) properties.get("city");
        String district = (String) properties.get("district");
        if (district == null) district = (String) properties.get("suburb");
        if (district == null) district = (String) properties.get("county");

        StringBuilder streetBuilder = new StringBuilder();
        if (street != null && !street.isBlank()) {
            if (houseNumber != null && !houseNumber.isBlank()) {
                streetBuilder.append(street.trim()).append(" ").append(houseNumber.trim());
            } else {
                streetBuilder.append(street.trim());
            }
        } else if (name != null && !name.isBlank()) {
            streetBuilder.append(name.trim());
            if (houseNumber != null && !houseNumber.isBlank()) {
                streetBuilder.append(" ").append(houseNumber.trim());
            }
        }

        String addressText = streetBuilder.toString();
        if (addressText.isBlank()) {
            return null;
        }

        String[] normalized = AzerbaijanLocations.normalizeCityAndRayon(cityRaw, district);
        return GeocodingResponse.builder()
                .addressText(addressText)
                .city(normalized[0])
                .rayon(normalized[1])
                .latitude(lat)
                .longitude(lon)
                .build();
    }

    private String buildStreetAddress(Object addressObj, String displayNameFallback) {
        if (addressObj instanceof Map) {
            Map<?, ?> address = (Map<?, ?>) addressObj;
            String road = stringVal(address, "road");
            if (road == null) road = stringVal(address, "pedestrian");
            if (road == null) road = stringVal(address, "path");
            if (road == null) road = stringVal(address, "footway");
            if (road == null) road = stringVal(address, "residential");
            String houseNumber = stringVal(address, "house_number");
            if (road != null && !road.isBlank()) {
                if (houseNumber != null && !houseNumber.isBlank()) {
                    return road.trim() + " " + houseNumber.trim();
                }
                return road.trim();
            }
            String amenity = stringVal(address, "amenity");
            if (amenity == null) amenity = stringVal(address, "building");
            if (amenity != null && !amenity.isBlank()) {
                return amenity.trim();
            }
        }
        return stripCityRayonFromDisplay(displayNameFallback);
    }

    private String stripCityRayonFromDisplay(String displayName) {
        if (displayName == null || displayName.isBlank()) {
            return displayName;
        }
        String[] parts = displayName.split(",");
        if (parts.length == 0) {
            return displayName.trim();
        }
        // Keep first segment as street-ish; drop trailing city/country noise
        return parts[0].trim();
    }

    private String stringVal(Map<?, ?> map, String key) {
        Object val = map.get(key);
        return val == null ? null : val.toString();
    }

    private String extractCity(Object addressObj) {
        if (!(addressObj instanceof Map)) return null;
        Map<?, ?> address = (Map<?, ?>) addressObj;
        String city = stringVal(address, "city");
        if (city == null) city = stringVal(address, "town");
        if (city == null) city = stringVal(address, "municipality");
        if (city == null) city = stringVal(address, "village");
        if (city == null) city = stringVal(address, "hamlet");
        return city;
    }

    private String extractRayon(Object addressObj, String city) {
        if (!(addressObj instanceof Map)) return null;
        Map<?, ?> address = (Map<?, ?>) addressObj;
        String rayon = stringVal(address, "city_district");
        if (rayon == null) rayon = stringVal(address, "suburb");
        if (rayon == null) rayon = stringVal(address, "district");
        if (rayon == null) rayon = stringVal(address, "borough");
        if (rayon == null) rayon = stringVal(address, "quarter");
        // Only keep if it maps to a Bakı rayon (or city is Bakı)
        String canonicalCity = AzerbaijanLocations.canonical(city);
        if (!AzerbaijanLocations.BAKI.equals(canonicalCity) && AzerbaijanLocations.canonicalRayon(rayon) == null) {
            return null;
        }
        return rayon;
    }
}
