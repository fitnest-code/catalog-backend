package az.fitnest.catalog.dto.v5;

import java.util.List;

/** Entrance analytics grouped by the category picked at QR scan; categoryKey "legacy" = pre-picker rows. */
public record GymCategoryAnalyticsResponseV2(
        Long gymId, long totalSuccessfulScans, long totalFailedScans, double totalRevenue, List<Row> categories) {

    public record Row(
            String categoryKey,
            Long categoryId,
            String categoryName,
            long successfulScans,
            long failedScans,
            double revenue) {}
}
