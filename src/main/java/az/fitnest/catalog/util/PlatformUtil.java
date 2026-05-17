package az.fitnest.catalog.util;

public final class PlatformUtil {

    private PlatformUtil() {
    }

    public static String detectPlatform(String userAgent) {
        if (userAgent == null) return "Unknown";
        String ua = userAgent.toLowerCase();
        if (ua.contains("iphone") || ua.contains("ipad") || ua.contains("ipod") || ua.contains("ios")) return "iOS";
        if (ua.contains("android")) return "Android";
        if (ua.contains("windows")) return "Windows";
        if (ua.contains("macintosh") || ua.contains("mac os")) return "macOS";
        if (ua.contains("linux")) return "Linux";
        return "Web/Other";
    }
}
