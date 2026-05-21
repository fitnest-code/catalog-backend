package az.fitnest.catalog.service;

public interface GymQrCodeService {
    void generateAndSaveQrCode(Long gymId);
    String generateAndSaveQrCodeSync(Long gymId);
}
