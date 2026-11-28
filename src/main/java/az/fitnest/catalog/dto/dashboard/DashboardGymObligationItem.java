package az.fitnest.catalog.dto.dashboard;

public record DashboardGymObligationItem(
        Long gymId,
        String gymName,
        long qrCount,
        double amount
) {}
