package az.fitnest.catalog.dto.dashboard;

public record DashboardTopGymItem(
        Long gymId,
        String gymName,
        String category,
        String packages,
        long qrCount,
        long uniqueCustomers,
        double amount,
        Double trendPct
) {}
