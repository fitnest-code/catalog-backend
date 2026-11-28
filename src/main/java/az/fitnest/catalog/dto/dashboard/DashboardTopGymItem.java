package az.fitnest.catalog.dto.dashboard;

public record DashboardTopGymItem(
        Long gymId,
        String gymName,
        long qrCount,
        double amount
) {}
