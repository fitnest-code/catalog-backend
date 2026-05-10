package az.fitnest.catalog.dto.response;

public record ReservationStatsResponse(
    long total,
    long pending,
    long confirmed,
    long cancelled
) {}
