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

        gymRepository.findById(gymId).ifPresent(gym -> {
            if (gym.getQrCodeToken() != null && gym.getQrCodeUrl() != null) {
                return;
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

                String fsId = fileStorageService.saveFile(multipartFile, "/gyms/qrs");
                String url = "/api/v1/media/stream/" + fsId;

                synchronized (this) {
                    Gym currentGym = gymRepository.findById(gymId).orElse(gym);
                    currentGym.setQrCodeUrl(url);
                    currentGym.setQrCodeValue(secureToken);
                    currentGym.setQrCodeToken(secureToken);
                    gymRepository.save(currentGym);
                }


            } catch (Exception e) {

                gym.setQrCodeUrl("/api/v1/gyms/" + gym.getId() + "/qr");
                if (gym.getQrCodeToken() == null) {
                    gym.setQrCodeToken("PENDING_" + UUID.randomUUID());
                }
                gymRepository.save(gym);
            }
        });
    }
}
