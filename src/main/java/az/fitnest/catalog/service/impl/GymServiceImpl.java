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
package az.fitnest.catalog.service.impl;

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
import az.fitnest.catalog.dto.MembershipPresetDto;
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
import az.fitnest.catalog.service.MembershipPresetsProvider;
import az.fitnest.catalog.service.ReverseGeocodingService;
import java.time.LocalDateTime;
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

    @Override
    @Transactional(readOnly=true)
    public GymDetailResponse getGymDetail(Long userId, Long gymId) {
        Gym gym = (Gym)this.gymRepository.findById(gymId).orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "Gym not found"));
        Map<String, List<GymImage>> grouped = gym.getImages().stream().filter(img -> img.getImageName() != null).collect(Collectors.groupingBy(GymImage::getImageName));
        List<GymRoomDto> rooms = grouped.entrySet().stream().map(entry -> {
            List<GymImageDto> images = entry.getValue().stream().map(img -> GymImageDto.builder().id(img.getId()).gymId(img.getGym() != null ? img.getGym().getId() : null).name(img.getImageName()).url(img.getUrl()).build()).collect(Collectors.toList());
            return GymRoomDto.builder().images(images).build();
        }).collect(Collectors.toList());
        List<MembershipPresetDto> presets = MembershipPresetsProvider.getPresets();
        List<GymPlanItemDto> membershipPlans = presets.stream().map(preset -> {
            List<String> benefits = preset.getOptions().stream().flatMap(opt -> opt.getServices().stream()).distinct().collect(Collectors.toList());
            return GymPlanItemDto.builder().plan_id(null).name(preset.getName()).benefits(benefits).build();
        }).collect(Collectors.toList());
        List<GymTrainerDto> trainerDtos = gym.getTrainers().stream().limit(5L).map(t -> GymTrainerDto.builder().trainer_id(t.getId() != null ? t.getId().toString() : null).full_name(t.getFullName()).specialization(t.getSpecialization()).image_url(t.getImageUrl()).build()).collect(Collectors.toList());
        List<GymReviewDto> recentReviews = gym.getReviews().stream().sorted((a, b) -> b.getCreatedDate().compareTo(a.getCreatedDate())).limit(3L).map(r -> GymReviewDto.builder().review_id(r.getId() != null ? r.getId().toString() : null).rating(r.getRating()).comment(r.getComment()).created_at(r.getCreatedDate() != null ? r.getCreatedDate().format(DateTimeFormatter.ISO_DATE_TIME) : null).author(GymReviewAuthorDto.builder().user_id(r.getUserId() != null ? r.getUserId().toString() : null).full_name("User " + r.getUserId()).avatar_url(null).build()).build()).collect(Collectors.toList());
        return GymDetailResponse.builder().gym_id(gym.getId().toString()).name(gym.getName()).description(gym.getDescription()).status(gym.getStatus() != null ? gym.getStatus().name() : null).address(gym.getAddress() != null ? gym.getAddress().getAddressText() : null).phone(gym.getPhone()).email(gym.getEmail()).work_hours(gym.getWorkHours().stream().map(wh -> GymWorkHourDto.builder().day(wh.getDay()).from(wh.getFromTime()).to(wh.getToTime()).build()).collect(Collectors.toList())).rooms(rooms).membership_plans(membershipPlans).trainers(trainerDtos).recent_reviews(recentReviews).build();
    }

    @Override
    @Transactional(readOnly=true)
    public GymImageResponse getGymImages(Long gymId) {
        Gym gym = (Gym)this.gymRepository.findById(gymId).orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "Gym not found"));
        List<GymImageItemDto> items = gym.getImages().stream().map(img -> GymImageItemDto.builder().image_id((String)(img.getId() != null ? img.getId().toString() : "img_" + img.hashCode())).url(img.getUrl()).type(img.getType() != null ? img.getType() : "other").title(img.getTitle()).build()).collect(Collectors.toList());
        return GymImageResponse.builder().items(items).build();
    }

    @Override
    @Transactional(readOnly=true)
    public GymPackagesResponse getGymPackages(Long gymId) {
        Gym gym = (Gym)this.gymRepository.findById(gymId).orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "Gym not found"));
        List<MembershipPresetDto> presets = MembershipPresetsProvider.getPresets();
        List<GymPlanItemDto> items = presets.stream().map(preset -> {
            List<String> benefits = preset.getOptions().stream().flatMap(opt -> opt.getServices().stream()).distinct().collect(Collectors.toList());
            return GymPlanItemDto.builder().plan_id(null).name(preset.getName()).benefits(benefits).build();
        }).collect(Collectors.toList());
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
        Page<Trainer> trainerPage = this.trainerRepository.findByGymId(gymId, (Pageable)PageRequest.of((int)(page - 1), (int)pageSize));
        List<GymTrainerDto> items = trainerPage.getContent().stream().map(t -> GymTrainerDto.builder().trainer_id(t.getId().toString()).full_name(t.getFullName()).specialization(t.getSpecialization()).image_url(t.getImageUrl()).build()).collect(Collectors.toList());
        return GymTrainersResponse.builder().items(items).total(trainerPage.getTotalElements()).page(page).pageSize(pageSize).build();
    }

    @Override
    @Transactional(readOnly=true)
    public GymReviewsResponse getReviews(Long gymId, int page, int pageSize, String sort) {
        if (!this.gymRepository.existsById(gymId)) {
            throw new ResourceNotFoundException("GYM_NOT_FOUND", "Gym not found");
        }
        Sort sortObj = Sort.unsorted();
        if ("newest".equalsIgnoreCase(sort)) {
            sortObj = Sort.by((Sort.Direction)Sort.Direction.DESC, (String[])new String[]{"createdDate"});
        } else if ("highest".equalsIgnoreCase(sort)) {
            sortObj = Sort.by((Sort.Direction)Sort.Direction.DESC, (String[])new String[]{"rating"});
        } else if ("lowest".equalsIgnoreCase(sort)) {
            sortObj = Sort.by((Sort.Direction)Sort.Direction.ASC, (String[])new String[]{"rating"});
        }
        Page<Review> reviewPage = this.reviewRepository.findByGymId(gymId, (Pageable)PageRequest.of((int)(page - 1), (int)pageSize, (Sort)sortObj));
        List<GymReviewDto> items = reviewPage.getContent().stream().map(r -> GymReviewDto.builder().review_id(r.getId().toString()).rating(r.getRating()).comment(r.getComment()).created_at(r.getCreatedDate().format(DateTimeFormatter.ISO_DATE_TIME)).author(GymReviewAuthorDto.builder().user_id(r.getUserId().toString()).full_name("User " + r.getUserId()).avatar_url(null).build()).build()).collect(Collectors.toList());
        return GymReviewsResponse.builder().items(items).total(reviewPage.getTotalElements()).page(page).pageSize(pageSize).build();
    }

    @Override
    @Transactional
    public void addReview(Long userId, Long gymId, ReviewRequest request) {
        Gym gym = (Gym)this.gymRepository.findById(gymId).orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "Gym not found"));
        Review review = new Review();
        review.setUserId(userId);
        review.setRating(request.getRating());
        review.setComment(request.getComment());
        gym.getReviews().add(review);
        double newRating = (gym.getRating() * (double)gym.getReviewsCount().intValue() + (double)request.getRating().intValue()) / (double)(gym.getReviewsCount() + 1);
        gym.setRating(newRating);
        gym.setReviewsCount(gym.getReviewsCount() + 1);
        this.gymRepository.save(gym);
    }

    @Override
    @Transactional
    public void addTrainer(Long gymId, TrainerRequest request) {
        Gym gym = (Gym)this.gymRepository.findById(gymId).orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "Gym not found"));
        Trainer trainer = new Trainer();
        this.updateTrainerFromRequest(trainer, request);
        gym.getTrainers().add(trainer);
        this.gymRepository.save(gym);
    }

    @Override
    @Transactional
    public void updateTrainer(Long gymId, Long trainerId, TrainerRequest request) {
        Gym gym = (Gym)this.gymRepository.findById(gymId).orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "Gym not found"));
        Trainer trainer = gym.getTrainers().stream().filter(t -> t.getId().equals(trainerId)).findFirst().orElseThrow(() -> new ResourceNotFoundException("TRAINER_NOT_FOUND", "Trainer not found"));
        if (request.getImageUrl() != null && !request.getImageUrl().equals(trainer.getImageUrl())) {
            this.fileStorageService.deleteFile(trainer.getImageUrl());
        }
        this.updateTrainerFromRequest(trainer, request);
        this.gymRepository.save(gym);
    }

    @Override
    @Transactional
    public void deleteTrainer(Long gymId, Long trainerId) {
        Gym gym = (Gym)this.gymRepository.findById(gymId).orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "Gym not found"));
        Trainer trainerToDelete = gym.getTrainers().stream().filter(t -> t.getId().equals(trainerId)).findFirst().orElseThrow(() -> new ResourceNotFoundException("TRAINER_NOT_FOUND", "Trainer not found"));
        if (trainerToDelete.getImageUrl() != null && !trainerToDelete.getImageUrl().isBlank()) {
            this.fileStorageService.deleteFile(trainerToDelete.getImageUrl());
        }
        gym.getTrainers().remove(trainerToDelete);
        this.gymRepository.save(gym);
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
        this.gymRepository.save(gym);
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
    public Object getReservationRules(Long gymId) {
        if (!this.gymRepository.existsById(gymId)) {
            throw new ResourceNotFoundException("GYM_NOT_FOUND", "Gym not found");
        }
        return Map.of("reservation_required", false, "rules", Map.of("max_reservations_per_day", 1, "cancel_before_minutes", 60));
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
        List<Gym> gyms;
        if (userLat != null && userLng != null) {
            double initialRadiusKm = 50.0;
            double[] bbox = this.boundingBox(userLat, userLng, initialRadiusKm);
            gyms = this.gymRepository.findByAddressLatitudeBetweenAndAddressLongitudeBetween(bbox[0], bbox[1], bbox[2], bbox[3]);
        } else {
            gyms = this.gymRepository.findAll();
        }
        Stream<Gym> stream = gyms.stream();
        if (q != null && !q.isBlank()) {
            String lowerQ = q.toLowerCase();
            stream = stream.filter(g -> g.getName() != null && g.getName().toLowerCase().contains(lowerQ) || g.getDescription() != null && g.getDescription().toLowerCase().contains(lowerQ));
        }
        List<GymMainPageDto> all = stream.map(gym -> {
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
        if (userLat != null && userLng != null) {
            all.sort((a, b) -> {
                Double da = a.getDistanceKm();
                Double db = b.getDistanceKm();
                if (da == null && db == null) {
                    return 0;
                }
                if (da == null) {
                    return 1;
                }
                if (db == null) {
                    return -1;
                }
                return Double.compare(da, db);
            });
        }
        int from = Math.max(0, (page - 1) * pageSize);
        int to = Math.min(all.size(), from + pageSize);
        if (from >= all.size()) {
            return Collections.emptyList();
        }
        return all.subList(from, to);
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
            gi.setTitle(file.getOriginalFilename() != null ? file.getOriginalFilename() : "");
            gi = (GymImage)this.gymImageRepository.save(gi);
            uploaded.add(GymImageDto.builder().id(gi.getId()).gymId(gymId).name(gi.getImageName()).url(gi.getUrl()).build());
        }
        return uploaded;
    }

    @Override
    @Transactional
    public GymImageDto replaceRoomImage(Long gymId, Long imageId, MultipartFile file) {
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
        existing.setTitle(file.getOriginalFilename() != null ? file.getOriginalFilename() : existing.getTitle());
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

    public GymServiceImpl(GymRepository gymRepository, ReviewRepository reviewRepository, TrainerRepository trainerRepository, CategoryRepository categoryRepository, GymImageRepository gymImageRepository, FileStorageService fileStorageService, ReverseGeocodingService reverseGeocodingService) {
        this.gymRepository = gymRepository;
        this.reviewRepository = reviewRepository;
        this.trainerRepository = trainerRepository;
        this.categoryRepository = categoryRepository;
        this.gymImageRepository = gymImageRepository;
        this.fileStorageService = fileStorageService;
        this.reverseGeocodingService = reverseGeocodingService;
    }
}

