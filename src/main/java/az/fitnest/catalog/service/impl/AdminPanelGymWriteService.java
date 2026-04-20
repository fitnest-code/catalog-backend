package az.fitnest.catalog.service.impl;

import az.fitnest.catalog.dto.admin.*;
import az.fitnest.catalog.exception.BadRequestException;
import az.fitnest.catalog.exception.ConflictException;
import az.fitnest.catalog.exception.ResourceNotFoundException;
import az.fitnest.catalog.mapper.AdminPanelGymMapper;
import az.fitnest.catalog.model.entity.AdminPanelGymImage;
import az.fitnest.catalog.model.entity.GymAdminPanel;
import az.fitnest.catalog.model.enums.AdminPanelGymStatus;
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

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminPanelGymWriteService {

    private static final List<String> ALLOWED_TYPES = List.of("image/jpeg", "image/jpg", "image/png");
    private static final long MAX_SIZE = 2 * 1024 * 1024;

    private final AdminPanelReverseGeocodingService reverseGeocodingService;
    private final GymAdminPanelRepository gymAdminPanelRepository;
    private final FileStorageService fileStorageService;
    private final AdminPanelGymMapper gymAdminMapper;
    private final LocationService locationService;

    @Transactional
    public AdminPanelGymResponse createGymForAdmin(AdminPanelCreateGymRequest request) {
        GymAdminPanel saved = gymAdminPanelRepository.save(gymAdminMapper.toEntity(request));
        return gymAdminMapper.toCreateResponse(saved);
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

        gymAdminMapper.updateGeneralInfo(gym, request, geocoding);

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

}
