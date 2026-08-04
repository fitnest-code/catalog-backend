package az.fitnest.catalog.service.impl;

import az.fitnest.catalog.dto.response.AppQrReportResponse;
import az.fitnest.catalog.model.entity.AppQrCode;
import az.fitnest.catalog.repository.AppQrCodeRepository;
import az.fitnest.catalog.service.AppQrCodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppQrCodeServiceImpl implements AppQrCodeService {

    public static final String APPLE_STORE_URL = "https://apps.apple.com/az/app/fitnest-gym-health/id6768059768";
    public static final String GOOGLE_PLAY_URL = "https://play.google.com/store/apps/details?id=az.fitnest&hl=en&pli=1";

    private final AppQrCodeRepository appQrCodeRepository;

    @Override
    @Transactional
    public String incrementAndGetRedirectUrl(String mode, String userAgent) {
        String normalizedMode = normalizeMode(mode);

        // Ensure record exists, then increment scan count
        appQrCodeRepository.findByMode(normalizedMode).orElseGet(() -> {
            AppQrCode newQr = AppQrCode.builder()
                    .mode(normalizedMode)
                    .scanCount(0L)
                    .build();
            return appQrCodeRepository.save(newQr);
        });

        int updatedRows = appQrCodeRepository.incrementScanCount(normalizedMode);
        log.info("Incremented scan count for app QR code mode: {}, updated rows: {}", normalizedMode, updatedRows);

        return determineTargetUrl(userAgent);
    }

    @Override
    @Transactional(readOnly = true)
    public AppQrReportResponse getAppQrReport() {
        long lightCount = appQrCodeRepository.findByMode("LIGHT").map(AppQrCode::getScanCount).orElse(0L);
        long darkCount = appQrCodeRepository.findByMode("DARK").map(AppQrCode::getScanCount).orElse(0L);

        return new AppQrReportResponse(lightCount, darkCount, lightCount + darkCount);
    }

    private String normalizeMode(String mode) {
        if (mode == null) return "LIGHT";
        String upper = mode.trim().toUpperCase(Locale.ROOT);
        return "DARK".equals(upper) ? "DARK" : "LIGHT";
    }

    private String determineTargetUrl(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return GOOGLE_PLAY_URL;
        }

        String uaLower = userAgent.toLowerCase(Locale.ROOT);
        if (uaLower.contains("iphone") || uaLower.contains("ipad") || uaLower.contains("ipod")
                || uaLower.contains("macintosh") || uaLower.contains("os x") || uaLower.contains("ios")) {
            return APPLE_STORE_URL;
        }

        return GOOGLE_PLAY_URL;
    }
}
