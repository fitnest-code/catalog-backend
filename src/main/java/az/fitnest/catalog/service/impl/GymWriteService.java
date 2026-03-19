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
import az.fitnest.catalog.model.entity.SavedGym;
import az.fitnest.catalog.model.entity.GymSubscription;
import az.fitnest.catalog.model.entity.GymSubscriptionBenefit;
import az.fitnest.catalog.dto.GymSubscriptionRequestDto;
import az.fitnest.catalog.dto.GymSubscriptionBenefitRequestDto;
import az.fitnest.catalog.model.entity.Trainer;
import az.fitnest.catalog.model.enums.GymStatus;
import az.fitnest.catalog.repository.CategoryRepository;
import az.fitnest.catalog.repository.GymRepository;
import az.fitnest.catalog.repository.SavedGymRepository;
import az.fitnest.catalog.service.FileStorageService;
import az.fitnest.catalog.service.ReverseGeocodingService;
import az.fitnest.catalog.client.OrderServiceGrpcClient;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import az.fitnest.catalog.util.ByteArrayMultipartFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class GymWriteService {

    private final GymRepository gymRepository;
    private final SavedGymRepository savedGymRepository;
    private final CategoryRepository categoryRepository;
    private final ReverseGeocodingService reverseGeocodingService;
    private final FileStorageService fileStorageService;
    private final OrderServiceGrpcClient orderServiceGrpcClient;

    @org.springframework.beans.factory.annotation.Autowired
    @org.springframework.context.annotation.Lazy
    private GymWriteService self;

    @Transactional
    public void createGym(GymRequest request) {
        if (request.categoryIds() == null || request.categoryIds().isEmpty()) {
            throw new BadRequestException("CATEGORY_REQUIRED", "error.category_required");
        }
        List<Category> categories = categoryRepository.findAllById(request.categoryIds());
        if (categories.size() != request.categoryIds().size()) {
            throw new BadRequestException("INVALID_CATEGORIES", "error.invalid_categories");
        }
        Gym gym = new Gym();
        gym.setName(request.name());
        gym.setDescription(request.description());

        if (request.address() != null) {
            Address address = new Address();
            Double lat = request.address().latitude();
            Double lng = request.address().longitude();
            address.setLatitude(lat);
            address.setLongitude(lng);
            GeocodingResponse geocoding = reverseGeocodingService.reverseGeocode(lat, lng);
            if (geocoding != null) {
                address.setAddressText(geocoding.addressText());
                address.setCity(geocoding.city());
            }
            gym.setAddress(address);
        }
        gym.setPhone(request.phone());
        gym.setEmail(request.email());
        gym.setCategories(new HashSet<>(categories));

        if (request.generalWorkHours() != null) {
            Set<az.fitnest.catalog.model.entity.GymWorkHour> generalWorkHours = request.generalWorkHours().stream()
                    .map(dto -> {
                        if (dto.period() == null) throw new BadRequestException("INVALID_PERIOD", "error.invalid_period");
                        if (!isValidPeriod(dto.period())) throw new BadRequestException("INVALID_PERIOD", "error.invalid_period");
                        return new az.fitnest.catalog.model.entity.GymWorkHour(dto.period(), dto.from(), dto.to());
                    })
                    .collect(java.util.stream.Collectors.toSet());
            gym.setGeneralWorkHours(generalWorkHours);
        }

        if (request.workHoursWoman() != null) {
            Set<az.fitnest.catalog.model.entity.GymWorkHour> workHoursWoman = request.workHoursWoman().stream()
                    .map(dto -> {
                        if (dto.period() == null) throw new BadRequestException("INVALID_PERIOD", "error.invalid_period");
                        if (!isValidPeriod(dto.period())) throw new BadRequestException("INVALID_PERIOD", "error.invalid_period");
                        return new az.fitnest.catalog.model.entity.GymWorkHour(dto.period(), dto.from(), dto.to());
                    })
                    .collect(java.util.stream.Collectors.toSet());
            gym.setWorkHoursWoman(workHoursWoman);
        }

        if (request.workHoursMan() != null) {
            Set<az.fitnest.catalog.model.entity.GymWorkHour> workHoursMan = request.workHoursMan().stream()
                    .map(dto -> {
                        if (dto.period() == null) throw new BadRequestException("INVALID_PERIOD", "error.invalid_period");
                        if (!isValidPeriod(dto.period())) throw new BadRequestException("INVALID_PERIOD", "error.invalid_period");
                        return new az.fitnest.catalog.model.entity.GymWorkHour(dto.period(), dto.from(), dto.to());
                    })
                    .collect(java.util.stream.Collectors.toSet());
            gym.setWorkHoursMan(workHoursMan);
        }

        gym.setStatus(request.status() != null ? request.status() : GymStatus.ACTIVE);

        Gym saved = gymRepository.save(gym);

        self.generateAndSaveQrCode(saved);
    }

    @Transactional
    @CacheEvict(cacheNames = "gyms", key = "#gymId")
    public void updateGym(Long gymId, GymRequest request) {
        if (request.categoryIds() == null || request.categoryIds().isEmpty()) {
            throw new BadRequestException("CATEGORY_REQUIRED", "error.category_required");
        }
        List<Category> categories = categoryRepository.findAllById(request.categoryIds());
        if (categories.size() != request.categoryIds().size()) {
            throw new BadRequestException("INVALID_CATEGORIES", "error.invalid_categories");
        }
        Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));

        gym.setName(request.name());
        gym.setDescription(request.description());

        if (request.address() != null) {
            Address address = new Address();
            Double lat = request.address().latitude();
            Double lng = request.address().longitude();
            address.setLatitude(lat);
            address.setLongitude(lng);
            GeocodingResponse geocoding = reverseGeocodingService.reverseGeocode(lat, lng);
            if (geocoding != null) {
                address.setAddressText(geocoding.addressText());
                address.setCity(geocoding.city());
            }
            gym.setAddress(address);
        }
        gym.setPhone(request.phone());
        gym.setEmail(request.email());
        gym.setCategories(new HashSet<>(categories));
        gym.getGeneralWorkHours().clear();
        if (request.generalWorkHours() != null) {
            Set<az.fitnest.catalog.model.entity.GymWorkHour> newGeneralWorkHours = request.generalWorkHours().stream()
                    .map(dto -> {
                        if (dto.period() == null) throw new BadRequestException("INVALID_PERIOD", "error.invalid_period");
                        if (!isValidPeriod(dto.period())) throw new BadRequestException("INVALID_PERIOD", "error.invalid_period");
                        return new az.fitnest.catalog.model.entity.GymWorkHour(dto.period(), dto.from(), dto.to());
                    })
                    .collect(java.util.stream.Collectors.toSet());
            gym.getGeneralWorkHours().addAll(newGeneralWorkHours);
        }

        gym.getWorkHoursWoman().clear();
        if (request.workHoursWoman() != null) {
            Set<az.fitnest.catalog.model.entity.GymWorkHour> newWorkHoursWoman = request.workHoursWoman().stream()
                    .map(dto -> {
                        if (dto.period() == null) throw new BadRequestException("INVALID_PERIOD", "error.invalid_period");
                        if (!isValidPeriod(dto.period())) throw new BadRequestException("INVALID_PERIOD", "error.invalid_period");
                        return new az.fitnest.catalog.model.entity.GymWorkHour(dto.period(), dto.from(), dto.to());
                    })
                    .collect(java.util.stream.Collectors.toSet());
            gym.getWorkHoursWoman().addAll(newWorkHoursWoman);
        }

        gym.getWorkHoursMan().clear();
        if (request.workHoursMan() != null) {
            Set<az.fitnest.catalog.model.entity.GymWorkHour> newWorkHoursMan = request.workHoursMan().stream()
                    .map(dto -> {
                        if (dto.period() == null) throw new BadRequestException("INVALID_PERIOD", "error.invalid_period");
                        if (!isValidPeriod(dto.period())) throw new BadRequestException("INVALID_PERIOD", "error.invalid_period");
                        return new az.fitnest.catalog.model.entity.GymWorkHour(dto.period(), dto.from(), dto.to());
                    })
                    .collect(java.util.stream.Collectors.toSet());
            gym.getWorkHoursMan().addAll(newWorkHoursMan);
        }

        gym.setStatus(request.status() != null ? request.status() : GymStatus.ACTIVE);

        gymRepository.save(gym);
    }

    @Transactional
    @CacheEvict(cacheNames = {"gyms", "gymImages", "gymPackages"}, key = "#gymId")
    public void enableGymSubscription(Long gymId, Long subscriptionId) {
        Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));
        if (!orderServiceGrpcClient.checkPackageExists(subscriptionId)) {
            throw new BadRequestException("PACKAGE_NOT_FOUND", "error.package_not_found");
        }
        gym.getSubscriptions().removeIf(s -> s.getPackageId() != null && s.getPackageId().equals(subscriptionId));
        GymSubscription subscription = new GymSubscription();
        subscription.setGym(gym);
        subscription.setPackageId(subscriptionId);
        subscription.setBenefits(new java.util.HashSet<>());
        gym.getSubscriptions().add(subscription);
        gymRepository.save(gym);
    }

    @Transactional
    @CacheEvict(cacheNames = {"gyms", "gymImages", "gymPackages"}, key = "#gymId")
    public void updateGymSubscriptionBenefits(Long gymId, Long packageId, az.fitnest.catalog.dto.GymSubscriptionBenefitsUpdateRequest request) {
        Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));
        GymSubscription subscription = gym.getSubscriptions().stream().findFirst()
                .orElseThrow(() -> new BadRequestException("SUBSCRIPTION_NOT_ENABLED", "error.subscription_not_enabled"));
        subscription.getBenefits().clear();
        if (request.benefits() != null) {
            List<GymSubscriptionBenefit> newBenefits = request.benefits().stream().map(b -> {
                GymSubscriptionBenefit benefit = new GymSubscriptionBenefit();
                benefit.setBenefit(b.benefit());
                benefit.setBenefitLogo(b.benefitLogo());
                return benefit;
            }).toList();
            subscription.getBenefits().addAll(newBenefits);
        }

        gymRepository.save(gym);
    }

    @Transactional
    @CacheEvict(cacheNames = {"gyms", "gymImages", "gymPackages"}, key = "#gymId")
    public void deleteGym(Long gymId) {
        Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));

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
            }
        }

        if (gym.getRooms() != null) {
            List<String> roomImageUrls = gym.getRooms().stream()
                    .flatMap(r -> r.getImages().stream())
                    .map(az.fitnest.catalog.model.entity.RoomImage::getPictureUrl)
                    .filter(url -> url != null && !url.isBlank())
                    .toList();
            if (!roomImageUrls.isEmpty()) {
                try {
                    fileStorageService.deleteFiles(roomImageUrls);
                } catch (Exception e) {}
            }
        }

        if (gym.getTrainers() != null && !gym.getTrainers().isEmpty()) {
            for (Trainer trainer : gym.getTrainers()) {
                if (trainer.getPicture() != null && !trainer.getPicture().isBlank()) {
                    safeDeleteFile(trainer.getPicture());
                }
            }
        }
        savedGymRepository.deleteByGymId(gymId);
        gymRepository.delete(gym);
    }

    @Transactional
    public boolean toggleSave(Long userId, Long gymId) {
        java.util.Optional<az.fitnest.catalog.model.entity.SavedGym> existing = savedGymRepository.findByUserIdAndGymId(userId, gymId);
        if (existing.isPresent()) {
            savedGymRepository.delete(existing.get());
            return false;
        } else {
            Gym gym = gymRepository.findById(gymId).orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));
            az.fitnest.catalog.model.entity.SavedGym saved = new az.fitnest.catalog.model.entity.SavedGym();
            saved.setUserId(userId);
            saved.setGym(gym);
            savedGymRepository.save(saved);
            return true;
        }
    }

    @Transactional
    public CheckInResponseDto checkIn(Long userId, Long gymId) {
        Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));

        orderServiceGrpcClient.checkIn(userId, gymId);

        String addressText = gym.getAddress() != null ? gym.getAddress().getAddressText() : null;
        LocalDateTime now = LocalDateTime.now();

        return new CheckInResponseDto(addressText, now.toLocalDate(), now.toLocalTime());
    }

    private void safeDeleteFile(String url) {
        try {
            fileStorageService.deleteFile(url);
        } catch (Exception e) {
        }
    }

    @Async
    public void generateAndSaveQrCode(Gym gym) {
        try {
            String secureToken = java.util.UUID.randomUUID().toString();
            String qrContent = "https://fitnest.app/gym/" + gym.getId() + "?token=" + secureToken;
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
            gymRepository.findById(gym.getId()).ifPresent(g -> {
                g.setQrCodeUrl("/api/v1/media/stream/" + fsId);
                g.setQrCodeValue(qrContent);
                g.setQrCodeToken(secureToken);
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
    public void addRoomImages(Long gymId, List<String> roomNames, List<MultipartFile> files) {
        Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));

        if (roomNames.size() != files.size()) {
            throw new BadRequestException("INVALID_INPUT", "error.invalid_input");
        }

        for (int i = 0; i < files.size(); i++) {
            String roomName = roomNames.get(i);
            MultipartFile file = files.get(i);

            String fsId = fileStorageService.saveFile(file, "/gyms/rooms");
            String url = "/api/v1/media/stream/" + fsId;

            az.fitnest.catalog.model.entity.Room room = gym.getRooms().stream()
                    .filter(r -> r.getName().equals(roomName))
                    .findFirst()
                    .orElseGet(() -> {
                        az.fitnest.catalog.model.entity.Room newRoom = az.fitnest.catalog.model.entity.Room.builder()
                                .name(roomName)
                                .gym(gym)
                                .build();
                        gym.getRooms().add(newRoom);
                        return newRoom;
                    });

            az.fitnest.catalog.model.entity.RoomImage roomImage = az.fitnest.catalog.model.entity.RoomImage.builder()
                    .room(room)
                    .pictureUrl(url)
                    .build();

            room.getImages().add(roomImage);
        }

        gymRepository.save(gym);
    }

    @Transactional
    @CacheEvict(cacheNames = "gyms", key = "#gymId")
    public void updateCoverImage(Long gymId, MultipartFile file) {
        Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));

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

    private boolean isValidPeriod(az.fitnest.catalog.model.enums.GymWorkHourPeriod period) {
        return period == az.fitnest.catalog.model.enums.GymWorkHourPeriod.WEEKDAYS || period == az.fitnest.catalog.model.enums.GymWorkHourPeriod.SATURDAY || period == az.fitnest.catalog.model.enums.GymWorkHourPeriod.SUNDAY;
    }
}
