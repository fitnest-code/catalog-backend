package az.fitnest.catalog.dto.dashboard;

import java.util.List;

public record DashboardGymObligationsResponse(
        double totalObligation,
        List<DashboardGymObligationItem> gyms,
        List<Double> sparkline,
        String dateFrom,
        String dateTo
) {}
