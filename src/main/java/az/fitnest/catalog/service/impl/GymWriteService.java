package az.fitnest.catalog.service.impl;

import az.fitnest.catalog.dto.GeocodingResponse;

import az.fitnest.catalog.dto.GymRequest;
import az.fitnest.catalog.dto.CheckInResponseDto;
import az.fitnest.catalog.dto.GymSubscriptionsUpdateRequest;
import az.fitnest.catalog.exception.BadRequestException;
import az.fitnest.catalog.exception.ResourceNotFoundException;
import az.fitnest.catalog.model.entity.Address;
import az.fitnest.catalog.model.entity.Category;
import az.fitnest.catalog.model.entity.Gym;
import az.fitnest.catalog.model.entity.GymImage;
import az.fitnest.catalog.model.entity.GymSubscription;
import az.fitnest.catalog.model.entity.GymSubscriptionBenefit;
import az.fitnest.catalog.dto.GymSubscriptionRequestDto;
import az.fitnest.catalog.dto.GymSubscriptionBenefitRequestDto;
import az.fitnest.catalog.model.entity.Trainer;
import az.fitnest.catalog.model.enums.GymStatus;
import az.fitnest.catalog.repository.CategoryRepository;
import az.fitnest.catalog.repository.GymRepository;
import az.fitnest.catalog.service.FileStorageService;
import az.fitnest.catalog.service.ReverseGeocodingService;
import az.fitnest.catalog.client.OrderServiceGrpcClient;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import az.fitnest.catalog.util.ByteArrayMultipartFile;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class GymWriteService {

    private final GymRepository gymRepository;
    private final CategoryRepository categoryRepository;
    private final ReverseGeocodingService reverseGeocodingService;
    private final FileStorageService fileStorageService;
    private final OrderServiceGrpcClient orderServiceGrpcClient;

    @Transactional
    public void createGym(GymRequest request) {
        Gym gym = new Gym();
        gym.setName(request.getName());
        gym.setDescription(request.getDescription());


        if (request.getAddress() != null) {
            Address address = new Address();
            Double lat = request.getAddress().getLatitude();
            Double lng = request.getAddress().getLongitude();
            address.setLatitude(lat);
            address.setLongitude(lng);
            GeocodingResponse geocoding = reverseGeocodingService.reverseGeocode(lat, lng);
            if (geocoding != null) {
                address.setAddressText(geocoding.getAddressText());
                address.setCity(geocoding.getCity());
            }
            gym.setAddress(address);
        }
        gym.setPhone(request.getPhone());
        gym.setEmail(request.getEmail());
        if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
            List<Category> categories = categoryRepository.findAllById(request.getCategoryIds());
            if (categories.size() != request.getCategoryIds().size()) {
                throw new BadRequestException("INVALID_CATEGORIES", "One or more category IDs are invalid");
            }
            gym.setCategories(new HashSet<>(categories));
        }
        gym.setResponsiblePerson(request.getResponsiblePerson());
        gym.setStatus(request.getStatus() != null ? request.getStatus() : GymStatus.ACTIVE);

        Gym saved = gymRepository.save(gym);
        // Call it asynchronously to prevent blocking the HTTP request
        // Using @Async requires @EnableAsync on the main class
        // Alternatively we can use Spring Events
        generateAndSaveQrCode(saved);
    }

    @Transactional
    @CacheEvict(cacheNames = "gyms", key = "#gymId")
    public void updateGym(Long gymId, GymRequest request) {
        Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "Gym not found"));



        gym.setName(request.getName());
        gym.setDescription(request.getDescription());


        if (request.getAddress() != null) {
            Address address = new Address();
            Double lat = request.getAddress().getLatitude();
            Double lng = request.getAddress().getLongitude();
            address.setLatitude(lat);
            address.setLongitude(lng);
            GeocodingResponse geocoding = reverseGeocodingService.reverseGeocode(lat, lng);
            if (geocoding != null) {
                address.setAddressText(geocoding.getAddressText());
                address.setCity(geocoding.getCity());
            }
            gym.setAddress(address);
        }
        gym.setPhone(request.getPhone());
        gym.setEmail(request.getEmail());
        if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
            List<Category> categories = categoryRepository.findAllById(request.getCategoryIds());
            if (categories.size() != request.getCategoryIds().size()) {
                throw new BadRequestException("INVALID_CATEGORIES", "One or more category IDs are invalid");
            }
            gym.setCategories(new HashSet<>(categories));
        }
        gym.setResponsiblePerson(request.getResponsiblePerson());
        gym.setStatus(request.getStatus() != null ? request.getStatus() : GymStatus.ACTIVE);

        gymRepository.save(gym);
    }

    @Transactional
    @CacheEvict(cacheNames = {"gyms", "gymImages", "gymPackages"}, key = "#gymId")
    public void updateGymSubscriptions(Long gymId, GymSubscriptionsUpdateRequest request) {
        Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "Gym not found"));

        gym.getSubscriptions().clear();
        if (request.getSubscriptions() != null) {
            for (GymSubscriptionRequestDto subDto : request.getSubscriptions()) {
                if (!orderServiceGrpcClient.checkPlanExists(subDto.getPlanId())) {
                    throw new BadRequestException("PLAN_NOT_FOUND", "Membership plan ID " + subDto.getPlanId() + " does not exist or is inactive.");
                }
                GymSubscription subscription = new GymSubscription();
                subscription.setPlanId(subDto.getPlanId());
                subscription.setGym(gym);
                if (subDto.getBenefits() != null) {
                    List<GymSubscriptionBenefit> benefits = subDto.getBenefits().stream().map(b -> {
                        GymSubscriptionBenefit benefit = new GymSubscriptionBenefit();
                        benefit.setBenefit(b.getBenefit());
                        benefit.setBenefitLogo(b.getBenefitLogo());
                        return benefit;
                    }).toList();
                    subscription.setBenefits(new java.util.ArrayList<>(benefits));
                }
                gym.getSubscriptions().add(subscription);
            }
        }
        gymRepository.save(gym);
    }

    @Transactional
    @CacheEvict(cacheNames = {"gyms", "gymImages", "gymPackages"}, key = "#gymId")
    public void deleteGym(Long gymId) {
        Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "Gym not found"));

        if (gym.getCoverImageUrl() != null && !gym.getCoverImageUrl().isBlank()) {
            safeDeleteFile(gym.getCoverImageUrl());
        }

        if (gym.getImages() != null && !gym.getImages().isEmpty()) {
            List<String> imageUrls = gym.getImages().stream()
                    .map(GymImage::getUrl)
                    .filter(url -> url != null && !url.isBlank())
                    .toList();
            try {
                fileStorageService.deleteFiles(imageUrls);
            } catch (Exception e) {
                // Background error on deletion - swallowed on purpose
            }
        }

        if (gym.getTrainers() != null && !gym.getTrainers().isEmpty()) {
            for (Trainer trainer : gym.getTrainers()) {
                if (trainer.getPicture() != null && !trainer.getPicture().isBlank()) {
                    safeDeleteFile(trainer.getPicture());
                }
            }
        }
        gymRepository.delete(gym);
    }

    @Transactional
    public CheckInResponseDto checkIn(Long userId, Long gymId) {
        Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "Gym not found"));

        orderServiceGrpcClient.checkIn(userId, gymId);

        String addressText = gym.getAddress() != null ? gym.getAddress().getAddressText() : null;
        LocalDateTime now = LocalDateTime.now();

        return new CheckInResponseDto(addressText, now.toLocalDate(), now.toLocalTime());
    }

    private void safeDeleteFile(String url) {
        try {
            fileStorageService.deleteFile(url);
        } catch (Exception e) {
            // Background error on deletion - swallowed on purpose
        }
    }

    // Ideally moved to an event listener or background queue
    @Async
    public void generateAndSaveQrCode(Gym gym) {
        try {
            String qrContent = "{\"gymId\": " + gym.getId() + "}";
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(qrContent, BarcodeFormat.QR_CODE, 500, 500);

            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            byte[] pngData = pngOutputStream.toByteArray();

            String gymAddress = gym.getAddress() != null && gym.getAddress().getAddressText() != null ? gym.getAddress().getAddressText() : "";
            String baseName = sanitizeFilename(gym.getName() + "_" + gymAddress);
            ByteArrayMultipartFile multipartFile = new ByteArrayMultipartFile(
                    pngData,
                    "qr_code",
                    baseName + "_" + java.util.UUID.randomUUID().toString().substring(0, 8) + "_qr.png",
                    "image/png"
            );

            String fsId = fileStorageService.saveFile(multipartFile, "/gyms");
            // Requires a separate transaction to update since this runs async
            gymRepository.findById(gym.getId()).ifPresent(g -> {
                g.setQrCodeUrl("/api/v1/media/stream/" + fsId);
                gymRepository.save(g);
            });
        } catch (Exception e) {
            gymRepository.findById(gym.getId()).ifPresent(g -> {
                g.setQrCodeUrl("/api/v1/gyms/" + g.getId() + "/qr");
                gymRepository.save(g);
            });
        }
    }

    @Transactional
    @CacheEvict(cacheNames = "gyms", key = "#gymId")
    public void updateLogo(Long gymId, MultipartFile file) {
        Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "Gym not found"));

        String fsId = fileStorageService.saveFile(file, "/gyms/logos", gym.getLogoUrl());
        gym.setLogoUrl("/api/v1/media/stream/" + fsId);
        gymRepository.save(gym);
    }

    @Transactional
    @CacheEvict(cacheNames = "gyms", key = "#gymId")
    public void updateCoverImage(Long gymId, MultipartFile file) {
        Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "Gym not found"));

        String fsId = fileStorageService.saveFile(file, "/gyms/covers", gym.getCoverImageUrl());
        gym.setCoverImageUrl("/api/v1/media/stream/" + fsId);
        gymRepository.save(gym);
    }

    @Transactional
    public void deleteAllGyms() {
        List<Gym> gyms = gymRepository.findAll();
        for (Gym gym : gyms) {
            deleteGym(gym.getId());
        }
    }

    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) return "unnamed";
        return filename.replaceAll("[^a-zA-Z0-9.\\-_]", "_");
    }
}
