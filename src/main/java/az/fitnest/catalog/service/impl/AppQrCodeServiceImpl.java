package az.fitnest.catalog.service.impl;

import az.fitnest.catalog.dto.response.AppQrReportResponse;
import az.fitnest.catalog.model.entity.AppQrCode;
import az.fitnest.catalog.repository.AppQrCodeRepository;
import az.fitnest.catalog.service.AppQrCodeService;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageConfig;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppQrCodeServiceImpl implements AppQrCodeService {

    public static final String APPLE_STORE_URL = "https://apps.apple.com/az/app/fitnest-gym-health/id6768059768";
    public static final String GOOGLE_PLAY_URL = "https://play.google.com/store/apps/details?id=az.fitnest&hl=en&pli=1";

    private final AppQrCodeRepository appQrCodeRepository;

    @Value("${app.public-base-url:https://api.fitnest.az}")
    private String publicBaseUrl;

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

        String lightImage = publicBaseUrl + "/api/v1/public/app-qr/image/light";
        String darkImage = publicBaseUrl + "/api/v1/public/app-qr/image/dark";

        return new AppQrReportResponse(lightCount, darkCount, lightCount + darkCount, lightImage, darkImage);
    }

    @Override
    public byte[] getQrCodeImageBytes(String mode) {
        String normalizedMode = normalizeMode(mode);
        String redirectUrl = publicBaseUrl + "/api/v1/public/app-qr/scan/" + normalizedMode.toLowerCase(Locale.ROOT);

        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(redirectUrl, BarcodeFormat.QR_CODE, 500, 500);

            // Light mode: dark blue on white; Dark mode: cyan (#00DBDB) on dark navy (#14234B)
            MatrixToImageConfig config;
            if ("DARK".equals(normalizedMode)) {
                config = new MatrixToImageConfig(0xFF00DBDB, 0xFF14234B);
            } else {
                config = new MatrixToImageConfig(0xFF14234B, 0xFFFFFFFF);
            }

            try (ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream()) {
                MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream, config);
                return pngOutputStream.toByteArray();
            }
        } catch (Exception e) {
            log.error("Failed to generate QR code image for mode: {}", normalizedMode, e);
            throw new RuntimeException("Could not generate QR code image", e);
        }
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
