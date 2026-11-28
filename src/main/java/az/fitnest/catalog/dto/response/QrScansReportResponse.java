package az.fitnest.catalog.dto.response;

/** Admin QR entrance KPI for a date window (vs previous equal-length window). */
public record QrScansReportResponse(
        long count,
        double percentageChange,
        boolean isPositiveTrend
) {
    public QrScansReportResponse(long count) {
        this(count, 0.0, true);
    }
}
