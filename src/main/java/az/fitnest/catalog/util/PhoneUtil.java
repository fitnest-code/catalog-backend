package az.fitnest.catalog.util;

public class PhoneUtil {

    public static String normalize(String phone) {
        if (phone == null || phone.isBlank()) {
            return phone;
        }

        // 1. Remove all non-digits
        String digits = phone.replaceAll("\\D", "");

        String normalized;
        
        // 2. Handle 994551234567 (12 digits)
        if (digits.length() == 12 && digits.startsWith("994")) {
            normalized = "+" + digits;
        } 
        // 3. Handle 0551234567 (10 digits)
        else if (digits.length() == 10 && digits.startsWith("0")) {
            normalized = "+994" + digits.substring(1);
        }
        // 4. Handle 551234567 (9 digits)
        else if (digits.length() == 9) {
            normalized = "+994" + digits;
        }
        else {
            // If it's already +994... just keep it as is after removing non-digits
            if (phone.startsWith("+") && digits.length() >= 9) {
                return "+" + digits;
            }
            return phone;
        }

        return normalized;
    }
}
