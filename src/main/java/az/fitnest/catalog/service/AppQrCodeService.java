package az.fitnest.catalog.service;

import az.fitnest.catalog.dto.response.AppQrReportResponse;

public interface AppQrCodeService {
    String incrementAndGetRedirectUrl(String mode, String userAgent);
    AppQrReportResponse getAppQrReport();
    byte[] getQrCodeImageBytes(String mode);
}
