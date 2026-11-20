package az.fitnest.catalog.service.landing;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Tier helpers for landing API v2. Kept separate from the v1 service, whose badge intentionally
 * stays on the highest tier; v2 exposes the lowest tier that unlocks entry ("from Silver+").
 */
public final class LandingMembershipSupport {

    public static final String BRONZE = "bronze";

    private LandingMembershipSupport() {
    }

    /** Maps a (possibly localized) package name to a tier; unknown names fall back to bronze. */
    public static String membershipFromName(String name) {
        if (name == null) {
            return BRONZE;
        }
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.contains("platinum") || lower.contains("platin")) {
            return "platinum";
        }
        if (lower.contains("gold") || lower.contains("qızıl") || lower.contains("qizil")) {
            return "gold";
        }
        if (lower.contains("silver") || lower.contains("gümüş") || lower.contains("gumus")) {
            return "silver";
        }
        return BRONZE;
    }

    public static int membershipRank(String membership) {
        if (membership == null) {
            return 1;
        }
        return switch (membership) {
            case "platinum" -> 4;
            case "gold" -> 3;
            case "silver" -> 2;
            default -> 1;
        };
    }

    /** Lowest tier among the given tiers; bronze when the input is null or empty. */
    public static String minEntryMembership(Collection<String> memberships) {
        if (memberships == null || memberships.isEmpty()) {
            return BRONZE;
        }
        return memberships.stream()
                .filter(Objects::nonNull)
                .min(Comparator.comparingInt(LandingMembershipSupport::membershipRank))
                .orElse(BRONZE);
    }

    /** Distinct tiers from package names, ascending by rank; bronze when nothing resolves. */
    public static List<String> accessMembershipsSorted(Collection<String> packageNames) {
        if (packageNames == null || packageNames.isEmpty()) {
            return List.of(BRONZE);
        }
        List<String> tiers = packageNames.stream()
                .map(LandingMembershipSupport::membershipFromName)
                .distinct()
                .sorted(Comparator.comparingInt(LandingMembershipSupport::membershipRank))
                .toList();
        return tiers.isEmpty() ? List.of(BRONZE) : tiers;
    }

    /** Normalizes a filter param to a known tier, or null (= no filter) for anything else. */
    public static String normalizeTier(String value) {
        if (value == null) {
            return null;
        }
        String lower = value.trim().toLowerCase(Locale.ROOT);
        return switch (lower) {
            case "bronze", "silver", "gold", "platinum" -> lower;
            default -> null;
        };
    }
}
