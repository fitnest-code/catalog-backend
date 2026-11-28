package az.fitnest.catalog.dto.dashboard;

import java.util.List;

public record DashboardQrSparklineResponse(
        long totalScans,
        Double percentageChange,
        boolean isPositiveTrend,
        List<Double> sparkline,
        String dateFrom,
        String dateTo
) {}
