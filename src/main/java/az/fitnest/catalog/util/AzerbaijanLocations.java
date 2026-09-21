package az.fitnest.catalog.util;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class AzerbaijanLocations {

    public static final String BAKI = "Bakı";

    public static final List<String> CITIES = List.of(
            "Bakı",
            "Abşeron",
            "Ağcabədi",
            "Ağdam",
            "Ağdaş",
            "Ağstafa",
            "Ağsu",
            "Astara",
            "Babək",
            "Balakən",
            "Beyləqan",
            "Bərdə",
            "Biləsuvar",
            "Cəbrayıl",
            "Cəlilabad",
            "Culfa",
            "Daşkəsən",
            "Füzuli",
            "Gədəbəy",
            "Gəncə",
            "Goranboy",
            "Göyçay",
            "Göygöl",
            "Hacıqabul",
            "Xaçmaz",
            "Xankəndi",
            "Xızı",
            "Xocalı",
            "Xocavənd",
            "İmişli",
            "İsmayıllı",
            "Kəlbəcər",
            "Kəngərli",
            "Kürdəmir",
            "Laçın",
            "Lerik",
            "Lənkəran",
            "Masallı",
            "Mingəçevir",
            "Naftalan",
            "Naxçıvan",
            "Neftçala",
            "Oğuz",
            "Ordubad",
            "Qax",
            "Qazax",
            "Qəbələ",
            "Qobustan",
            "Quba",
            "Qubadlı",
            "Qusar",
            "Saatlı",
            "Sabirabad",
            "Salyan",
            "Samux",
            "Sədərək",
            "Siyəzən",
            "Sumqayıt",
            "Şabran",
            "Şahbuz",
            "Şamaxı",
            "Şəki",
            "Şəmkir",
            "Şərur",
            "Şirvan",
            "Şuşa",
            "Tərtər",
            "Tovuz",
            "Ucar",
            "Yardımlı",
            "Yevlax",
            "Zaqatala",
            "Zəngilan",
            "Zərdab"
    );

    public static final List<String> BAKI_RAYONS = List.of(
            "Binəqədi",
            "Xətai",
            "Xəzər",
            "Qaradağ",
            "Nərimanov",
            "Nəsimi",
            "Nizami",
            "Pirallahı",
            "Sabunçu",
            "Səbail",
            "Suraxanı",
            "Yasamal"
    );

    private static final Map<String, String> ALIASES = Map.ofEntries(
            Map.entry("baku", "Bakı"),
            Map.entry("baki", "Bakı"),
            Map.entry("bakı", "Bakı"),
            Map.entry("ganja", "Gəncə"),
            Map.entry("gence", "Gəncə"),
            Map.entry("gəncə", "Gəncə"),
            Map.entry("sumgait", "Sumqayıt"),
            Map.entry("sumqayit", "Sumqayıt"),
            Map.entry("sumqayıt", "Sumqayıt"),
            Map.entry("mingachevir", "Mingəçevir"),
            Map.entry("nakhchivan", "Naxçıvan"),
            Map.entry("nakhichevan", "Naxçıvan"),
            Map.entry("sheki", "Şəki"),
            Map.entry("shaki", "Şəki"),
            Map.entry("lankaran", "Lənkəran")
    );

    private static final Map<String, String> RAYON_ALIASES = Map.ofEntries(
            Map.entry("bineqedi", "Binəqədi"),
            Map.entry("binagadi", "Binəqədi"),
            Map.entry("binagady", "Binəqədi"),
            Map.entry("khatai", "Xətai"),
            Map.entry("xetai", "Xətai"),
            Map.entry("hetey", "Xətai"),
            Map.entry("khazar", "Xəzər"),
            Map.entry("xezer", "Xəzər"),
            Map.entry("garadagh", "Qaradağ"),
            Map.entry("qaradag", "Qaradağ"),
            Map.entry("karadag", "Qaradağ"),
            Map.entry("narimanov", "Nərimanov"),
            Map.entry("nerimanov", "Nərimanov"),
            Map.entry("nasimi", "Nəsimi"),
            Map.entry("nesimi", "Nəsimi"),
            Map.entry("nizami", "Nizami"),
            Map.entry("pirallahi", "Pirallahı"),
            Map.entry("pirallahy", "Pirallahı"),
            Map.entry("sabunchu", "Sabunçu"),
            Map.entry("sabuncu", "Sabunçu"),
            Map.entry("sabail", "Səbail"),
            Map.entry("sebail", "Səbail"),
            Map.entry("surakhani", "Suraxanı"),
            Map.entry("suraxani", "Suraxanı"),
            Map.entry("yasamal", "Yasamal")
    );

    private AzerbaijanLocations() {
    }

    public static boolean isBaki(String city) {
        String canonical = canonical(city);
        return BAKI.equals(canonical);
    }

    public static boolean matches(String storedCity, String selectedCity) {
        if (selectedCity == null || selectedCity.isBlank()) {
            return true;
        }
        if (storedCity == null || storedCity.isBlank()) {
            return false;
        }
        String selected = normalize(selectedCity);
        String stored = normalize(storedCity);
        if (stored.equals(selected)) {
            return true;
        }
        String canonicalSelected = canonical(selectedCity);
        String canonicalStored = canonical(storedCity);
        if (canonicalSelected != null && canonicalSelected.equalsIgnoreCase(canonicalStored)) {
            return true;
        }
        return stored.startsWith(selected) || stored.contains(" " + selected) || stored.contains("," + selected);
    }

    public static boolean matchesRayon(String storedRayon, String selectedRayon) {
        if (selectedRayon == null || selectedRayon.isBlank()) {
            return true;
        }
        if (storedRayon == null || storedRayon.isBlank()) {
            return false;
        }
        String selected = normalize(selectedRayon);
        String stored = normalize(storedRayon);
        if (stored.equals(selected)) {
            return true;
        }
        String canonicalSelected = canonicalRayon(selectedRayon);
        String canonicalStored = canonicalRayon(storedRayon);
        return canonicalSelected != null && canonicalSelected.equalsIgnoreCase(canonicalStored);
    }

    public static String canonical(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String repaired = repairMojibake(raw).trim();
        String firstPart = repaired.split("[,/]")[0].trim()
                .replaceAll("(?iu)\\s*(şəhəri|sehəri|sheheri|city|города?)\\s*$", "")
                .trim();
        String alias = ALIASES.get(normalize(firstPart));
        if (alias != null) {
            return alias;
        }
        for (String city : CITIES) {
            String cityNorm = normalize(city);
            String valueNorm = normalize(firstPart);
            if (valueNorm.equals(cityNorm) || valueNorm.startsWith(cityNorm)) {
                return city;
            }
        }
        return repaired;
    }

    /**
     * Canonicalize a Bakı rayon name. Returns null if not a known Bakı rayon.
     */
    public static String canonicalRayon(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String repaired = repairMojibake(raw).trim();
        String cleaned = repaired
                .replaceAll("(?iu)\\s*(rayonu|rayon|district|районе?|р\\.?|r\\.?)\\s*$", "")
                .trim();
        String alias = RAYON_ALIASES.get(normalize(cleaned));
        if (alias != null) {
            return alias;
        }
        for (String rayon : BAKI_RAYONS) {
            String rayonNorm = normalize(rayon);
            String valueNorm = normalize(cleaned);
            if (valueNorm.equals(rayonNorm) || valueNorm.startsWith(rayonNorm)) {
                return rayon;
            }
        }
        return null;
    }

    /**
     * Apply city+rayon rules: canonicalize city; keep rayon only for Bakı.
     */
    public static String[] normalizeCityAndRayon(String cityRaw, String rayonRaw) {
        String city = canonical(cityRaw);
        String rayon = null;
        if (isBaki(city)) {
            rayon = canonicalRayon(rayonRaw);
        }
        return new String[]{city, rayon};
    }

    public static String repairMojibake(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        if (!looksMojibake(value)) {
            return value;
        }
        try {
            String repaired = new String(value.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
            if (!repaired.isBlank() && !looksMojibake(repaired)) {
                return repaired;
            }
        } catch (Exception ignored) {
        }
        return value;
    }

    private static boolean looksMojibake(String value) {
        return value.contains("Ã") || value.contains("Â") || value.contains("Ä") || value.contains("Å")
                || value.contains("�") || value.contains("kÃ") || value.contains("Ã¼") || value.contains("Ã¶");
    }

    private static String normalize(String value) {
        String folded = Normalizer.normalize(value.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFKD)
                .replaceAll("\\p{M}+", "")
                .replace('ə', 'e')
                .replace('ı', 'i')
                .replace('ö', 'o')
                .replace('ü', 'u')
                .replace('ğ', 'g')
                .replace('ş', 's')
                .replace('ç', 'c');
        return folded.replaceAll("[^a-z0-9]+", "");
    }
}
