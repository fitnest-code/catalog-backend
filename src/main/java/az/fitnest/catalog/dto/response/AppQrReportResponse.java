package az.fitnest.catalog.dto.response;

public record AppQrReportResponse(
        long lightCount,
        long darkCount,
        long totalCount,
        String lightQrCodeUrl,
        String darkQrCodeUrl
) {}
