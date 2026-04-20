package az.fitnest.catalog.service.impl;

import az.fitnest.catalog.dto.admin.*;
import az.fitnest.catalog.exception.BadRequestException;
import az.fitnest.catalog.exception.ConflictException;
import az.fitnest.catalog.exception.ResourceNotFoundException;
import az.fitnest.catalog.mapper.AdminPanelGymMapper;
import az.fitnest.catalog.model.entity.AdminPanelGymImage;
import az.fitnest.catalog.model.entity.AdminPanelWorkingHour;
import az.fitnest.catalog.model.entity.GymAdminPanel;
import az.fitnest.catalog.model.enums.AdminPanelGymStatus;
import az.fitnest.catalog.repository.AdminPanelWorkingHourRepository;
import az.fitnest.catalog.repository.GymAdminPanelRepository;
import az.fitnest.catalog.service.AdminPanelReverseGeocodingService;
import az.fitnest.catalog.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminPanelGymWriteService {

    private static final List<String> ALLOWED_TYPES = List.of("image/jpeg", "image/jpg", "image/png");
    private static final long MAX_SIZE = 2 * 1024 * 1024;

    private final AdminPanelReverseGeocodingService reverseGeocodingService;
    private final AdminPanelWorkingHourRepository workingHourRepository;
    private final GymAdminPanelRepository gymAdminPanelRepository;
    private final FileStorageService fileStorageService;
    private final AdminPanelGymMapper adminPanelGymMapper;
    private final LocationService locationService;

    @Transactional
    public AdminPanelGymResponse createGymForAdmin(AdminPanelCreateGymRequest request) {
        GymAdminPanel saved = gymAdminPanelRepository.save(adminPanelGymMapper.toEntity(request));
        return adminPanelGymMapper.toCreateResponse(saved);
    }

    @Transactional
    public void deleteGym(Long gymId) {
        GymAdminPanel gym = gymAdminPanelRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));

        if (gym.getSubscriptions() != null && !gym.getSubscriptions().isEmpty()) {
            throw new ConflictException("GYM_HAS_ACTIVE_SUBSCRIPTIONS", "error.gym_has_active_subscriptions");
        }

        gym.setStatus(AdminPanelGymStatus.DELETED);
        gymAdminPanelRepository.save(gym);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = {"gym-detail", "main-page-gyms"}, allEntries = true)
    })
    public void updateGeneralInfo(Long gymId, GeneralInfoRequest request) {
        if ((request.latitude() == null) != (request.longitude() == null)) {
            throw new BadRequestException("INVALID_LOCATION", "error.lat_lng_must_be_together");
        }

        GymAdminPanel gym = gymAdminPanelRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));

        AdminPanelGeocodingResponse geocoding = reverseGeocodingService
                .reverseGeocode(request.latitude(), request.longitude());

        adminPanelGymMapper.updateGeneralInfo(gym, request, geocoding);

        if (request.latitude() != null) {
            locationService.resolveAndSetLocation(gym.getAddress(), geocoding);
        }

        gymAdminPanelRepository.save(gym);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = {"gym-detail", "main-page-gyms"}, allEntries = true),
            @CacheEvict(cacheNames = "gym-images", key = "#gymId")
    })
    public CoverImageResponse uploadCoverImage(Long gymId, MultipartFile file) {
        validateImageFile(file);

        GymAdminPanel gym = gymAdminPanelRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));

        String fsId = fileStorageService.saveFile(file, "/gyms/covers", gym.getCoverImageUrl());
        String url = "/api/v1/media/stream/" + fsId;

        gym.setCoverImageUrl(url);
        gymAdminPanelRepository.save(gym);

        return new CoverImageResponse(url);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = {"gym-detail", "main-page-gyms"}, allEntries = true),
            @CacheEvict(cacheNames = "gym-images", key = "#gymId")
    })
    public void deleteCoverImage(Long gymId) {
        GymAdminPanel gym = gymAdminPanelRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));

        if (!StringUtils.hasText(gym.getCoverImageUrl())) {
            throw new ResourceNotFoundException("COVER_IMAGE_NOT_FOUND", "error.cover_image_not_found");
        }

        fileStorageService.deleteFile(gym.getCoverImageUrl());
        gym.setCoverImageUrl(null);
        gymAdminPanelRepository.save(gym);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = {"gym-detail", "main-page-gyms"}, allEntries = true),
            @CacheEvict(cacheNames = "gym-images", key = "#gymId")
    })
    public void addGalleryImage(Long gymId, MultipartFile file, Integer sortOrder) {
        validateImageFile(file);

        GymAdminPanel gym = gymAdminPanelRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));

        String fsId = fileStorageService.saveFile(file, "/gyms/gallery");
        String url = "/api/v1/media/stream/" + fsId;

        int order = sortOrder != null ? sortOrder :
                gym.getImages().stream()
                .mapToInt(i -> i.getSortOrder() != null ? i.getSortOrder() : 0)
                .max()
                .orElse(0) + 1;

        AdminPanelGymImage image = AdminPanelGymImage.builder()
                .url(url)
                .sortOrder(order)
                .build();

        gym.getImages().add(image);
        gymAdminPanelRepository.save(gym);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = {"gym-detail", "main-page-gyms"}, allEntries = true),
            @CacheEvict(cacheNames = "gym-images", key = "#gymId")
    })
    public void deleteGalleryImage(Long gymId, Long imageId) {
        GymAdminPanel gym = gymAdminPanelRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));

        AdminPanelGymImage image = gym.getImages().stream()
                .filter(i -> i.getId().equals(imageId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("IMAGE_NOT_FOUND", "error.image_not_found"));

        fileStorageService.deleteFile(image.getUrl());
        gym.getImages().remove(image);
        gymAdminPanelRepository.save(gym);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = {"gym-detail", "main-page-gyms"}, allEntries = true),
            @CacheEvict(cacheNames = "gym-images", key = "#gymId")
    })
    public void updateImageOrder(Long gymId, UpdateImageOrderRequest request) {
        GymAdminPanel gym = gymAdminPanelRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));

        Map<Long, AdminPanelGymImage> imageMap = gym.getImages().stream()
                .collect(Collectors.toMap(AdminPanelGymImage::getId, i -> i));

        for (UpdateImageOrderRequest.ImageOrderItem item : request.images()) {
            AdminPanelGymImage image = imageMap.get(item.id());
            if (image == null) {
                throw new ResourceNotFoundException("IMAGE_NOT_FOUND", "error.image_not_found");
            }
            image.setSortOrder(item.sortOrder());
        }

        gymAdminPanelRepository.save(gym);
    }

    @Transactional
    public WorkingHourDto addWorkingHour(Long gymId, WorkingHourRequest request) {
        gymAdminPanelRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));

        if (workingHourRepository.existsByGymIdAndDayOfWeek(gymId, request.dayOfWeek())) {
            throw new ConflictException("WORKING_HOUR_ALREADY_EXISTS", "error.working_hour_already_exists");
        }

        validateWorkingHourRequest(request);

        AdminPanelWorkingHour wh = AdminPanelWorkingHour.builder()
                .gym(gymAdminPanelRepository.getReferenceById(gymId))
                .dayOfWeek(request.dayOfWeek())
                .openTime(parseTime(request.openTime()))
                .closeTime(parseTime(request.closeTime()))
                .isClosed(request.isClosed())
                .build();

        return adminPanelGymMapper.toWorkingHourDto(workingHourRepository.save(wh));
    }

    @Transactional
    public void updateWorkingHour(Long gymId, Long workingHourId, WorkingHourRequest request) {
        AdminPanelWorkingHour wh = workingHourRepository.findByIdAndGymId(workingHourId, gymId)
                .orElseThrow(() -> new ResourceNotFoundException("WORKING_HOUR_NOT_FOUND", "error.working_hour_not_found"));

        validateWorkingHourRequest(request);
        adminPanelGymMapper.updateWorkingHour(wh, request);

        workingHourRepository.save(wh);
    }

    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("FILE_REQUIRED", "error.file_required");
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new BadRequestException("INVALID_FILE_TYPE", "error.invalid_file_type");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BadRequestException("FILE_TOO_LARGE", "error.file_too_large");
        }
    }

    private void validateWorkingHourRequest(WorkingHourRequest request) {
        if (!request.isClosed()) {
            if (!StringUtils.hasText(request.openTime()) || !StringUtils.hasText(request.closeTime())) {
                throw new BadRequestException("TIME_REQUIRED", "error.open_close_time_required");
            }
            LocalTime open = parseTime(request.openTime());
            LocalTime close = parseTime(request.closeTime());
            if (!open.isBefore(close)) {
                throw new BadRequestException("INVALID_TIME_RANGE", "error.open_time_must_be_before_close");
            }
        }
    }

    private LocalTime parseTime(String time) {
        if (!StringUtils.hasText(time)) return null;
        try {
            return LocalTime.parse(time);
        } catch (Exception e) {
            throw new BadRequestException("INVALID_TIME_FORMAT", "error.invalid_time_format");
        }
    }

}
