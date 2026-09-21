package az.fitnest.catalog.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Splits legacy free-text addresses into city / rayon / street parts.
 * Example: "Bakı şəhəri, Nizami rayonu, x küçəsi 95"
 */
public final class AddressParseUtil {

    private static final Pattern CITY_SUFFIX = Pattern.compile(
            "(?iu)\\s*(şəhəri|sehəri|sheheri|city|города?)\\s*$");
    private static final Pattern RAYON_SUFFIX = Pattern.compile(
            "(?iu)\\s*(rayonu|rayon|district|районе?|р\\.?|r\\.?)\\s*$");

    private AddressParseUtil() {
    }

    public record ParsedAddress(String city, String rayon, String addressText) {
    }

    public static ParsedAddress parse(String cityColumn, String addressText) {
        String existingCity = AzerbaijanLocations.canonical(cityColumn);
        String existingRayon = AzerbaijanLocations.canonicalRayon(cityColumn);

        String raw = AzerbaijanLocations.repairMojibake(addressText);
        if (raw == null || raw.isBlank()) {
            return new ParsedAddress(existingCity, null, null);
        }

        String[] parts = raw.split(",");
        String foundCity = existingCity;
        String foundRayon = null;
        List<String> streetParts = new ArrayList<>();

        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) continue;

            String withoutCitySuffix = CITY_SUFFIX.matcher(trimmed).replaceFirst("").trim();
            String cityCandidate = AzerbaijanLocations.canonical(withoutCitySuffix);
            if (cityCandidate != null && AzerbaijanLocations.CITIES.contains(cityCandidate)
                    && (foundCity == null || AzerbaijanLocations.isBaki(cityCandidate) || foundCity.equals(cityCandidate))) {
                // Prefer explicit Bakı / known city tokens from the string
                if (foundCity == null || AzerbaijanLocations.isBaki(cityCandidate) || !AzerbaijanLocations.CITIES.contains(foundCity)) {
                    foundCity = cityCandidate;
                }
                continue;
            }

            String withoutRayonSuffix = RAYON_SUFFIX.matcher(trimmed).replaceFirst("").trim();
            String rayonCandidate = AzerbaijanLocations.canonicalRayon(withoutRayonSuffix);
            if (rayonCandidate != null) {
                foundRayon = rayonCandidate;
                continue;
            }

            // Also try matching rayon names embedded without suffix
            rayonCandidate = AzerbaijanLocations.canonicalRayon(trimmed);
            if (rayonCandidate != null && trimmed.equalsIgnoreCase(rayonCandidate)) {
                foundRayon = rayonCandidate;
                continue;
            }

            streetParts.add(trimmed);
        }

        if (foundCity == null) {
            foundCity = existingCity;
        }

        // Only keep rayon for Bakı
        if (!AzerbaijanLocations.isBaki(foundCity)) {
            foundRayon = null;
        } else if (foundRayon == null && existingRayon != null) {
            foundRayon = existingRayon;
        }

        String street = streetParts.isEmpty() ? null : String.join(", ", streetParts);
        if (street == null || street.isBlank()) {
            // If everything was city/rayon, keep a cleaned remnant of the original street-looking first part
            street = stripKnownPrefixes(raw, foundCity, foundRayon);
        }

        return new ParsedAddress(foundCity, foundRayon, street);
    }

    private static String stripKnownPrefixes(String raw, String city, String rayon) {
        String result = raw;
        if (city != null) {
            result = result.replaceAll("(?iu)\\b" + Pattern.quote(city) + "(\\s*(şəhəri|sehəri|city))?\\b,?", "");
        }
        if (rayon != null) {
            result = result.replaceAll("(?iu)\\b" + Pattern.quote(rayon) + "(\\s*(rayonu|rayon|district))?\\b,?", "");
        }
        result = result.replaceAll("^[,\\s]+|[,\\s]+$", "").replaceAll("\\s*,\\s*", ", ").trim();
        return result.isBlank() ? null : result;
    }
}
