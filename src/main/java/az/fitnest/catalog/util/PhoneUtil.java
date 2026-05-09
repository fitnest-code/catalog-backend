package az.fitnest.catalog.util;

public class PhoneUtil {

    public static String normalize(String phone) {
        if (phone == null || phone.isBlank()) {
            return phone;
        }

        String digits = phone.replaceAll("\\D", "");

        String normalized;

        if (digits.length() == 12 && digits.startsWith("994")) {
            normalized = "+" + digits;
        }
        else if (digits.length() == 10 && digits.startsWith("0")) {
            normalized = "+994" + digits.substring(1);
        }
        else if (digits.length() == 9) {
            normalized = "+994" + digits;
        }
        else {
            if (phone.startsWith("+") && digits.length() >= 9) {
                return "+" + digits;
            }
            return phone;
        }

        return normalized;
    }
}
