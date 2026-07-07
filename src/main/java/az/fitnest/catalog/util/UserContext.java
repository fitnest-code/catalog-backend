package az.fitnest.catalog.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.HttpServletRequest;

public class UserContext {

    private static az.fitnest.catalog.client.UserServiceGrpcClient userServiceGrpcClient;

    public static void setUserServiceGrpcClient(az.fitnest.catalog.client.UserServiceGrpcClient client) {
        userServiceGrpcClient = client;
    }

    public static Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Long) {
            return (Long) auth.getPrincipal();
        }
        return null;
    }

    public static boolean isTestUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        return auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_TEST".equals(a.getAuthority()) || "TEST".equals(a.getAuthority()));
    }

    public static Long extractUserId(Object principal) {
        if (principal instanceof Long) {
            return (Long) principal;
        }
        return null;
    }

    public static String getUserLanguage() {
        try {
            var requestAttributes = RequestContextHolder.getRequestAttributes();
            if (requestAttributes instanceof ServletRequestAttributes) {
                HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
                String acceptLanguage = request.getHeader("Accept-Language");
                if (acceptLanguage != null && !acceptLanguage.trim().isEmpty()) {
                    String upper = acceptLanguage.trim().split("[,;-]")[0].toUpperCase();
                    if (upper.equals("EN") || upper.equals("RU") || upper.equals("AZ")) {
                        return upper;
                    }
                }
            }
        } catch (Exception ignored) {
        }

        Long userId = getCurrentUserId();
        if (userId != null && userServiceGrpcClient != null) {
            try {
                az.fitnest.catalog.client.CachedUser user = userServiceGrpcClient.getUserById(userId);
                if (user != null && user.getLanguage() != null && !user.getLanguage().isBlank()) {
                    String lang = user.getLanguage().toUpperCase().trim();
                    if (lang.equals("EN") || lang.equals("RU") || lang.equals("AZ")) {
                        return lang;
                    }
                }
            } catch (Exception ignored) {
            }
        }

        try {
            java.util.Locale locale = LocaleContextHolder.getLocale();
            if (locale != null && locale.getLanguage() != null) {
                String lang = locale.getLanguage().toUpperCase();
                if (lang.equals("EN") || lang.equals("RU") || lang.equals("AZ")) {
                    return lang;
                }
            }
        } catch (Exception ignored) {
        }

        return "AZ";
    }
}


