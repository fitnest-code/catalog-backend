package az.fitnest.catalog.util;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class AzerbaijanLocations {

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

    private AzerbaijanLocations() {
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

    public static String canonical(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String repaired = repairMojibake(raw).trim();
        String alias = ALIASES.get(normalize(repaired.split("[,/]")[0].trim()));
        if (alias != null) {
            return alias;
        }
        for (String city : CITIES) {
            String cityNorm = normalize(city);
            String valueNorm = normalize(repaired);
            if (valueNorm.equals(cityNorm) || valueNorm.startsWith(cityNorm + " ") || valueNorm.startsWith(cityNorm + ",")) {
                return city;
            }
        }
        return repaired;
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
