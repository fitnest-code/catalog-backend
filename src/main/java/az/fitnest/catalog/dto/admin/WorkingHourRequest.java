package az.fitnest.catalog.dto.admin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record WorkingHourRequest(
        @NotNull(message = "həftənin günü boş ola bilməz")
        @Min(value = 1, message = "həftənin günü minimum 1 olmalıdır")
        @Max(value = 7, message = "həftənin günü maksimum 7 olmalıdır")
        Integer dayOfWeek,

        String openTime,
        String closeTime,

        @NotNull(message = "isClosed boş ola bilməz")
        Boolean isClosed
) {
}
