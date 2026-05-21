package az.fitnest.catalog.service.impl;

import az.fitnest.catalog.model.entity.Gym;
import az.fitnest.catalog.repository.GymRepository;
import az.fitnest.catalog.service.FileStorageService;
import az.fitnest.catalog.util.ByteArrayMultipartFile;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GymQrCodeServiceImpl implements az.fitnest.catalog.service.GymQrCodeService {

    private final GymRepository gymRepository;
    private final FileStorageService fileStorageService;

    @Async("qrcodeExecutor")
    @org.springframework.transaction.annotation.Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void generateAndSaveQrCode(Long gymId) {
        generateAndSaveQrCodeInternal(gymId, false);
    }

    @org.springframework.transaction.annotation.Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public String generateAndSaveQrCodeSync(Long gymId) {
        return generateAndSaveQrCodeInternal(gymId, true);
    }

    private String generateAndSaveQrCodeInternal(Long gymId, boolean forceRegenerate) {
        Gym gym = gymRepository.findById(gymId).orElseThrow(() -> new RuntimeException("Gym not found: " + gymId));
        if (!forceRegenerate && gym.getQrCodeToken() != null && gym.getQrCodeUrl() != null 
                && !gym.getQrCodeUrl().contains("/qr") && !gym.getQrCodeUrl().contains("PENDING")) {
            return gym.getQrCodeUrl();
        }

        try {
            String secureToken = UUID.randomUUID().toString();

            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(secureToken, BarcodeFormat.QR_CODE, 500, 500);

            byte[] pngData;
            try (ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream()) {
                MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
                pngData = pngOutputStream.toByteArray();
            }

            String filename = "qr_" + UUID.randomUUID() + ".png";
            ByteArrayMultipartFile multipartFile = new ByteArrayMultipartFile(
                    pngData,
                    "qr_code",
                    filename,
                    "image/png"
            );

            String url = fileStorageService.saveFile(multipartFile, "/gyms/qrs");

            gym.setQrCodeUrl(url);
            gym.setQrCodeValue(secureToken);
            gym.setQrCodeToken(secureToken);
            gymRepository.save(gym);
            return url;

        } catch (Exception e) {
            String fallbackUrl = "/api/v1/gyms/" + gym.getId() + "/qr";
            gym.setQrCodeUrl(fallbackUrl);
            if (gym.getQrCodeToken() == null) {
                gym.setQrCodeToken("PENDING_" + UUID.randomUUID());
            }
            gymRepository.save(gym);
            return fallbackUrl;
        }
    }
}
