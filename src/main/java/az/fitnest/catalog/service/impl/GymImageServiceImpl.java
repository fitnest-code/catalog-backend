package az.fitnest.catalog.service.impl;

import az.fitnest.catalog.dto.response.GymImageDto;
import az.fitnest.catalog.exception.BadRequestException;
import az.fitnest.catalog.exception.ResourceNotFoundException;
import az.fitnest.catalog.model.entity.Gym;
import az.fitnest.catalog.model.entity.GymImage;
import az.fitnest.catalog.repository.GymImageRepository;
import az.fitnest.catalog.repository.GymRepository;
import az.fitnest.catalog.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class GymImageServiceImpl implements az.fitnest.catalog.service.GymImageService {

    private final GymRepository gymRepository;
    private final GymImageRepository gymImageRepository;
    private final FileStorageService fileStorageService;

    @Transactional
    @CacheEvict(cacheNames = {"gyms", "gymImages"}, key = "#gymId")
    public void updateCoverImageUrl(Long gymId, String url) {
        Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));
        if (gym.getCoverImageUrl() != null && !gym.getCoverImageUrl().equals(url)) {
            safeDeleteFile(gym.getCoverImageUrl());
        }
        gym.setCoverImageUrl(url);
        gymRepository.save(gym);
    }

    @Transactional
    @CacheEvict(cacheNames = {"gyms", "gymImages"}, key = "#gymId")
    public void deleteCoverImageUrl(Long gymId) {
        Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));
        if (gym.getCoverImageUrl() != null && !gym.getCoverImageUrl().isBlank()) {
            safeDeleteFile(gym.getCoverImageUrl());
            gym.setCoverImageUrl(null);
            gymRepository.save(gym);
        }
    }

    @Transactional
    @CacheEvict(cacheNames = {"gyms", "gymImages"}, key = "#gymId")
    public GymImageDto putGymImage(Long gymId, String imageName, String url) {
        Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));
        GymImage gymImage = new GymImage();
        gymImage.setGym(gym);
        gymImage.setImageName(imageName);
        gymImage.setUrl(url);
        gymImage = gymImageRepository.save(gymImage);
        return GymImageDto.builder().id(gymImage.getId()).gymId(gymId).name(gymImage.getImageName()).url(gymImage.getUrl()).build();
    }

    @Transactional
    @CacheEvict(cacheNames = {"gyms", "gymImages"}, key = "#gymId")
    public GymImageDto uploadRoomImage(Long gymId, String roomName, MultipartFile file) {
        validateImageFile(file);

        Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));

        String fullUrl = fileStorageService.saveFile(file, "/gyms/" + gymId);

        GymImage gi = new GymImage();
        gi.setGym(gym);
        gi.setImageName(roomName);
        gi.setUrl(fullUrl);
        gi.setType("photo");
        gi.setTitle(sanitizeFilename(file.getOriginalFilename()));
        gi = gymImageRepository.save(gi);

        return GymImageDto.builder().id(gi.getId()).gymId(gymId).name(gi.getImageName()).url(gi.getUrl()).build();
    }

    @Transactional
    @CacheEvict(cacheNames = {"gyms", "gymImages"}, key = "#gymId")
    public void deleteRoomImage(Long gymId, Long imageId) {
        GymImage existing = gymImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_IMAGE_NOT_FOUND", "error.gym_image_not_found"));
        if (existing.getGym() == null || !existing.getGym().getId().equals(gymId)) {
            throw new ResourceNotFoundException("GYM_IMAGE_MISMATCH", "error.gym_image_mismatch");
        }
        if (existing.getUrl() != null && !existing.getUrl().isBlank()) {
            safeDeleteFile(existing.getUrl());
        }
        gymImageRepository.delete(existing);
    }

    private void safeDeleteFile(String url) {
        try {
            fileStorageService.deleteFile(url);
        } catch (Exception e) {
        }
    }

    private void validateImageFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BadRequestException("FILE_EMPTY", "error.file_empty");
        }
        long maxSize = 5 * 1024 * 1024;
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new BadRequestException("FILE_TOO_LARGE", "error.file_too_large");
        }
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.equalsIgnoreCase("image/jpeg") && !contentType.equalsIgnoreCase("image/jpg") && !contentType.equalsIgnoreCase("image/png") && !contentType.equalsIgnoreCase("image/webp"))) {
            throw new BadRequestException("INVALID_FILE_TYPE", "error.invalid_file_type");
        }

    }

    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) return "unnamed";
        return filename.replaceAll("[^a-zA-Z0-9.\\-_]", "_");
    }
}
