package az.fitnest.catalog.service.impl;

/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.data.domain.Page
 *  org.springframework.data.domain.PageRequest
 *  org.springframework.data.domain.Pageable
 *  org.springframework.data.domain.Sort
 *  org.springframework.data.domain.Sort$Direction
 *  org.springframework.stereotype.Service
 *  org.springframework.transaction.annotation.Transactional
 *  org.springframework.web.multipart.MultipartFile
 */
import az.fitnest.catalog.dto.ReservationRulesResponse;
import az.fitnest.catalog.exception.BadRequestException;

import az.fitnest.catalog.dto.GymDetailResponse;
import az.fitnest.catalog.dto.GymImageDto;
import az.fitnest.catalog.dto.GymImageItemDto;
import az.fitnest.catalog.dto.GymImageResponse;
import az.fitnest.catalog.dto.GymMainPageDto;
import az.fitnest.catalog.dto.GymNearbyResponseDto;
import az.fitnest.catalog.dto.GymPackageIncludesResponse;
import az.fitnest.catalog.dto.GymPackagesResponse;
import az.fitnest.catalog.dto.GymPlanItemDto;
import az.fitnest.catalog.dto.GymRequest;
import az.fitnest.catalog.dto.GymReviewAuthorDto;
import az.fitnest.catalog.dto.GymReviewDto;
import az.fitnest.catalog.dto.GymReviewsResponse;
import az.fitnest.catalog.dto.GymRoomDto;
import az.fitnest.catalog.dto.GymTrainerDto;
import az.fitnest.catalog.dto.GymTrainersResponse;
import az.fitnest.catalog.dto.GymWorkHourDto;
import az.fitnest.catalog.dto.ReviewRequest;
import az.fitnest.catalog.dto.TrainerRequest;
import az.fitnest.catalog.exception.BadRequestException;
import az.fitnest.catalog.exception.ResourceNotFoundException;
import az.fitnest.catalog.model.entity.Address;
import az.fitnest.catalog.model.entity.Category;
import az.fitnest.catalog.model.entity.Gym;
import az.fitnest.catalog.model.entity.GymImage;
import az.fitnest.catalog.model.entity.Review;
import az.fitnest.catalog.model.entity.Trainer;
import az.fitnest.catalog.model.enums.GymStatus;
import az.fitnest.catalog.repository.CategoryRepository;
import az.fitnest.catalog.repository.GymImageRepository;
import az.fitnest.catalog.repository.GymRepository;
import az.fitnest.catalog.repository.ReviewRepository;
import az.fitnest.catalog.repository.TrainerRepository;
import az.fitnest.catalog.service.FileStorageService;
import az.fitnest.catalog.service.GymService;
import az.fitnest.catalog.service.ReverseGeocodingService;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import az.fitnest.catalog.util.ByteArrayMultipartFile;
import java.io.ByteArrayOutputStream;

@Service
public class GymServiceImpl
implements GymService {
    private final GymRepository gymRepository;
    private final ReviewRepository reviewRepository;
    private final TrainerRepository trainerRepository;
    private final CategoryRepository categoryRepository;
    private final GymImageRepository gymImageRepository;
    private final FileStorageService fileStorageService;
    private final ReverseGeocodingService reverseGeocodingService;
    private final az.fitnest.catalog.client.OrderServiceGrpcClient orderServiceGrpcClient;

        @Override
        @Transactional(readOnly=true)
        public GymDetailResponse getGymDetail(Long userId, Long gymId) {
            Gym gym = (Gym)this.gymRepository.findById(gymId).orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "Gym not found"));
            Map<String, List<GymImage>> grouped = this.gymImageRepository.findByGymId(gymId).stream().filter(img -> img.getImageName() != null).collect(Collectors.groupingBy(GymImage::getImageName));
            List<GymRoomDto> rooms = grouped.entrySet().stream().map(entry -> {
                List<GymImageDto> images = entry.getValue().stream().map(this::toGymImageDto).collect(Collectors.toList());
                return GymRoomDto.builder().room_name(entry.getKey()).images(images).build();
            }).collect(Collectors.toList());
            List<GymPlanItemDto> membershipPlans = orderServiceGrpcClient.getGymPlans(gymId);
            List<GymTrainerDto> trainerDtos = this.trainerRepository.findByGymId(gymId, PageRequest.of(0, 5, Sort.by("id"))).getContent().stream().map(this::toGymTrainerDto).collect(Collectors.toList());
            List<GymReviewDto> recentReviews = this.reviewRepository.findByGymId(gymId, PageRequest.of(0, 3, Sort.by(Sort.Direction.DESC, "createdDate"))).getContent().stream().map(this::toGymReviewDto).collect(Collectors.toList());
            return GymDetailResponse.builder().gym_id(gym.getId().toString()).name(gym.getName()).description(gym.getDescription()).status(gym.getStatus() != null ? gym.getStatus().name() : null).address(gym.getAddress() != null ? gym.getAddress().getAddressText() : null).phone(gym.getPhone()).email(gym.getEmail()).work_hours(this.gymRepository.findWorkHoursByGymId(gymId).stream().map(wh -> GymWorkHourDto.builder().day(wh.getDay()).from(wh.getFromTime()).to(wh.getToTime()).build()).collect(Collectors.toList())).rooms(rooms).membership_plans(membershipPlans).trainers(trainerDtos).recent_reviews(recentReviews).qr_code_url(gym.getQrCodeUrl()).build();
        }

    @Override
    @Transactional(readOnly=true)
    public GymImageResponse getGymImages(Long gymId) {
        if (!this.gymRepository.existsById(gymId)) {
            throw new ResourceNotFoundException("GYM_NOT_FOUND", "Gym not found");
        }
        List<GymImageItemDto> items = this.gymImageRepository.findByGymId(gymId).stream().map(this::toGymImageItemDto).collect(Collectors.toList());
        return GymImageResponse.builder().items(items).build();
    }

    @Override
    @Transactional(readOnly=true)
    public GymPackagesResponse getGymPackages(Long gymId) {
        if (!this.gymRepository.existsById(gymId)) {
            throw new ResourceNotFoundException("GYM_NOT_FOUND", "Gym not found");
        }
        List<GymPlanItemDto> items = orderServiceGrpcClient.getGymPlans(gymId);
        return GymPackagesResponse.builder().items(items).build();
    }

    @Override
    @Transactional(readOnly=true)
    public GymPackageIncludesResponse getPackageIncludes(Long gymId, Long packageId) {
        if (!this.gymRepository.existsById(gymId)) {
            throw new ResourceNotFoundException("GYM_NOT_FOUND", "Gym not found");
        }
        return GymPackageIncludesResponse.builder().plan_id(String.valueOf(packageId)).items(List.of()).build();
    }

    @Override
    @Transactional(readOnly=true)
    public GymTrainersResponse getTrainers(Long gymId, int page, int pageSize) {
        if (!this.gymRepository.existsById(gymId)) {
            throw new ResourceNotFoundException("GYM_NOT_FOUND", "Gym not found");
        }
        Page<Trainer> trainerPage = this.trainerRepository.findByGymId(gymId, pageable(page, pageSize, Sort.unsorted()));
        List<GymTrainerDto> items = trainerPage.getContent().stream().map(this::toGymTrainerDto).collect(Collectors.toList());
        return GymTrainersResponse.builder().items(items).total(trainerPage.getTotalElements()).page(page).pageSize(pageSize).build();
    }

    @Override
    @Transactional(readOnly=true)
    public GymReviewsResponse getReviews(Long gymId, int page, int pageSize, String sort) {
        if (!this.gymRepository.existsById(gymId)) {
            throw new ResourceNotFoundException("GYM_NOT_FOUND", "Gym not found");
        }
        Page<Review> reviewPage = this.reviewRepository.findByGymId(gymId, pageable(page, pageSize, sortForReviews(sort)));
        List<GymReviewDto> items = reviewPage.getContent().stream().map(this::toGymReviewDto).collect(Collectors.toList());
        return GymReviewsResponse.builder().items(items).total(reviewPage.getTotalElements()).page(page).pageSize(pageSize).build();
    }

    @Override
    @Transactional
    public void addReview(Long userId, Long gymId, ReviewRequest request) {
        Gym gym = (Gym)this.gymRepository.findById(gymId).orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "Gym not found"));
        Review review = new Review();
        review.setUserId(userId);
        review.setGymId(gymId);
        review.setRating(request.getRating());
        review.setComment(request.getComment());
        this.reviewRepository.save(review);
        
        Map<String, Object> stats = this.reviewRepository.getRatingAndCountByGymId(gymId);
        Double avgRating = stats.get("avgRating") != null ? (Double) stats.get("avgRating") : 0.0;
        Long totalCount = stats.get("totalCount") != null ? (Long) stats.get("totalCount") : 0L;
        
        gym.setRating(avgRating);
        gym.setReviewsCount(totalCount.intValue());
        this.gymRepository.save(gym);
    }

    @Override
    @Transactional
    public void addTrainer(Long gymId, TrainerRequest request) {
        if (!this.gymRepository.existsById(gymId)) {
            throw new ResourceNotFoundException("GYM_NOT_FOUND", "Gym not found");
        }
        Trainer trainer = new Trainer();
        trainer.setGymId(gymId);
        this.updateTrainerFromRequest(trainer, request);
        this.trainerRepository.save(trainer);
    }

    @Override
    @Transactional
    public void updateTrainer(Long gymId, Long trainerId, TrainerRequest request) {
        Trainer trainer = this.trainerRepository.findById(trainerId).orElseThrow(() -> new ResourceNotFoundException("TRAINER_NOT_FOUND", "Trainer not found"));
        if (!gymId.equals(trainer.getGymId())) {
            throw new ResourceNotFoundException("TRAINER_NOT_FOUND", "Trainer not found");
        }
        if (request.getImageUrl() != null && !request.getImageUrl().equals(trainer.getImageUrl())) {
            this.fileStorageService.deleteFile(trainer.getImageUrl());
        }
        this.updateTrainerFromRequest(trainer, request);
        this.trainerRepository.save(trainer);
    }

    @Override
    @Transactional
    public void deleteTrainer(Long gymId, Long trainerId) {
        Trainer trainerToDelete = this.trainerRepository.findById(trainerId).orElseThrow(() -> new ResourceNotFoundException("TRAINER_NOT_FOUND", "Trainer not found"));
        if (!gymId.equals(trainerToDelete.getGymId())) {
            throw new ResourceNotFoundException("TRAINER_NOT_FOUND", "Trainer not found");
        }
        if (trainerToDelete.getImageUrl() != null && !trainerToDelete.getImageUrl().isBlank()) {
            this.fileStorageService.deleteFile(trainerToDelete.getImageUrl());
        }
        this.trainerRepository.delete(trainerToDelete);
    }

    @Override
    @Transactional
    public void createGym(GymRequest request) {
        Gym gym = new Gym();
        gym.setName(request.getName());
        gym.setDescription(request.getDescription());
        if (request.getStatus() != null) {
            try {
                gym.setStatus(GymStatus.valueOf(request.getStatus().toUpperCase(Locale.ROOT)));
            }
            catch (IllegalArgumentException e) {
                throw new BadRequestException("INVALID_GYM_STATUS", "Invalid gym status: " + request.getStatus());
            }
        } else {
            gym.setStatus(GymStatus.ACTIVE);
        }
        gym.setCoverImageUrl(request.getCoverImageUrl());
        if (request.getAddress() != null) {
            Address address = new Address();
            Double lat = request.getAddress().getLatitude();
            Double lng = request.getAddress().getLongitude();
            address.setLatitude(lat);
            address.setLongitude(lng);
            address.setAddressText(this.reverseGeocodingService.reverseGeocode(lat, lng));
            gym.setAddress(address);
        }
        gym.setPhone(request.getPhone());
        gym.setEmail(request.getEmail());
        if (request.getCategoryIds() != null) {
            HashSet<Category> categories = new HashSet<Category>(this.categoryRepository.findAllById(request.getCategoryIds()));
            gym.setCategories(categories);
        }
        Gym saved = this.gymRepository.save(gym);
        generateAndSaveQrCode(saved);
        this.gymRepository.save(saved);
    }

    @Override
    @Transactional
    public void updateGym(Long gymId, GymRequest request) {
        Gym gym = (Gym)this.gymRepository.findById(gymId).orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "Gym not found"));
        if (request.getCoverImageUrl() != null && !request.getCoverImageUrl().equals(gym.getCoverImageUrl())) {
            this.fileStorageService.deleteFile(gym.getCoverImageUrl());
        }
        gym.setName(request.getName());
        gym.setDescription(request.getDescription());
        if (request.getStatus() != null) {
            try {
                gym.setStatus(GymStatus.valueOf(request.getStatus().toUpperCase(Locale.ROOT)));
            }
            catch (IllegalArgumentException e) {
                throw new BadRequestException("INVALID_GYM_STATUS", "Invalid gym status: " + request.getStatus());
            }
        }
        gym.setCoverImageUrl(request.getCoverImageUrl());
        if (request.getAddress() != null) {
            Address address = new Address();
            Double lat = request.getAddress().getLatitude();
            Double lng = request.getAddress().getLongitude();
            address.setLatitude(lat);
            address.setLongitude(lng);
            address.setAddressText(this.reverseGeocodingService.reverseGeocode(lat, lng));
            gym.setAddress(address);
        }
        gym.setPhone(request.getPhone());
        gym.setEmail(request.getEmail());
        if (request.getCategoryIds() != null) {
            HashSet<Category> categories = new HashSet<Category>(this.categoryRepository.findAllById(request.getCategoryIds()));
            gym.setCategories(categories);
        }
        this.gymRepository.save(gym);
    }

    @Override
    @Transactional
    public void updateLogoUrl(Long gymId, String url) {
        Gym gym = (Gym)this.gymRepository.findById(gymId).orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "Gym not found"));
        if (gym.getLogoUrl() != null && !gym.getLogoUrl().equals(url)) {
            this.fileStorageService.deleteFile(gym.getLogoUrl());
        }
        gym.setLogoUrl(url);
        this.gymRepository.save(gym);
    }

    @Override
    @Transactional
    public void updateCoverImageUrl(Long gymId, String url) {
        Gym gym = (Gym)this.gymRepository.findById(gymId).orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "Gym not found"));
        if (gym.getCoverImageUrl() != null && !gym.getCoverImageUrl().equals(url)) {
            this.fileStorageService.deleteFile(gym.getCoverImageUrl());
        }
        gym.setCoverImageUrl(url);
        this.gymRepository.save(gym);
    }

    private void updateTrainerFromRequest(Trainer trainer, TrainerRequest request) {
        trainer.setFullName(request.getFullName());
        trainer.setSpecialization(request.getSpecialization());
        trainer.setImageUrl(request.getImageUrl());
    }

    @Override
    @Transactional(readOnly=true)
    public ReservationRulesResponse getReservationRules(Long gymId) {
        if (!this.gymRepository.existsById(gymId)) {
            throw new ResourceNotFoundException("GYM_NOT_FOUND", "Gym not found");
        }
        return new ReservationRulesResponse(false, Map.of("max_reservations_per_day", 1, "cancel_before_minutes", 60));
    }

    @Override
    @Transactional(readOnly=true)
    public List<GymNearbyResponseDto> getNearbyGyms(double lat, double lng, double radiusKm) {
        double[] bbox = this.boundingBox(lat, lng, radiusKm);
        Double minLat = bbox[0];
        Double maxLat = bbox[1];
        Double minLng = bbox[2];
        Double maxLng = bbox[3];
        List<Gym> candidates = this.gymRepository.findByAddressLatitudeBetweenAndAddressLongitudeBetween(minLat, maxLat, minLng, maxLng);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime newThreshold = now.minusDays(30L);
        return candidates.stream().filter(gym -> gym.getAddress() != null && gym.getAddress().getLatitude() != null && gym.getAddress().getLongitude() != null).map(gym -> {
            double distance = this.calculateDistance(lat, lng, gym.getAddress().getLatitude(), gym.getAddress().getLongitude());
            return GymNearbyResponseDto.builder().gymId(gym.getId()).name(gym.getName()).address(gym.getAddress() != null ? gym.getAddress().getAddressText() : null).rating(gym.getRating()).isNew(gym.getCreatedDate() != null && gym.getCreatedDate().isAfter(newThreshold)).distanceKm(distance).build();
        }).filter(dto -> dto.getDistanceKm() <= radiusKm).sorted(Comparator.comparingDouble(GymNearbyResponseDto::getDistanceKm)).toList();
    }

    @Override
    @Transactional(readOnly=true)
    public List<GymMainPageDto> getClosestGyms(String q, int page, int pageSize, Double userLat, Double userLng) {
        Page<Gym> gymPage;
        Pageable pageable = pageable(page, pageSize, Sort.unsorted());
        if (userLat != null && userLng != null) {
            double initialRadiusKm = 50.0;
            double[] bbox = this.boundingBox(userLat, userLng, initialRadiusKm);
            if (q != null && !q.isBlank()) {
                gymPage = this.gymRepository.findClosestGymsWithQuery(q, bbox[0], bbox[1], bbox[2], bbox[3], userLat, userLng, pageable);
            } else {
                gymPage = this.gymRepository.findClosestGyms(bbox[0], bbox[1], bbox[2], bbox[3], userLat, userLng, pageable);
            }
        } else {
            // Fallback generic sorting if no location provided
            gymPage = this.gymRepository.findAll(pageable);
            if (q != null && !q.isBlank()) {
                // Not ideal, but realistically user location should be pushed for typical closest requests.
                // Could be moved to DB natively in the future.
                List<Gym> filtered = gymPage.getContent().stream()
                        .filter(g -> g.getName() != null && g.getName().toLowerCase().contains(q.toLowerCase())
                                || g.getDescription() != null && g.getDescription().toLowerCase().contains(q.toLowerCase()))
                        .toList();
                gymPage = new org.springframework.data.domain.PageImpl<>(filtered, pageable, filtered.size());
            }
        }

        return gymPage.getContent().stream().map(gym -> {
            double stars = gym.getRating() != null ? gym.getRating() : 0.0;
            boolean isNew = gym.getCreatedDate() != null && gym.getCreatedDate().isAfter(LocalDateTime.now().minusMonths(1L));
            String imageUrl = gym.getCoverImageUrl();
            Address address = gym.getAddress();
            String location = address != null ? address.getAddressText() : null;
            Double distanceKm = null;
            if (userLat != null && userLng != null && address != null && address.getLatitude() != null && address.getLongitude() != null) {
                distanceKm = this.calculateDistance(userLat, userLng, address.getLatitude(), address.getLongitude());
            }
            return GymMainPageDto.builder().gymId(gym.getId().toString()).name(gym.getName()).imageUrl(imageUrl).stars(stars).isNew(isNew).location(location).distanceKm(distanceKm).build();
        }).collect(Collectors.toList());
    }

    private GymTrainerDto toGymTrainerDto(Trainer t) {
        return GymTrainerDto.builder()
                .trainer_id(t.getId() != null ? t.getId().toString() : null)
                .full_name(t.getFullName())
                .specialization(t.getSpecialization())
                .image_url(t.getImageUrl())
                .build();
    }

    private GymReviewDto toGymReviewDto(Review r) {
        return GymReviewDto.builder()
                .review_id(r.getId() != null ? r.getId().toString() : null)
                .rating(r.getRating())
                .comment(r.getComment())
                .created_at(r.getCreatedDate() != null ? r.getCreatedDate() : null)
                .author(GymReviewAuthorDto.builder()
                        .user_id(r.getUserId() != null ? r.getUserId().toString() : null)
                        .full_name("User " + r.getUserId())
                        .avatar_url(null)
                        .build())
                .build();
    }

    private GymImageDto toGymImageDto(GymImage img) {
        return GymImageDto.builder()
                .id(img.getId())
                .gymId(img.getGym() != null ? img.getGym().getId() : null)
                .name(img.getImageName())
                .url(img.getUrl())
                .build();
    }

    private GymImageItemDto toGymImageItemDto(GymImage img) {
        return GymImageItemDto.builder()
                .image_id(img.getId() != null ? img.getId().toString() : "img_" + System.identityHashCode(img))
                .url(img.getUrl())
                .type(img.getType() != null ? img.getType() : "other")
                .title(img.getTitle())
                .build();
    }

    private Pageable pageable(int page, int size, Sort sort) {
        int safePage = Math.max(page, 1) - 1;
        int safeSize = Math.max(1, Math.min(size, 100));
        return PageRequest.of(safePage, safeSize, sort);
    }

    private Sort sortForReviews(String sort) {
        if ("newest".equalsIgnoreCase(sort)) {
            return Sort.by(Sort.Direction.DESC, "createdDate");
        } else if ("highest".equalsIgnoreCase(sort)) {
            return Sort.by(Sort.Direction.DESC, "rating");
        } else if ("lowest".equalsIgnoreCase(sort)) {
            return Sort.by(Sort.Direction.ASC, "rating");
        }
        return Sort.unsorted();
    }

    private double[] boundingBox(double lat, double lng, double radiusKm) {
        double R = 6371.0;
        double latRadians = Math.toRadians(lat);
        double radiusRatio = radiusKm / 6371.0;
        double minLat = lat - Math.toDegrees(radiusRatio);
        double maxLat = lat + Math.toDegrees(radiusRatio);
        double deltaLng = Math.toDegrees(Math.asin(Math.sin(radiusRatio) / Math.cos(latRadians)));
        double minLng = lng - deltaLng;
        double maxLng = lng + deltaLng;
        return new double[]{minLat, maxLat, minLng, maxLng};
    }

    private double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        int R = 6371;
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lng2 - lng1);
        double a = Math.sin(latDistance / 2.0) * Math.sin(latDistance / 2.0) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(lonDistance / 2.0) * Math.sin(lonDistance / 2.0);
        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
        double result = 6371.0 * c;
        return (double)Math.round(result * 10.0) / 10.0;
    }

    @Override
    @Transactional
    public GymImageDto putGymImage(Long gymId, String imageName, String url) {
        Gym gym = (Gym)this.gymRepository.findById(gymId).orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "Gym not found"));
        GymImage gymImage = new GymImage();
        gymImage.setGym(gym);
        gymImage.setImageName(imageName);
        gymImage.setUrl(url);
        gymImage = (GymImage)this.gymImageRepository.save(gymImage);
        return GymImageDto.builder().id(gymImage.getId()).gymId(gymId).name(gymImage.getImageName()).url(gymImage.getUrl()).build();
    }

    @Override
    @Transactional
    public GymImageDto uploadGymImage(Long gymId, String imageName, MultipartFile file) {
        validateImageFile(file);
        Gym gym = (Gym)this.gymRepository.findById(gymId).orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "Gym not found"));
        GymImage existingImage = gym.getImages().stream().filter(img -> imageName.equals(img.getImageName())).findFirst().orElse(null);
        if (existingImage != null && existingImage.getUrl() != null && !existingImage.getUrl().isBlank()) {
            try {
                this.fileStorageService.deleteFile(existingImage.getUrl());
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        String fsId = this.fileStorageService.saveFile(file, "/gyms/" + gymId);
        String fullUrl = "/api/v1/media/stream/" + fsId;
        return this.putGymImage(gymId, imageName, fullUrl);
    }

    @Override
    @Transactional
    public List<GymImageDto> uploadRoomImages(Long gymId, String imageName, MultipartFile[] files) {
        if (files == null || files.length == 0) {
            return Collections.emptyList();
        }
        Arrays.stream(files).forEach(this::validateImageFile);
        Gym gym = (Gym)this.gymRepository.findById(gymId).orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "Gym not found"));
        ArrayList<GymImageDto> uploaded = new ArrayList<GymImageDto>();
        for (MultipartFile file : files) {
            String fsId = this.fileStorageService.saveFile(file, "/gyms/" + gymId);
            String fullUrl = "/api/v1/media/stream/" + fsId;
            GymImage gi = new GymImage();
            gi.setGym(gym);
            gi.setImageName(imageName);
            gi.setUrl(fullUrl);
            gi.setType("photo");
            gi.setTitle(sanitizeFilename(file.getOriginalFilename()));
            gi = (GymImage)this.gymImageRepository.save(gi);
            uploaded.add(GymImageDto.builder().id(gi.getId()).gymId(gymId).name(gi.getImageName()).url(gi.getUrl()).build());
        }
        return uploaded;
    }

    @Override
    @Transactional
    public GymImageDto replaceRoomImage(Long gymId, Long imageId, MultipartFile file) {
        validateImageFile(file);
        GymImage existing = (GymImage)this.gymImageRepository.findById(imageId).orElseThrow(() -> new ResourceNotFoundException("GYM_IMAGE_NOT_FOUND", "Gym image not found"));
        if (existing.getGym() == null || !existing.getGym().getId().equals(gymId)) {
            throw new ResourceNotFoundException("GYM_IMAGE_MISMATCH", "Image does not belong to specified gym");
        }
        if (existing.getUrl() != null && !existing.getUrl().isBlank()) {
            try {
                this.fileStorageService.deleteFile(existing.getUrl());
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        String fsId = this.fileStorageService.saveFile(file, "/gyms/" + gymId);
        String fullUrl = "/api/v1/media/stream/" + fsId;
        existing.setUrl(fullUrl);
        existing.setTitle(sanitizeFilename(file.getOriginalFilename()));
        GymImage saved = (GymImage)this.gymImageRepository.save(existing);
        return GymImageDto.builder().id(saved.getId()).gymId(gymId).name(saved.getImageName()).url(saved.getUrl()).build();
    }

    @Override
    @Transactional
    public void deleteRoomImage(Long gymId, Long imageId) {
        GymImage existing = (GymImage)this.gymImageRepository.findById(imageId).orElseThrow(() -> new ResourceNotFoundException("GYM_IMAGE_NOT_FOUND", "Gym image not found"));
        if (existing.getGym() == null || !existing.getGym().getId().equals(gymId)) {
            throw new ResourceNotFoundException("GYM_IMAGE_MISMATCH", "Image does not belong to specified gym");
        }
        if (existing.getUrl() != null && !existing.getUrl().isBlank()) {
            try {
                this.fileStorageService.deleteFile(existing.getUrl());
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        this.gymImageRepository.delete(existing);
    }

    @Override
    @Transactional
    public void deleteGym(Long gymId) {
        Gym gym = (Gym)this.gymRepository.findById(gymId).orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "Gym not found"));
        if (gym.getCoverImageUrl() != null && !gym.getCoverImageUrl().isBlank()) {
            try {
                this.fileStorageService.deleteFile(gym.getCoverImageUrl());
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        if (gym.getImages() != null && !gym.getImages().isEmpty()) {
            try {
                List<String> imageUrls = gym.getImages().stream().map(GymImage::getUrl).filter(url -> url != null && !url.isBlank()).toList();
                this.fileStorageService.deleteFiles(imageUrls);
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        if (gym.getTrainers() != null && !gym.getTrainers().isEmpty()) {
            for (Trainer trainer : gym.getTrainers()) {
                if (trainer.getImageUrl() == null || trainer.getImageUrl().isBlank()) continue;
                try {
                    this.fileStorageService.deleteFile(trainer.getImageUrl());
                }
                catch (Exception exception) {}
            }
        }
        this.gymRepository.delete(gym);
    }

    @Override
    @Transactional(readOnly=true)
    public String getGymQrUrl(Long gymId) {
        Gym gym = (Gym)this.gymRepository.findById(gymId).orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "Gym not found"));
        return gym.getQrCodeUrl();
    }

    @Override
    @Transactional(readOnly=true)
    public az.fitnest.catalog.dto.AddressDto getGymLocation(Long gymId) {
        Gym gym = (Gym)this.gymRepository.findById(gymId).orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "Gym not found"));
        az.fitnest.catalog.model.entity.Address addr = gym.getAddress();
        if (addr == null) {
            return az.fitnest.catalog.dto.AddressDto.builder().addressText(null).latitude(null).longitude(null).build();
        }
        return az.fitnest.catalog.dto.AddressDto.builder()
                .addressText(addr.getAddressText())
                .latitude(addr.getLatitude())
                .longitude(addr.getLongitude())
                .build();
    }

    public GymServiceImpl(GymRepository gymRepository, ReviewRepository reviewRepository, TrainerRepository trainerRepository, CategoryRepository categoryRepository, GymImageRepository gymImageRepository, FileStorageService fileStorageService, ReverseGeocodingService reverseGeocodingService, az.fitnest.catalog.client.OrderServiceGrpcClient orderServiceGrpcClient) {
        this.gymRepository = gymRepository;
        this.reviewRepository = reviewRepository;
        this.trainerRepository = trainerRepository;
        this.categoryRepository = categoryRepository;
        this.gymImageRepository = gymImageRepository;
        this.fileStorageService = fileStorageService;
        this.reverseGeocodingService = reverseGeocodingService;
        this.orderServiceGrpcClient = orderServiceGrpcClient;
    }

    private void validateImageFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BadRequestException("FILE_EMPTY", "File cannot be empty");
        }
        long maxSize = 5 * 1024 * 1024; // 5 MB
        if (file.getSize() > maxSize) {
            throw new BadRequestException("FILE_TOO_LARGE", "File size exceeds 5MB limit");
        }
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.equals("image/jpeg") && !contentType.equals("image/png") && !contentType.equals("image/webp"))) {
            throw new BadRequestException("INVALID_FILE_TYPE", "Only JPG, PNG and WEBP images are allowed");
        }
    }

    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "unnamed";
        }
        return filename.replaceAll("[^a-zA-Z0-9.\\-_]", "_");
    }

    private void generateAndSaveQrCode(Gym gym) {
        try {
            String qrContent = "{\"gymId\": " + gym.getId() + "}";
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(qrContent, BarcodeFormat.QR_CODE, 500, 500);
            
            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            byte[] pngData = pngOutputStream.toByteArray();
            
            ByteArrayMultipartFile multipartFile = new ByteArrayMultipartFile(
                    pngData,
                    "qr_code",
                    "gym_" + gym.getId() + "_qr.png",
                    "image/png"
            );
            
            String fsId = this.fileStorageService.saveFile(multipartFile, "/gyms/" + gym.getId() + "/qr");
            gym.setQrCodeUrl("/api/v1/media/stream/" + fsId);
        } catch (Exception e) {
            // Log issue but don't fail gym creation; fallback to relative URL
            gym.setQrCodeUrl("/api/v1/gyms/" + gym.getId() + "/qr");
        }
    }
}

