package az.fitnest.catalog.dto.dashboard;

import java.util.List;

public record DashboardTopGymsResponse(
        List<DashboardTopGymItem> gyms,
        String dateFrom,
        String dateTo
) {}
