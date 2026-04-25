package az.fitnest.catalog.dto.admin;

public record WorkingHourDto(
        Long id,
        Integer dayOfWeek,
        String dayLabel,
        String openTime,
        String closeTime,
        Boolean isClosed
) {
}
