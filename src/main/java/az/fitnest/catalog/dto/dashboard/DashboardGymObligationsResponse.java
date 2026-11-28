package az.fitnest.catalog.dto.dashboard;

import java.util.List;

public record DashboardGymObligationsResponse(
        double totalObligation,
        List<DashboardGymObligationItem> gyms,
        String dateFrom,
        String dateTo
) {}
