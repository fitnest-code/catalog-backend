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
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class GymQrCodeService {

    private final GymRepository gymRepository;
    private final FileStorageService fileStorageService;

    @Async("qrcodeExecutor")
    public void generateAndSaveQrCode(Long gymId) {
        log.info("Starting asynchronous QR code generation for gym ID: {}", gymId);
        gymRepository.findById(gymId).ifPresent(gym -> {
            try {
                String secureToken = UUID.randomUUID().toString();
                // Issue 7: Use secure token instead of just ID to prevent information leakage
                String qrContent = secureToken; 
                
                QRCodeWriter qrCodeWriter = new QRCodeWriter();
                BitMatrix bitMatrix = qrCodeWriter.encode(qrContent, BarcodeFormat.QR_CODE, 500, 500);

                ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
                MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
                byte[] pngData = pngOutputStream.toByteArray();

                // Issue 6: Randomized filename to prevent path traversal and naming conflicts
                String filename = "qr_" + UUID.randomUUID() + ".png";
                
                ByteArrayMultipartFile multipartFile = new ByteArrayMultipartFile(
                        pngData,
                        "qr_code",
                        filename,
                        "image/png"
                );

                String fsId = fileStorageService.saveFile(multipartFile, "/gyms/qrs");
                
                gym.setQrCodeUrl("/api/v1/media/stream/" + fsId);
                gym.setQrCodeValue(qrContent);
                gym.setQrCodeToken(secureToken);
                gymRepository.save(gym);
                
                log.info("Successfully generated and saved QR code for gym ID: {}", gymId);
            } catch (Exception e) {
                log.error("Failed to generate QR code for gym ID: {}", gymId, e);
                // Fallback URL if generation fails
                gym.setQrCodeUrl("/api/v1/gyms/" + gym.getId() + "/qr");
                gymRepository.save(gym);
            }
        });
    }
}
